package com.delta.esports.dto;

import com.delta.esports.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoosterSummaryResponse {
    private Long id;
    private String nickname;
    private String avatar;
    private String gender;
    private String boosterStatus;
    private BigDecimal rating;
    private Integer totalOrders;
    private String introduction;

    public static BoosterSummaryResponse from(User user) {
        return BoosterSummaryResponse.builder()
                .id(user.getId()).nickname(user.getNickname()).avatar(user.getAvatar())
                .gender(user.getGender()).boosterStatus(user.getBoosterStatus())
                .rating(user.getRating()).totalOrders(user.getTotalOrders())
                .introduction(user.getIntroduction()).build();
    }
}
