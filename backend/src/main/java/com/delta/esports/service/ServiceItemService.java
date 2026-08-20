package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.entity.ServiceItem;
import com.delta.esports.mapper.ServiceItemMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 服务项目：公开列表走 Redis 缓存（Cache-Aside 三步曲）。
 * 服务列表属于「读多、写少、能容忍几分钟旧数据」的典型缓存对象。
 * Redis 不可用（如本地开发未启动）时自动回源 MySQL，不影响功能。
 */
@Service
public class ServiceItemService {

    private static final String CACHE_KEY = "services:all";
    private static final long CACHE_TTL_MINUTES = 10;

    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ObjectMapper objectMapper;

    public List<ServiceItem> findAll(String category) {
        // ① 先读缓存里的全量上架服务，没有则查库并回填
        List<ServiceItem> list = readCachedAll();
        // ② 按分类在内存里过滤（结果与数据库过滤一致，且天然防“穿透”：乱传的分类不会打到 MySQL）
        if (category == null || category.isEmpty()) {
            return list;
        }
        return list.stream()
                .filter(item -> category.equals(item.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Cache-Aside：先读 Redis → 没有查 MySQL → 查完回填（10 分钟过期）。
     * 任何 Redis 异常都回源数据库（fail-open），保证服务可用。
     */
    private List<ServiceItem> readCachedAll() {
        try {
            String cached = redis.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<ServiceItem>>() {});
            }
        } catch (Exception ignored) {
            // 缓存不可用/反序列化失败 → 走数据库
        }
        List<ServiceItem> list = serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getIsActive, 1)
                .orderByAsc(ServiceItem::getSortOrder));
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(list),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException | RuntimeException ignored) {
            // 回填失败不阻塞返回
        }
        return list;
    }

    /** 后台列表需要最新数据（含已下架），不走缓存 */
    public List<ServiceItem> findAllForAdmin() {
        return serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .orderByAsc(ServiceItem::getSortOrder));
    }

    public ServiceItem findById(Long id) {
        return serviceItemMapper.selectById(id);
    }

    @Transactional
    public void create(ServiceItem item) {
        serviceItemMapper.insert(item);
        evictCache();
    }

    @Transactional
    public void update(ServiceItem item) {
        ServiceItem exist = serviceItemMapper.selectById(item.getId());
        if (exist == null) throw new BusinessException("服务项目不存在");
        serviceItemMapper.updateById(item);
        evictCache();
    }

    @Transactional
    public void toggleActive(Long id, boolean active) {
        ServiceItem item = new ServiceItem();
        item.setId(id);
        item.setIsActive(active ? 1 : 0);
        serviceItemMapper.updateById(item);
        evictCache();
    }

    /** 数据被改动后删除缓存，下次请求重新加载，避免脏数据 */
    private void evictCache() {
        try {
            redis.delete(CACHE_KEY);
        } catch (RuntimeException ignored) {
            // 删缓存失败不影响写操作本身
        }
    }
}
