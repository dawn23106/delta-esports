package com.delta.esports.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_message")
public class OrderMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long senderId;
    private String content;
    private String type;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
