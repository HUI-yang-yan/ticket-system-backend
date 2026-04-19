package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.PaymentDTO;
import com.ticket.system.dto.response.PaymentResultDTO;
import com.ticket.system.dto.response.RefundCheckResult;
import com.ticket.system.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/payment")
@Tag(name = "支付管理", description = "支付创建、查询、回调、退款")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create")
    @Operation(summary = "创建支付", description = "为订单创建支付记录")
    public Result<PaymentResultDTO> createPayment(@RequestBody @Valid PaymentDTO paymentDTO) {
        log.info("创建支付: orderId={}, paymentAmount={}",
                paymentDTO.getOrderId(), paymentDTO.getPaymentAmount());

        PaymentResultDTO paymentResult = paymentService.createPayment(paymentDTO);
        return Result.success("支付创建成功", paymentResult);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID查询支付", description = "通过订单ID获取支付记录")
    public Result<PaymentResultDTO> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResultDTO payment = paymentService.getPaymentByOrderId(orderId);
        return Result.success(payment);
    }

    @GetMapping("/number/{paymentNumber}")
    @Operation(summary = "根据支付号查询", description = "通过支付号精确查询支付记录")
    public Result<PaymentResultDTO> getPaymentByNumber(@PathVariable String paymentNumber) {
        PaymentResultDTO payment = paymentService.getPaymentByNumber(paymentNumber);
        return Result.success(payment);
    }

    @PostMapping("/callback")
    @Operation(summary = "支付回调", description = "接收第三方支付平台的回调通知")
    public Result<Boolean> processPaymentCallback(
            @RequestParam String paymentNumber,
            @RequestParam String status) {
        log.info("支付回调: paymentNumber={}, status={}", paymentNumber, status);

        boolean success = paymentService.processPaymentCallback(paymentNumber, status);
        return Result.success("回调处理成功", success);
    }

    @PostMapping("/refund/{orderId}")
    @Operation(summary = "申请退款", description = "对指定订单申请退款")
    public Result<Boolean> refundPayment(@PathVariable Long orderId) {
        log.info("退款申请: orderId={}", orderId);

        boolean success = paymentService.refundPayment(orderId);
        return Result.success("退款申请成功", success);
    }

    @GetMapping("/refund/check/{orderId}")
    @Operation(summary = "退款检查", description = "检查订单是否可退及退款金额")
    public Result<RefundCheckResult> refundCheck(@PathVariable Long orderId) {
        log.info("退款检查: orderId={}", orderId);

        RefundCheckResult result = paymentService.refundCheck(orderId);
        return Result.success(result);
    }
}