package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.PaymentDTO;
import com.ticket.system.dto.response.PaymentResultDTO;
import com.ticket.system.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create")
    public Result<PaymentResultDTO> createPayment(@RequestBody @Valid PaymentDTO paymentDTO) {
        log.info("创建支付: orderId={}, paymentAmount={}",
                paymentDTO.getOrderId(), paymentDTO.getPaymentAmount());

        PaymentResultDTO paymentResult = paymentService.createPayment(paymentDTO);
        return Result.success("支付创建成功", paymentResult);
    }

    @GetMapping("/order/{orderId}")
    public Result<PaymentResultDTO> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResultDTO payment = paymentService.getPaymentByOrderId(orderId);
        return Result.success(payment);
    }

    @GetMapping("/number/{paymentNumber}")
    public Result<PaymentResultDTO> getPaymentByNumber(@PathVariable String paymentNumber) {
        PaymentResultDTO payment = paymentService.getPaymentByNumber(paymentNumber);
        return Result.success(payment);
    }

    @PostMapping("/callback")
    public Result<Boolean> processPaymentCallback(
            @RequestParam String paymentNumber,
            @RequestParam String status) {
        log.info("支付回调: paymentNumber={}, status={}", paymentNumber, status);

        boolean success = paymentService.processPaymentCallback(paymentNumber, status);
        return Result.success("回调处理成功", success);
    }

    @PostMapping("/refund/{orderId}")
    public Result<Boolean> refundPayment(@PathVariable Long orderId) {
        log.info("退款申请: orderId={}", orderId);

        boolean success = paymentService.refundPayment(orderId);
        return Result.success("退款申请成功", success);
    }
}