package com.delta.esports.controller;

import com.delta.esports.common.Result;
import com.delta.esports.entity.ServiceItem;
import com.delta.esports.service.ServiceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "服务项目", description = "陪玩代练服务项目接口")
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private ServiceItemService serviceItemService;

    @Operation(summary = "服务项目列表（公开）")
    @GetMapping
    public Result<?> list(@RequestParam(required = false) String category) {
        return Result.success(serviceItemService.findAll(category));
    }

    @Operation(summary = "服务项目详情（公开）")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        ServiceItem item = serviceItemService.findById(id);
        if (item == null) return Result.error("服务项目不存在");
        return Result.success(item);
    }

    @Operation(summary = "创建服务项目")
    @PostMapping
    public Result<?> create(@Valid @RequestBody ServiceItem item) {
        serviceItemService.create(item);
        return Result.success();
    }

    @Operation(summary = "更新服务项目")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ServiceItem item) {
        item.setId(id);
        serviceItemService.update(item);
        return Result.success();
    }

    @Operation(summary = "上架/下架服务项目")
    @PutMapping("/{id}/toggle")
    public Result<?> toggle(@PathVariable Long id, @RequestParam boolean active) {
        serviceItemService.toggleActive(id, active);
        return Result.success();
    }
}
