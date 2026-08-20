package com.delta.esports.service;

import com.delta.esports.common.PageResult;
import com.delta.esports.dto.BoosterSummaryResponse;
import com.delta.esports.entity.Announcement;
import com.delta.esports.entity.ServiceItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 Redis 缓存会用到的 JSON 序列化/反序列化都能正确往返。
 * 测试环境没有 Redis（走 fail-open 查库），但序列化代码与生产完全一致，
 * 确保「缓存命中时能读回」而不只是能写入。
 */
@SpringBootTest
class CacheSerializationTest {

    @Autowired private ObjectMapper objectMapper;
    @Autowired private ServiceItemService serviceItemService;
    @Autowired private AnnouncementService announcementService;
    @Autowired private UserService userService;

    @Test
    void servicesListRoundTrip() throws Exception {
        List<ServiceItem> list = serviceItemService.findAll(null);
        String json = objectMapper.writeValueAsString(list);
        List<ServiceItem> back = objectMapper.readValue(json, new TypeReference<List<ServiceItem>>() {});
        assertEquals(list.size(), back.size());
        assertEquals(list.get(0).getId(), back.get(0).getId());
    }

    @Test
    void serviceDetailRoundTrip() throws Exception {
        ServiceItem item = serviceItemService.findById(1L);
        String json = objectMapper.writeValueAsString(item);
        ServiceItem back = objectMapper.readValue(json, ServiceItem.class);
        assertEquals(item.getId(), back.getId());
        assertEquals(item.getName(), back.getName());
    }

    @Test
    void announcementsPageRoundTrip() throws Exception {
        PageResult<Announcement> result = announcementService.page(1, 10);
        String json = objectMapper.writeValueAsString(result);
        PageResult<Announcement> back = objectMapper.readValue(json, new TypeReference<PageResult<Announcement>>() {});
        assertEquals(result.getTotal(), back.getTotal());
        assertEquals(result.getRecords().size(), back.getRecords().size());
        assertEquals(result.getPage(), back.getPage());
        assertEquals(result.getSize(), back.getSize());
    }

    @Test
    void boostersPageRoundTrip() throws Exception {
        PageResult<BoosterSummaryResponse> result = userService.boosterPage(1, 10);
        String json = objectMapper.writeValueAsString(result);
        PageResult<BoosterSummaryResponse> back = objectMapper.readValue(json,
                new TypeReference<PageResult<BoosterSummaryResponse>>() {});
        assertEquals(result.getTotal(), back.getTotal());
        assertEquals(result.getRecords().size(), back.getRecords().size());
        assertEquals(result.getRecords().get(0).getNickname(), back.getRecords().get(0).getNickname());
    }
}
