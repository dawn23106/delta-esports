package com.delta.esports.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.PageSupport;
import com.delta.esports.entity.OrderMessage;
import com.delta.esports.entity.ServiceItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class QueryPaginationTest {

    @Autowired private ServiceItemMapper serviceItemMapper;
    @Autowired private OrderMessageMapper messageMapper;

    @Test
    void mybatisPlusPaginationActuallyLimitsDatabaseResult() {
        for (int i = 0; i < 105; i++) {
            ServiceItem item = new ServiceItem();
            item.setName("pagination-" + i);
            item.setCategory("test");
            item.setBasePrice(BigDecimal.ONE);
            item.setIsActive(1);
            item.setSortOrder(i);
            serviceItemMapper.insert(item);
        }

        Page<ServiceItem> result = serviceItemMapper.selectPage(
                PageSupport.of(1, 20), new LambdaQueryWrapper<ServiceItem>().orderByAsc(ServiceItem::getId));
        assertEquals(20, result.getRecords().size());
        assertTrue(result.getTotal() >= 105);
    }

    @Test
    void messageCursorReturnsBoundedOlderPage() {
        long orderId = 987654321L;
        for (int i = 0; i < 5; i++) {
            OrderMessage message = new OrderMessage();
            message.setOrderId(orderId);
            message.setSenderId(1L);
            message.setContent("message-" + i);
            message.setType("text");
            messageMapper.insert(message);
        }

        List<OrderMessage> latest = messageMapper.selectRecent(orderId, null, 2);
        assertEquals(2, latest.size());
        assertTrue(latest.get(0).getId() > latest.get(1).getId());

        List<OrderMessage> older = messageMapper.selectRecent(orderId, latest.get(1).getId(), 2);
        assertEquals(2, older.size());
        assertTrue(older.get(0).getId() < latest.get(1).getId());
    }
}
