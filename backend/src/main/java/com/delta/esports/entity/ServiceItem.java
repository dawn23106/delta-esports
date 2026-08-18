package com.delta.esports.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@TableName("t_service_item")
public class ServiceItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 200, message = "服务名称不能超过200个字符")
    private String name;
    private String description;
    @NotBlank(message = "服务分类不能为空")
    private String category;
    @NotNull(message = "服务价格不能为空")
    @DecimalMin(value = "0.01", message = "服务价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "服务价格最多保留两位小数")
    private BigDecimal basePrice;
    private String priceUnit;
    private String coverImage;
    private String guaranteeDesc;
    private String refundPolicy;
    private Integer isActive;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
