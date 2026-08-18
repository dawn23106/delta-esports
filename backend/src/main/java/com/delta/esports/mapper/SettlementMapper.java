package com.delta.esports.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delta.esports.entity.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface SettlementMapper extends BaseMapper<Settlement> {

    /** 结算时间早于 deadline 的净收入总额（已过锁定期、可提现的部分） */
    @Select("SELECT COALESCE(SUM(net_amount), 0) FROM t_settlement " +
            "WHERE booster_id = #{boosterId} AND status = 'completed' AND created_at <= #{deadline}")
    BigDecimal sumWithdrawableNet(@Param("boosterId") Long boosterId, @Param("deadline") LocalDateTime deadline);
}
