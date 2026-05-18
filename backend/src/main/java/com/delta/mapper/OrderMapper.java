package com.delta.mapper;

import com.delta.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 订单数据访问层 — MyBatis 接口。
 * @Mapper 注解告诉 Spring："这是一个MyBatis Mapper，请生成代理实现类"。
 * SQL 语句在 resources/mapper/OrderMapper.xml 中，不写Java注解。
 *
 * 方法命名规则：find开头=查询多条，count开头=统计数量，insert/update开头=写操作。
 */
@Mapper
public interface OrderMapper {

    /** 插入订单，id 由数据库自增回填 */
    int insert(Order order);

    Order findById(Long id);

    /** 更新订单状态（终态变更） */
    int updateStatus(Long id, String status);

    /**
     * 乐观锁抢单 — SQL: UPDATE ... WHERE status='pending'
     * @return 受影响行数，1=成功，0=已被抢
     */
    int claimOptimistic(Long id, Long boosterId);

    /** 玩家查看自己的订单（分页，offset=起始位置，limit=条数） */
    List<Order> findByCustomer(Long customerId, int offset, int limit);

    long countByCustomer(Long customerId);

    /** 打手浏览可接订单池（只查pending状态） */
    List<Order> findPool(Long boosterId, String game, int offset, int limit);

    long countPool(String game);

    /** 打手查看自己已接的订单 */
    List<Order> findByBooster(Long boosterId, int offset, int limit);

    long countByBooster(Long boosterId);

    /** 客服查询全量订单（支持状态+游戏筛选） */
    List<Order> findAll(String status, String game, int offset, int limit);

    long countAll(String status, String game);

    /** 客服派单 — 跳过抢单直接assign给打手 */
    int assign(Long id, Long boosterId, Long csId);
}
