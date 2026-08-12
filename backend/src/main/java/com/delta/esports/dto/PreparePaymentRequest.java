package com.delta.esports.dto;

import lombok.Data;

@Data
public class PreparePaymentRequest {
    /** wx.login 返回的一次性 code；本地 mock 模式可为空。 */
    private String loginCode;
}
