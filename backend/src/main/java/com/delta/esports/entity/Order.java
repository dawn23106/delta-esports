package com.delta.esports.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long bossId;
    private Long boosterId;
    private Long serviceId;
    private String serviceName;
    private String gameRegion;
    private String gameRank;
    private String gameMap;
    private String bossNote;
    private String status;
    private BigDecimal amount;
    private Integer isQualified;
    private String resultNote;
    private String resultImages;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
