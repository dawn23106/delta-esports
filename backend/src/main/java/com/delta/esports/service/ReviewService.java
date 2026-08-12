package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.PageResult;
import com.delta.esports.entity.Order;
import com.delta.esports.entity.Review;
import com.delta.esports.entity.User;
import com.delta.esports.mapper.OrderMapper;
import com.delta.esports.mapper.ReviewMapper;
import com.delta.esports.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Transactional
    public Review createReview(Long bossId, Long orderId, Integer rating, String content, String tags) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(400, "评分必须在1到5之间");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBossId().equals(bossId)) throw new BusinessException("无权评价");
        if (!"done".equals(order.getStatus())) throw new BusinessException("订单未完成，无法评价");

        Long count = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        if (count > 0) throw new BusinessException("该订单已评价");

        Review review = new Review();
        review.setOrderId(orderId);
        review.setBossId(bossId);
        review.setBoosterId(order.getBoosterId());
        review.setRating(rating);
        review.setContent(content);
        review.setTags(tags);
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException duplicate) {
            throw new BusinessException(409, "该订单已评价");
        }

        updateBoosterRating(order.getBoosterId());

        return review;
    }

    public PageResult<Review> getBoosterReviews(Long boosterId, int page, int size) {
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<>();
        qw.eq(Review::getBoosterId, boosterId).orderByDesc(Review::getCreatedAt);
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(result);
    }

    public PageResult<Review> getBossReviews(Long bossId, int page, int size) {
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<>();
        qw.eq(Review::getBossId, bossId).orderByDesc(Review::getCreatedAt);
        return PageResult.of(reviewMapper.selectPage(new Page<>(page, size), qw));
    }

    private void updateBoosterRating(Long boosterId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getBoosterId, boosterId));
        if (reviews.isEmpty()) return;
        double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(5.0);
        // 只更新评分字段，避免把并发到账后的余额用旧对象覆盖掉。
        User update = new User();
        update.setId(boosterId);
        update.setRating(BigDecimal.valueOf(Math.round(avgRating * 100.0) / 100.0));
        userMapper.updateById(update);
    }
}
