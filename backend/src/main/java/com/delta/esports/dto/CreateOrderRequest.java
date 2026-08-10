package com.delta.esports.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateOrderRequest {
    @NotNull(message = "服务项目ID不能为空")
    private Long serviceId;

    private Long boosterId;

    @NotBlank(message = "游戏区服不能为空")
    @Size(max = 50, message = "游戏区服不能超过50个字符")
    private String gameRegion;

    @NotBlank(message = "游戏段位不能为空")
    @Size(max = 50, message = "游戏段位不能超过50个字符")
    private String gameRank;

    @NotBlank(message = "游戏地图不能为空")
    @Size(max = 100, message = "游戏地图不能超过100个字符")
    private String gameMap;

    @Size(max = 500, message = "备注最多500字")
    private String bossNote;
}
