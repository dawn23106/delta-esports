package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.config.PaymentProperties;
import com.delta.esports.dto.PaymentStatusResponse;
import com.delta.esports.dto.PreparePaymentResponse;
import com.delta.esports.entity.Order;
import com.delta.esports.entity.PaymentOrder;
import com.delta.esports.mapper.OrderMapper;
import com.delta.esports.mapper.PaymentOrderMapper;
import com.delta.esports.mapper.UserMapper;
import com.delta.esports.payment.WeChatMiniProgramClient;
import com.delta.esports.payment.PaymentGateway;
import com.delta.esports.payment.PaymentGatewayRouter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {
    private final PaymentOrderMapper paymentOrderMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final PaymentGateway paymentGateway;
    private final WeChatMiniProgramClient weChatClient;
    private final PaymentProperties properties;
    private final Cache<Long, Boolean> queryThrottle = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

    public PaymentService(PaymentOrderMapper paymentOrderMapper, OrderMapper orderMapper, UserMapper userMapper,
                          PaymentGatewayRouter gatewayRouter, WeChatMiniProgramClient weChatClient,
                          PaymentProperties properties) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.paymentGateway = gatewayRouter.active();
        this.weChatClient = weChatClient;
        this.properties = properties;
    }

    @Transactional
    public PreparePaymentResponse prepare(Long userId, Long orderId, String loginCode) {
        Order order = orderMapper.selectForUpdate(orderId);
        requireOwnership(userId, order);
        if (!"pending_payment".equals(order.getStatus())) {
            throw new BusinessException(400, "当前订单无需支付或状态已变化");
        }

        PaymentOrder payment = findByOrderId(orderId);
        if (payment == null) {
            payment = new PaymentOrder();
            payment.setOrderId(orderId);
            payment.setOutTradeNo(order.getOrderNo());
            payment.setProvider(paymentGateway.providerCode());
            payment.setStatus("created");
            payment.setAmount(order.getAmount());
            payment.setMerchantId(paymentGateway.merchantId());
            payment.setRefundedAmount(BigDecimal.ZERO);
            paymentOrderMapper.insert(payment);
        }

        if (properties.isMockEnabled()) {
            payment.setStatus("prepared");
            paymentOrderMapper.updateById(payment);
            return response(order, payment, true, Collections.emptyMap());
        }

        String openId = weChatClient.exchangeCodeForOpenId(loginCode);
        Map<String, Object> payParams = paymentGateway.prepareMiniProgramPayment(
                payment.getOutTradeNo(), payment.getAmount(), safeBody(order.getServiceName()), openId);
        payment.setStatus("prepared");
        payment.setFailureReason(null);
        paymentOrderMapper.updateById(payment);
        return response(order, payment, false, payParams);
    }

    @Transactional
    public PaymentStatusResponse confirmMock(Long userId, Long orderId) {
        if (!properties.isMockEnabled()) throw new BusinessException(404, "模拟支付接口不可用");
        Order order = requireOwnedOrder(userId, orderId);
        PaymentOrder payment = findByOrderId(orderId);
        if (payment == null) throw new BusinessException(400, "请先创建支付订单");
        markPaid(order, payment, "MOCK-" + payment.getOutTradeNo(), "MOCK-PAY");
        return status(orderMapper.selectById(orderId), paymentOrderMapper.selectById(payment.getId()));
    }

    @Transactional
    public PaymentStatusResponse queryAndSync(Long userId, Long orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        PaymentOrder payment = findByOrderId(orderId);
        if (payment == null) return status(order, null);
        if (!properties.isMockEnabled()
                && ("created".equals(payment.getStatus()) || "prepared".equals(payment.getStatus()))) {
            if (queryThrottle.getIfPresent(orderId) != null) {
                throw new BusinessException(429, "支付结果查询过于频繁，请10秒后重试");
            }
            queryThrottle.put(orderId, Boolean.TRUE);
            Map<String, Object> remote = paymentGateway.query(payment.getOutTradeNo());
            if (number(remote.get("payStatus")) == 1) {
                validateRemotePayment(payment, remote);
                markPaid(order, payment, string(remote.get("orderNo")), string(remote.get("payNo")));
            }
        }
        return status(orderMapper.selectById(orderId), paymentOrderMapper.selectById(payment.getId()));
    }

    @Transactional
    public String handlePayCallback(Map<String, String> callback) {
        if (!properties.isEnabled()) return "PAYMENT_DISABLED";
        if (!paymentGateway.verifyPayCallback(callback)) return "SIGN_FAIL";
        if (!"1".equals(callback.get("code"))) return "PAY_FAIL";
        if (!paymentGateway.merchantId().equals(callback.get("mchId"))) return "MCH_ID_FAIL";

        PaymentOrder payment = findByOutTradeNo(callback.get("outTradeNo"));
        if (payment == null) return "ORDER_NOT_FOUND";
        if (!sameMoney(payment.getAmount(), callback.get("money"))) return "AMOUNT_FAIL";
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order == null) return "ORDER_NOT_FOUND";
        markPaid(order, payment, callback.get("orderNo"), callback.get("payNo"));
        return "SUCCESS";
    }

    @Transactional
    public String handleRefundCallback(Map<String, String> callback) {
        if (!properties.isEnabled()) return "PAYMENT_DISABLED";
        if (!paymentGateway.verifyRefundCallback(callback)) return "SIGN_FAIL";
        if (!"1".equals(callback.get("code"))) return "REFUND_FAIL";
        if (!paymentGateway.merchantId().equals(callback.get("mchId"))) return "MCH_ID_FAIL";

        PaymentOrder payment = findByOutTradeNo(callback.get("outTradeNo"));
        if (payment == null) return "ORDER_NOT_FOUND";
        if (!sameMoney(payment.getAmount(), callback.get("refundMoney"))) return "AMOUNT_FAIL";
        finishRefund(payment, orderMapper.selectById(payment.getOrderId()), callback.get("refundNo"));
        return "SUCCESS";
    }

    @Transactional
    public Order cancelAndRefund(Order order) {
        order = orderMapper.selectForUpdate(order.getId());
        if (order == null) throw new BusinessException(404, "订单不存在");
        PaymentOrder payment = findByOrderId(order.getId());
        if ("pending_payment".equals(order.getStatus())) {
            if (orderMapper.cancelUnpaid(order.getId()) == 0) {
                throw new BusinessException(409, "订单状态已变化，请刷新后重试");
            }
            if (payment != null && "prepared".equals(payment.getStatus()) && !properties.isMockEnabled()) {
                paymentGateway.close(payment.getOutTradeNo());
                payment.setStatus("closed");
                paymentOrderMapper.updateById(payment);
            }
            return orderMapper.selectById(order.getId());
        }
        if (payment == null || !"paid".equals(payment.getStatus())) {
            throw new BusinessException(409, "未找到已支付记录，请联系管理员处理");
        }
        if (orderMapper.reserveRefund(order.getId()) == 0) {
            throw new BusinessException(409, "订单状态已变化，请刷新后重试");
        }
        String refundRequestNo = "RF" + payment.getOutTradeNo();
        if (paymentOrderMapper.reserveRefund(payment.getId(), refundRequestNo) == 0) {
            throw new BusinessException(409, "退款已在处理中，请勿重复提交");
        }
        if (properties.isMockEnabled()) {
            payment.setStatus("refunding");
            finishRefund(payment, order, "MOCK-" + refundRequestNo);
            return orderMapper.selectById(order.getId());
        }

        Map<String, Object> result = paymentGateway.refund(payment.getOutTradeNo(), payment.getAmount(), refundRequestNo);
        paymentOrderMapper.updateRefundNo(payment.getId(),
                string(result.getOrDefault("refundNo", refundRequestNo)));
        return orderMapper.selectById(order.getId());
    }

    private void validateRemotePayment(PaymentOrder payment, Map<String, Object> remote) {
        String remoteTradeNo = string(remote.get("outTradeNo"));
        if (!payment.getOutTradeNo().equals(remoteTradeNo)) throw new BusinessException(409, "支付订单号校验失败");
        Object remoteMoney = remote.get("money");
        if (remoteMoney != null && !sameMoney(payment.getAmount(), String.valueOf(remoteMoney))) {
            throw new BusinessException(409, "支付金额校验失败");
        }
        Object remoteMerchant = remote.get("mchid");
        if (remoteMerchant != null && !paymentGateway.merchantId().equals(String.valueOf(remoteMerchant))) {
            throw new BusinessException(409, "支付商户号校验失败");
        }
        Object payChannel = remote.get("payChannel");
        if (payChannel != null && !"wxpay".equals(String.valueOf(payChannel))) {
            throw new BusinessException(409, "支付渠道校验失败");
        }
    }

    private void markPaid(Order order, PaymentOrder payment, String providerOrderNo, String providerPayNo) {
        order = orderMapper.selectForUpdate(order.getId());
        if (order == null) throw new BusinessException(404, "订单不存在");
        if ("refunding".equals(payment.getStatus()) || "refunded".equals(payment.getStatus())) return;
        if ("paid".equals(payment.getStatus())) {
            publishPaidOrder(order);
            return;
        }
        if (paymentOrderMapper.markPaid(payment.getId(), providerOrderNo, providerPayNo) == 0) {
            PaymentOrder current = paymentOrderMapper.selectById(payment.getId());
            if (current != null && "paid".equals(current.getStatus())) {
                publishPaidOrder(order);
            }
            return;
        }
        if (!publishPaidOrder(order)) {
            Order currentOrder = orderMapper.selectById(order.getId());
            if (currentOrder != null && "cancelled".equals(currentOrder.getStatus())) {
                paymentOrderMapper.markPaidReview(payment.getId(),
                        "订单取消后收到支付成功通知，需要人工退款");
                return;
            }
            if (currentOrder != null && !"pending".equals(currentOrder.getStatus())
                    && !"assigned".equals(currentOrder.getStatus())) {
                throw new BusinessException(409, "业务订单状态与支付结果不一致");
            }
        }
    }

    /**
     * 发布已支付订单，并在同一事务中占用玩家预选的打手。
     * 预选打手已忙时自动清空预选人，让订单进入公共订单池。
     */
    private boolean publishPaidOrder(Order order) {
        if (!"pending_payment".equals(order.getStatus())) return true;

        boolean boosterReserved = false;
        if (order.getBoosterId() != null) {
            boosterReserved = userMapper.reserveBooster(order.getBoosterId()) == 1;
            if (!boosterReserved) {
                orderMapper.clearPreassignedBooster(order.getId(), order.getBoosterId());
            }
        }

        boolean published = orderMapper.publishPaid(order.getId()) == 1;
        if (!published && boosterReserved) userMapper.releaseBooster(order.getBoosterId());
        return published;
    }

    private void finishRefund(PaymentOrder payment, Order order, String refundNo) {
        if ("refunded".equals(payment.getStatus())) return;
        if (paymentOrderMapper.finishRefund(payment.getId(), refundNo, payment.getAmount()) == 0) {
            PaymentOrder current = paymentOrderMapper.selectById(payment.getId());
            if (current != null && "refunded".equals(current.getStatus())) return;
            throw new BusinessException(409, "退款状态已变化");
        }
        if (order != null) orderMapper.finishRefund(order.getId());
    }

    private PreparePaymentResponse response(Order order, PaymentOrder payment, boolean mock,
                                            Map<String, Object> params) {
        return PreparePaymentResponse.builder()
                .orderId(order.getId()).outTradeNo(payment.getOutTradeNo())
                .status(payment.getStatus()).mock(mock).paymentParams(params).build();
    }

    private PaymentStatusResponse status(Order order, PaymentOrder payment) {
        return PaymentStatusResponse.builder()
                .orderId(order.getId()).orderStatus(order.getStatus())
                .paymentStatus(payment == null ? "not_created" : payment.getStatus())
                .outTradeNo(payment == null ? null : payment.getOutTradeNo())
                .amount(order.getAmount()).build();
    }

    private Order requireOwnedOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        requireOwnership(userId, order);
        return order;
    }

    private void requireOwnership(Long userId, Order order) {
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!order.getBossId().equals(userId)) throw new BusinessException(403, "无权操作该订单");
    }

    private PaymentOrder findByOrderId(Long orderId) {
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderId, orderId));
    }

    private PaymentOrder findByOutTradeNo(String outTradeNo) {
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOutTradeNo, outTradeNo));
    }

    private boolean sameMoney(BigDecimal expected, String actual) {
        try { return expected.compareTo(new BigDecimal(actual)) == 0; }
        catch (Exception e) { return false; }
    }

    private int number(Object value) {
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Exception e) { return 0; }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safeBody(String name) {
        String body = name == null || name.isBlank() ? "沧月电竞服务" : name;
        return body.length() > 40 ? body.substring(0, 40) : body;
    }
}
