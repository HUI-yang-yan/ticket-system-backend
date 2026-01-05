package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.OrderCreateDTO;
import com.ticket.system.dto.response.OrderInfoDTO;
import com.ticket.system.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<OrderInfoDTO> createOrder(@RequestBody @Valid OrderCreateDTO orderCreateDTO) {
        log.info("创建订单: trainId={}, departureStationId={}, arrivalStationId={}",
                orderCreateDTO.getTrainId(), orderCreateDTO.getDepartureStationId(), orderCreateDTO.getArrivalStationId());

        OrderInfoDTO orderInfo = orderService.createOrder(orderCreateDTO);
        return Result.success("订单创建成功", orderInfo);
    }

    @GetMapping("/{orderId}")
    public Result<OrderInfoDTO> getOrderById(@PathVariable Long orderId) {
        OrderInfoDTO order = orderService.getOrderById(orderId);
        return Result.success(order);
    }

    @GetMapping("/number/{orderNumber}")
    public Result<OrderInfoDTO> getOrderByNumber(@PathVariable String orderNumber) {
        OrderInfoDTO order = orderService.getOrderByNumber(orderNumber);
        return Result.success(order);
    }

    @GetMapping("/user/list")
    public Result<List<OrderInfoDTO>> getUserOrders() {
        Long userId = ThreadLocalUtil.getUserId();
        List<OrderInfoDTO> orders = orderService.getUserOrders(userId);
        return Result.success(orders);
    }

    @PostMapping("/cancel/{orderId}")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        log.info("取消订单: orderId={}", orderId);

        boolean success = orderService.cancelOrder(orderId);
        return Result.success("订单取消成功", success);
    }

    @PostMapping("/pay/{orderId}")
    public Result<Boolean> payOrder(@PathVariable Long orderId) {
        log.info("支付订单: orderId={}", orderId);
        boolean success = orderService.payOrder(orderId);
        return Result.success("订单支付成功", success);
    }

    @GetMapping("/status/{status}")
    public Result<List<OrderInfoDTO>> getOrdersByStatus(@PathVariable Integer status) {
        List<OrderInfoDTO> orders = orderService.getOrdersByStatus(status);
        return Result.success(orders);
    }

    @PostMapping("/check/expired")
    public Result<String> checkOrderExpired() {
        orderService.checkOrderExpired();
        return Result.success("已检查过期订单");
    }

    @PostMapping("/auto/cancel")
    public Result<String> autoCancelExpiredOrders() {
        orderService.autoCancelExpiredOrders();
        return Result.success("已自动取消过期订单");
    }
}