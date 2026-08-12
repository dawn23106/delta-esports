package com.delta.esports.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_settlement")
public class Settlement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long boosterId;
    private BigDecimal amount;
    private String status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
