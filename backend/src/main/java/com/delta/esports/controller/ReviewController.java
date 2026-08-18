package com.delta.esports.controller;

import com.delta.esports.common.Result;
import com.delta.esports.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "评价", description = "评价相关接口")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Operation(summary = "创建评价")
    @PostMapping
    public Result<?> create(HttpServletRequest request,
                            @RequestParam Long orderId,
                            @RequestParam(defaultValue = "5") Integer rating,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) String tags) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reviewService.createReview(userId, orderId, rating, content, tags));
    }

    @Operation(summary = "陪陪评价列表")
    @GetMapping("/booster/{boosterId}")
    public Result<?> boosterReviews(@PathVariable Long boosterId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return Result.success(reviewService.getBoosterReviews(boosterId, page, size));
    }

    @Operation(summary = "我提交的评价")
    @GetMapping("/my")
    public Result<?> myReviews(HttpServletRequest request,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reviewService.getBossReviews(userId, page, size));
    }
}
