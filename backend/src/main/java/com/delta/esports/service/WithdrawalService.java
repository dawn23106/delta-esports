package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.PageSupport;
import com.delta.esports.entity.Withdrawal;
import com.delta.esports.mapper.SettlementMapper;
import com.delta.esports.mapper.UserMapper;
import com.delta.esports.mapper.WithdrawalMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class WithdrawalService {

    private static final Set<String> REVIEW_ACTIONS = Set.of("approve", "reject", "paid");

    @Autowired private WithdrawalMapper withdrawalMapper;
    @Autowired private SettlementMapper settlementMapper;
    @Autowired private UserMapper userMapper;

    /** 结算满多少天后才可提现 */
    @Value("${app.withdrawal.lock-days:7}")
    private int lockDays;

    /**
     * 陪陪申请提现：只能提取「结算满 lockDays 天」的净收入，且扣除已冻结的提现额度。
     */
    @Transactional
    public Withdrawal apply(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new BusinessException(400, "提现金额不合法");
        }
        LocalDateTime deadline = LocalDateTime.now().minusDays(lockDays);
        BigDecimal withdrawable = settlementMapper.sumWithdrawableNet(userId, deadline);
        BigDecimal frozen = withdrawalMapper.sumFrozenAmount(userId);
        BigDecimal available = withdrawable.subtract(frozen);
        if (available.compareTo(amount) < 0) {
            throw new BusinessException(409, "可提现金额不足（结算满 " + lockDays + " 天后才可提现）");
        }
        // 冻结余额：申请即从余额扣除，审核通过打款/驳回退回
        if (userMapper.deductBalance(userId, amount) == 0) {
            throw new BusinessException(400, "余额不足");
        }
        Withdrawal w = new Withdrawal();
        w.setUserId(userId);
        w.setAmount(amount);
        w.setStatus("pending");
        withdrawalMapper.insert(w);
        return w;
    }

    public Page<Withdrawal> myList(Long userId, int page, int size) {
        return withdrawalMapper.selectPage(PageSupport.of(page, size),
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getUserId, userId)
                        .orderByDesc(Withdrawal::getCreatedAt));
    }

    public Page<Withdrawal> adminPage(int page, int size, String status) {
        LambdaQueryWrapper<Withdrawal> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            qw.eq(Withdrawal::getStatus, status);
        }
        qw.orderByDesc(Withdrawal::getCreatedAt);
        return withdrawalMapper.selectPage(PageSupport.of(page, size), qw);
    }

    @Transactional
    public Withdrawal review(Long id, String action, String remark) {
        if (!REVIEW_ACTIONS.contains(action)) {
            throw new BusinessException(400, "审核动作不合法");
        }
        Withdrawal w = withdrawalMapper.selectById(id);
        if (w == null) throw new BusinessException(404, "提现申请不存在");

        switch (action) {
            case "approve":
                if (!"pending".equals(w.getStatus())) throw new BusinessException(409, "该申请当前状态不可通过");
                w.setStatus("approved");
                break;
            case "reject":
                if (!"pending".equals(w.getStatus())) throw new BusinessException(409, "该申请当前状态不可驳回");
                userMapper.addBalance(w.getUserId(), w.getAmount()); // 退回冻结余额
                w.setStatus("rejected");
                break;
            case "paid":
                if (!"approved".equals(w.getStatus())) throw new BusinessException(409, "该申请当前状态不可标记打款");
                w.setStatus("paid");
                break;
            default:
                throw new BusinessException(400, "审核动作不合法");
        }
        w.setRemark(remark);
        w.setReviewedAt(LocalDateTime.now());
        withdrawalMapper.updateById(w);
        return w;
    }
}
