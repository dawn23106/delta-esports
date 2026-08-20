package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.PageResult;
import com.delta.esports.common.PageSupport;
import com.delta.esports.config.RedisCacheTemplate;
import com.delta.esports.entity.Announcement;
import com.delta.esports.mapper.AnnouncementMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 公告：公开列表走 Redis 缓存。缓存全量有序列表，分页在内存完成；后台增删改自动失效。
 */
@Service
public class AnnouncementService {

    private static final String LIST_KEY = "announcements:all";
    private static final long TTL_MINUTES = 10;

    @Autowired
    private AnnouncementMapper announcementMapper;
    @Autowired
    private RedisCacheTemplate cache;

    public PageResult<Announcement> page(int page, int size) {
        List<Announcement> all = readCachedAll();
        int total = all.size();
        int safePage = PageSupport.normalizePage(page);
        int safeSize = PageSupport.normalizeSize(size);
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);

        PageResult<Announcement> result = new PageResult<>();
        result.setRecords(new ArrayList<>(all.subList(from, to)));
        result.setTotal(total);
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setPages((total + safeSize - 1) / safeSize);
        return result;
    }

    private List<Announcement> readCachedAll() {
        List<Announcement> cached = cache.get(LIST_KEY, new TypeReference<List<Announcement>>() {});
        if (cached != null) {
            return cached;
        }
        List<Announcement> all = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreatedAt));
        cache.set(LIST_KEY, all, TTL_MINUTES, TimeUnit.MINUTES);
        return all;
    }

    @Transactional
    public void create(Announcement a) {
        announcementMapper.insert(a);
        cache.evict(LIST_KEY);
    }

    @Transactional
    public void update(Announcement a) {
        announcementMapper.updateById(a);
        cache.evict(LIST_KEY);
    }

    @Transactional
    public void delete(Long id) {
        announcementMapper.deleteById(id);
        cache.evict(LIST_KEY);
    }
}
