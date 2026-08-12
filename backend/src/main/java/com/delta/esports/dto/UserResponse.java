package com.delta.esports.dto;

import com.delta.esports.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private String role;
    private String gender;
    private String status;
    private String boosterStatus;
    private BigDecimal balance;
    private BigDecimal rating;
    private Integer totalOrders;
    private String introduction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .gender(user.getGender())
                .status(user.getStatus())
                .boosterStatus(user.getBoosterStatus())
                .balance(user.getBalance())
                .rating(user.getRating())
                .totalOrders(user.getTotalOrders())
                .introduction(user.getIntroduction())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
