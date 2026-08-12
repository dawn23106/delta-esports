package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.entity.Gift;
import com.delta.esports.entity.User;
import com.delta.esports.mapper.GiftMapper;
import com.delta.esports.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GiftService {

    @Autowired
    private GiftMapper giftMapper;
    @Autowired
    private UserMapper userMapper;

    public Page<Gift> page(int page, int size) {
        LambdaQueryWrapper<Gift> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Gift::getCreatedAt);
        return giftMapper.selectPage(new Page<>(page, size), qw);
    }

    public Page<Gift> sentBy(Long senderId, int page, int size) {
        return giftMapper.selectPage(new Page<>(page, Math.min(Math.max(size, 1), 100)),
                new LambdaQueryWrapper<Gift>()
                        .eq(Gift::getSenderId, senderId)
                        .orderByDesc(Gift::getCreatedAt));
    }

    @Transactional
    public Gift sendGift(Long senderId, Long receiverId, String giftName, BigDecimal price, String message) {
        if (senderId == null) throw new BusinessException(401, "未登录");
        if (receiverId == null || receiverId.equals(senderId)) {
            throw new BusinessException(400, "礼物接收者不合法");
        }
        if (price == null || price.signum() <= 0 || price.scale() > 2
                || price.compareTo(new BigDecimal("100000.00")) > 0) {
            throw new BusinessException(400, "礼物金额不合法");
        }
        String normalizedName = giftName == null ? "" : giftName.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 100) {
            throw new BusinessException(400, "礼物名称应为1到100个字符");
        }
        if (message != null && message.length() > 500) {
            throw new BusinessException(400, "礼物留言不能超过500个字符");
        }
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null || !"booster".equals(receiver.getRole())) {
            throw new BusinessException("接收者不存在或不是陪陪");
        }

        // 原子扣款：数据库把“检查余额 + 扣减”合并成一条 UPDATE。
        // 余额不足时 WHERE 条件不满足，不更新任何行（返回 0），不会出现“余额不够却扣成功”。
        if (userMapper.deductBalance(senderId, price) == 0) {
            throw new BusinessException(400, "余额不足");
        }

        // 原子加款：balance = balance + price，并发下不会丢更新。
        userMapper.addBalance(receiverId, price);

        Gift gift = new Gift();
        gift.setSenderId(senderId);
        gift.setReceiverId(receiverId);
        gift.setGiftName(normalizedName);
        gift.setPrice(price);
        gift.setMessage(message);
        giftMapper.insert(gift);

        return gift;
    }
}
