package com.delta.esports.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delta.esports.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
    @Update("UPDATE t_payment_order SET status = 'paid', provider_order_no = #{providerOrderNo}, " +
            "provider_pay_no = #{providerPayNo}, paid_at = NOW(), failure_reason = NULL, updated_at = NOW() " +
            "WHERE id = #{id} AND status IN ('created', 'prepared')")
    int markPaid(@Param("id") Long id, @Param("providerOrderNo") String providerOrderNo,
                 @Param("providerPayNo") String providerPayNo);

    @Update("UPDATE t_payment_order SET status = 'paid_review', failure_reason = #{reason}, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'paid'")
    int markPaidReview(@Param("id") Long id, @Param("reason") String reason);

    @Update("UPDATE t_payment_order SET status = 'refunding', refund_no = #{refundNo}, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'paid'")
    int reserveRefund(@Param("id") Long id, @Param("refundNo") String refundNo);

    @Update("UPDATE t_payment_order SET refund_no = #{refundNo}, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'refunding'")
    int updateRefundNo(@Param("id") Long id, @Param("refundNo") String refundNo);

    @Update("UPDATE t_payment_order SET status = 'refunded', refund_no = #{refundNo}, " +
            "refunded_amount = #{amount}, refunded_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'refunding'")
    int finishRefund(@Param("id") Long id, @Param("refundNo") String refundNo,
                     @Param("amount") BigDecimal amount);
}
