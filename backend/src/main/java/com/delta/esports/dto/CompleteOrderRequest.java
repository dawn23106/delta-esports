package com.delta.esports.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CompleteOrderRequest {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "是否达标不能为空")
    private Boolean isQualified;

    @Size(max = 500, message = "备注最多500字")
    private String resultNote;

    private String resultImages;
}
