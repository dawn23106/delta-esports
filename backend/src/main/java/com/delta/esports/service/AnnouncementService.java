package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.PageResult;
import com.delta.esports.common.PageSupport;
import com.delta.esports.entity.Announcement;
import com.delta.esports.mapper.AnnouncementMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 公告：公开列表走 Redis 缓存（Cache-Aside）。
 * 公告「读多、写少、能容忍几分钟旧数据」，缓存全量有序列表，分页在内存完成；
 * 后台增删改自动失效缓存。
 */
@Service
public class AnnouncementService {

    private static final String CACHE_KEY = "announcements:all";
    private static final long CACHE_TTL_MINUTES = 10;

    @Autowired
    private AnnouncementMapper announcementMapper;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ObjectMapper objectMapper;

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

    /** Cache-Aside：先读缓存 → 没有查 MySQL（排序）→ 回填，10 分钟过期 */
    private List<Announcement> readCachedAll() {
        try {
            String cached = redis.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Announcement>>() {});
            }
        } catch (Exception ignored) {
            // 缓存不可用/反序列化失败 → 走数据库
        }
        List<Announcement> all = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreatedAt));
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(all),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ignored) {
            // 回填失败不阻塞返回
        }
        return all;
    }

    @Transactional
    public void create(Announcement a) {
        announcementMapper.insert(a);
        evictCache();
    }

    @Transactional
    public void update(Announcement a) {
        announcementMapper.updateById(a);
        evictCache();
    }

    @Transactional
    public void delete(Long id) {
        announcementMapper.deleteById(id);
        evictCache();
    }

    private void evictCache() {
        try {
            redis.delete(CACHE_KEY);
        } catch (RuntimeException ignored) {
            // 删缓存失败不影响写操作本身
        }
    }
}
