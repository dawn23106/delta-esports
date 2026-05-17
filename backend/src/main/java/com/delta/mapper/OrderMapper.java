package com.delta.mapper;

import com.delta.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface OrderMapper {

    int insert(Order order);

    Order findById(Long id);

    int updateStatus(Long id, String status);

    int claimOptimistic(Long id, Long boosterId);

    List<Order> findByCustomer(Long customerId, int offset, int limit);

    long countByCustomer(Long customerId);

    List<Order> findPool(Long boosterId, String game, int offset, int limit);

    long countPool(String game);

    List<Order> findByBooster(Long boosterId, int offset, int limit);

    long countByBooster(Long boosterId);

    List<Order> findAll(String status, String game, int offset, int limit);

    long countAll(String status, String game);

    int assign(Long id, Long boosterId, Long csId);
}
