package com.delta.esports.controller;

import com.delta.esports.common.Result;
import com.delta.esports.config.RequireRole;
import com.delta.esports.entity.Announcement;
import com.delta.esports.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台", description = "管理员专用接口")
@RequireRole("admin")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private AnnouncementService announcementService;
    @Autowired
    private GiftService giftService;
    @Autowired
    private SettlementService settlementService;
    @Autowired
    private ServiceItemService serviceItemService;
    @Autowired
    private WithdrawalService withdrawalService;

    // ===== 订单管理 =====
    @Operation(summary = "全部服务项目（含已下架）")
    @GetMapping("/services")
    public Result<?> services() {
        return Result.success(serviceItemService.findAllForAdmin());
    }

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public Result<?> orders(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String status) {
        return Result.success(orderService.adminPage(page, size, status));
    }

    @Operation(summary = "派单")
    @PostMapping("/orders/{id}/assign")
    public Result<?> assignOrder(@PathVariable Long id, @RequestParam Long boosterId) {
        return Result.success(orderService.assignOrder(id, boosterId));
    }

    @Operation(summary = "强制完成")
    @PostMapping("/orders/{id}/force-done")
    public Result<?> forceDone(@PathVariable Long id) {
        return Result.success(orderService.confirmOrder(id));
    }

    @Operation(summary = "标记争议")
    @PostMapping("/orders/{id}/dispute")
    public Result<?> disputeOrder(@PathVariable Long id) {
        return Result.success(orderService.disputeOrder(id));
    }

    // ===== 老板管理 =====
    @Operation(summary = "老板列表")
    @GetMapping("/bosses")
    public Result<?> bosses(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.page(page, size, "boss"));
    }

    @Operation(summary = "更新老板状态")
    @PutMapping("/bosses/{id}/status")
    public Result<?> updateBossStatus(@PathVariable Long id, @RequestParam String status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    // ===== 陪陪管理 =====
    @Operation(summary = "陪陪列表")
    @GetMapping("/boosters")
    public Result<?> boosters(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.page(page, size, "booster"));
    }

    @Operation(summary = "更新陪陪状态")
    @PutMapping("/boosters/{id}/status")
    public Result<?> updateBoosterStatus(@PathVariable Long id, @RequestParam String status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    // ===== 公告管理 =====
    @Operation(summary = "公告列表")
    @GetMapping("/announcements")
    public Result<?> announcements(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(announcementService.page(page, size));
    }

    @Operation(summary = "创建公告")
    @PostMapping("/announcements")
    public Result<?> createAnnouncement(@RequestBody Announcement a) {
        announcementService.create(a);
        return Result.success();
    }

    @Operation(summary = "更新公告")
    @PutMapping("/announcements/{id}")
    public Result<?> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement a) {
        a.setId(id);
        announcementService.update(a);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/announcements/{id}")
    public Result<?> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }

    // ===== 礼物记录 =====
    @Operation(summary = "礼物记录列表")
    @GetMapping("/gifts")
    public Result<?> gifts(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(giftService.page(page, size));
    }

    // ===== 结算管理 =====
    @Operation(summary = "结算列表")
    @GetMapping("/settlements")
    public Result<?> settlements(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(settlementService.page(page, size));
    }

    @Operation(summary = "更新结算状态")
    @PutMapping("/settlements/{id}")
    public Result<?> updateSettlement(@PathVariable Long id,
                                      @RequestParam String status,
                                      @RequestParam(required = false) String remark) {
        settlementService.updateStatus(id, status, remark);
        return Result.success();
    }

    // ===== 提现管理 =====
    @Operation(summary = "提现申请列表")
    @GetMapping("/withdrawals")
    public Result<?> withdrawals(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String status) {
        return Result.success(withdrawalService.adminPage(page, size, status));
    }

    @Operation(summary = "审核提现申请（approve/reject/paid）")
    @PutMapping("/withdrawals/{id}")
    public Result<?> reviewWithdrawal(@PathVariable Long id,
                                      @RequestParam String action,
                                      @RequestParam(required = false) String remark) {
        return Result.success(withdrawalService.review(id, action, remark));
    }
}
