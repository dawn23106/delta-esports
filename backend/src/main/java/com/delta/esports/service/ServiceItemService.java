package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.config.RedisCacheTemplate;
import com.delta.esports.entity.ServiceItem;
import com.delta.esports.mapper.ServiceItemMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 服务项目：公开列表/详情走 Redis 缓存（Cache-Aside）。
 * 缓存读写全部收敛到 {@link RedisCacheTemplate}，Redis 故障自动回源 MySQL。
 */
@Service
public class ServiceItemService {

    private static final String LIST_KEY = "services:all";
    private static final String DETAIL_KEY_PREFIX = "services:detail:";
    private static final long TTL_MINUTES = 10;

    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private RedisCacheTemplate cache;

    public List<ServiceItem> findAll(String category) {
        List<ServiceItem> list = readCachedAll();
        if (category == null || category.isEmpty()) {
            return list;
        }
        // 分类在内存过滤：与数据库过滤结果一致，且天然防缓存穿透（乱传分类不打 MySQL）
        return list.stream()
                .filter(item -> category.equals(item.getCategory()))
                .collect(Collectors.toList());
    }

    private List<ServiceItem> readCachedAll() {
        List<ServiceItem> cached = cache.get(LIST_KEY, new TypeReference<List<ServiceItem>>() {});
        if (cached != null) {
            return cached;
        }
        List<ServiceItem> list = serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getIsActive, 1)
                .orderByAsc(ServiceItem::getSortOrder));
        cache.set(LIST_KEY, list, TTL_MINUTES, TimeUnit.MINUTES);
        return list;
    }

    /** 后台列表需要最新数据（含已下架），不走缓存 */
    public List<ServiceItem> findAllForAdmin() {
        return serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .orderByAsc(ServiceItem::getSortOrder));
    }

    public ServiceItem findById(Long id) {
        String key = detailKey(id);
        ServiceItem cached = cache.get(key, ServiceItem.class);
        if (cached != null) {
            return cached;
        }
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item != null) {
            cache.set(key, item, TTL_MINUTES, TimeUnit.MINUTES);
        }
        return item;
    }

    @Transactional
    public void create(ServiceItem item) {
        serviceItemMapper.insert(item);
        cache.evict(LIST_KEY);
    }

    @Transactional
    public void update(ServiceItem item) {
        ServiceItem exist = serviceItemMapper.selectById(item.getId());
        if (exist == null) throw new BusinessException("服务项目不存在");
        serviceItemMapper.updateById(item);
        cache.evict(LIST_KEY);
        cache.evict(detailKey(item.getId()));
    }

    @Transactional
    public void toggleActive(Long id, boolean active) {
        ServiceItem item = new ServiceItem();
        item.setId(id);
        item.setIsActive(active ? 1 : 0);
        serviceItemMapper.updateById(item);
        cache.evict(LIST_KEY);
        cache.evict(detailKey(id));
    }

    private String detailKey(Long id) {
        return DETAIL_KEY_PREFIX + id;
    }
}
