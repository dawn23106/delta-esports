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

    @Transactional
    public Gift sendGift(Long senderId, Long receiverId, String giftName, BigDecimal price, String message) {
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null || !"booster".equals(receiver.getRole())) {
            throw new BusinessException("接收者不存在或不是陪陪");
        }

        User sender = userMapper.selectById(senderId);
        if (sender.getBalance().compareTo(price) < 0) {
            throw new BusinessException(400, "余额不足");
        }

        // 扣发送者
        sender.setBalance(sender.getBalance().subtract(price));
        userMapper.updateById(sender);

        // 加接收者
        receiver.setBalance(receiver.getBalance().add(price));
        userMapper.updateById(receiver);

        Gift gift = new Gift();
        gift.setSenderId(senderId);
        gift.setReceiverId(receiverId);
        gift.setGiftName(giftName);
        gift.setPrice(price);
        gift.setMessage(message);
        giftMapper.insert(gift);

        return gift;
    }
}
