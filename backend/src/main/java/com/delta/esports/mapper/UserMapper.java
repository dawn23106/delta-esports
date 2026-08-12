package com.delta.esports.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delta.esports.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 原子加余额（不检查余额），数据库自己执行 balance = balance + amount。
     * 为什么不能先 SELECT 再 UPDATE：两步之间有别的请求插入，旧值会覆盖新值（丢更新）。
     * @return 受影响行数（0 = 用户不存在）
     */
    @Update("UPDATE t_user SET balance = balance + #{amount}, updated_at = NOW() WHERE id = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子扣余额（带余额检查）：数据库把“检查余额”和“扣减”合并成一条 UPDATE。
     * 余额不足时 WHERE 条件不满足，不更新任何行（返回 0）。
     * @return 受影响行数（0 = 余额不足或用户不存在）
     */
    @Update("UPDATE t_user SET balance = balance - #{amount}, updated_at = NOW() " +
            "WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子完成订单数 +1：数据库自己算 total_orders = total_orders + 1。
     * 如果先读再加再写，并发下会少计数。
     * @return 受影响行数
     */
    @Update("UPDATE t_user SET total_orders = total_orders + 1, updated_at = NOW() WHERE id = #{userId}")
    int incrementTotalOrders(@Param("userId") Long userId);

    @Update("UPDATE t_user SET booster_status = 'busy', updated_at = NOW() " +
            "WHERE id = #{userId} AND role = 'booster' AND status = 'active' AND booster_status = 'idle'")
    int reserveBooster(@Param("userId") Long userId);

    @Update("UPDATE t_user SET booster_status = 'idle', updated_at = NOW() " +
            "WHERE id = #{userId} AND role = 'booster' AND booster_status = 'busy'")
    int releaseBooster(@Param("userId") Long userId);
}
