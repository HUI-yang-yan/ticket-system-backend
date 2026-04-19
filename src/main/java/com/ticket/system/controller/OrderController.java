package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.OrderCreateDTO;
import com.ticket.system.dto.response.OrderInfoDTO;
import com.ticket.system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单创建、查询、取消、支付")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "创建订单", description = "创建新的车票订单")
    public Result<OrderInfoDTO> createOrder(@RequestBody @Valid OrderCreateDTO orderCreateDTO) {
        log.info("创建订单: trainId={}, departureStationId={}, arrivalStationId={}",
                orderCreateDTO.getTrainId(), orderCreateDTO.getDepartureStationId(), orderCreateDTO.getArrivalStationId());

        OrderInfoDTO orderInfo = orderService.createOrder(orderCreateDTO);
        return Result.success("订单创建成功", orderInfo);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "根据ID查询订单", description = "通过订单ID获取订单详细信息")
    public Result<OrderInfoDTO> getOrderById(@PathVariable Long orderId) {
        OrderInfoDTO order = orderService.getOrderById(orderId);
        return Result.success(order);
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "根据订单号查询", description = "通过订单号精确查询订单")
    public Result<OrderInfoDTO> getOrderByNumber(@PathVariable String orderNumber) {
        OrderInfoDTO order = orderService.getOrderByNumber(orderNumber);
        return Result.success(order);
    }

    @GetMapping("/user/list")
    @Operation(summary = "获取用户订单列表", description = "获取当前登录用户的所有订单")
    public Result<List<OrderInfoDTO>> getUserOrders() {
        Long userId = ThreadLocalUtil.getUserId();
        List<OrderInfoDTO> orders = orderService.getUserOrders(userId);
        return Result.success(orders);
    }

    @PostMapping("/cancel/{orderId}")
    @Operation(summary = "取消订单", description = "取消指定订单ID的订单")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        log.info("取消订单: orderId={}", orderId);

        boolean success = orderService.cancelOrder(orderId);
        return Result.success("订单取消成功", success);
    }

    @PostMapping("/pay/{orderId}")
    @Operation(summary = "支付订单", description = "对指定订单进行支付")
    public Result<Boolean> payOrder(@PathVariable Long orderId) {
        log.info("支付订单: orderId={}", orderId);
        boolean success = orderService.payOrder(orderId);
        return Result.success("订单支付成功", success);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态查询订单", description = "根据订单状态（待支付、已取消等）筛选订单")
    public Result<List<OrderInfoDTO>> getOrdersByStatus(@PathVariable Integer status) {
        List<OrderInfoDTO> orders = orderService.getOrdersByStatus(status);
        return Result.success(orders);
    }

    @PostMapping("/check/expired")
    @Operation(summary = "检查过期订单", description = "检查并标记超期未支付的订单")
    public Result<String> checkOrderExpired() {
        orderService.checkOrderExpired();
        return Result.success("已检查过期订单");
    }

    @PostMapping("/auto/cancel")
    @Operation(summary = "自动取消过期订单", description = "系统自动取消超期未支付的订单")
    public Result<String> autoCancelExpiredOrders() {
        orderService.autoCancelExpiredOrders();
        return Result.success("已自动取消过期订单");
    }
}