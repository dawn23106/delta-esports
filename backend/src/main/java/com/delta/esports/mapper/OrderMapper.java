package com.delta.esports.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delta.esports.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM t_order WHERE id = #{orderId} FOR UPDATE")
    Order selectForUpdate(@Param("orderId") Long orderId);

    /**
     * 原子抢单/派单：只有订单状态仍为 pending 时才更新成功。
     * “检查状态 + 抢占”由数据库在一条 UPDATE 内原子完成，两个并发请求只有一个返回 1。
     * @return 受影响行数（1 = 抢单成功，0 = 已被别人抢走或状态不对）
     */
    @Update("UPDATE t_order SET status = 'assigned', booster_id = #{boosterId}, updated_at = NOW() " +
            "WHERE id = #{orderId} AND status = 'pending'")
    int claim(@Param("orderId") Long orderId, @Param("boosterId") Long boosterId);

    /**
     * 原子确认完成：只有状态仍为 submitted 时才更新成功。
     * 作为“完成资格”的抢占位，防止两个请求重复确认导致重复转账。
     * @return 受影响行数（1 = 确认成功，0 = 状态已变化）
     */
    @Update("UPDATE t_order SET status = 'done', updated_at = NOW() " +
            "WHERE id = #{orderId} AND status = 'submitted'")
    int markDone(@Param("orderId") Long orderId);

    @Update("UPDATE t_order SET status = 'in_progress', updated_at = NOW() " +
            "WHERE id = #{orderId} AND booster_id = #{boosterId} AND status = 'assigned'")
    int start(@Param("orderId") Long orderId, @Param("boosterId") Long boosterId);

    @Update("UPDATE t_order SET status = 'submitted', is_qualified = #{qualified}, " +
            "result_note = #{resultNote}, result_images = #{resultImages}, updated_at = NOW() " +
            "WHERE id = #{orderId} AND booster_id = #{boosterId} AND status = 'in_progress'")
    int submit(@Param("orderId") Long orderId, @Param("boosterId") Long boosterId,
               @Param("qualified") Integer qualified, @Param("resultNote") String resultNote,
               @Param("resultImages") String resultImages);

    @Update("UPDATE t_order SET status = CASE WHEN booster_id IS NULL THEN 'pending' ELSE 'assigned' END, " +
            "updated_at = NOW() WHERE id = #{orderId} AND status = 'pending_payment'")
    int publishPaid(@Param("orderId") Long orderId);

    @Update("UPDATE t_order SET booster_id = NULL, updated_at = NOW() " +
            "WHERE id = #{orderId} AND booster_id = #{boosterId} AND status = 'pending_payment'")
    int clearPreassignedBooster(@Param("orderId") Long orderId, @Param("boosterId") Long boosterId);

    @Update("UPDATE t_order SET status = 'cancelled', updated_at = NOW() " +
            "WHERE id = #{orderId} AND status = 'pending_payment'")
    int cancelUnpaid(@Param("orderId") Long orderId);

    @Update("UPDATE t_order SET status = 'refund_pending', updated_at = NOW() " +
            "WHERE id = #{orderId} AND status IN ('pending', 'assigned')")
    int reserveRefund(@Param("orderId") Long orderId);

    @Update("UPDATE t_order SET status = 'cancelled', updated_at = NOW() " +
            "WHERE id = #{orderId} AND status = 'refund_pending'")
    int finishRefund(@Param("orderId") Long orderId);
}
