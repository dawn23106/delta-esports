package com.delta.esports.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delta.esports.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {

    /** 该用户处于冻结中（pending/approved/paid）的提现总额，用于计算剩余可提现金额 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_withdrawal " +
            "WHERE user_id = #{userId} AND status IN ('pending', 'approved', 'paid')")
    BigDecimal sumFrozenAmount(@Param("userId") Long userId);
}
