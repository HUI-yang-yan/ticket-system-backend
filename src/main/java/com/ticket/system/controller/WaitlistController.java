package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.WaitlistCreateDTO;
import com.ticket.system.dto.response.WaitlistOrderDTO;
import com.ticket.system.dto.response.WaitlistTicketDTO;
import com.ticket.system.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/waitlist")
@Tag(name = "候补购票", description = "候补订单创建、查询、取消及队列管理")
public class WaitlistController {

    @Autowired
    private WaitlistService waitlistService;

    /**
     * 创建候补购票请求
     */
    @PostMapping("/create")
    @Operation(summary = "创建候补购票请求", description = "用户提交候补购票请求，指定车次、座位类型、出行日期")
    public Result<WaitlistOrderDTO> createWaitlistOrder(@RequestBody @Valid WaitlistCreateDTO dto) {
        log.info("创建候补购票请求: trainId={}, startStationId={}, endStationId={}, seatType={}, date={}",
                dto.getTrainId(), dto.getStartStationId(), dto.getEndStationId(),
                dto.getSeatType(), dto.getDepartureDate());

        WaitlistOrderDTO result = waitlistService.createWaitlistOrder(dto);
        return Result.success("候补购票请求创建成功", result);
    }

    /**
     * 取消候补订单
     */
    @PostMapping("/cancel/{id}")
    @Operation(summary = "取消候补订单", description = "取消用户提交的候补购票请求")
    public Result<Boolean> cancelWaitlistOrder(@PathVariable Long id) {
        log.info("取消候补订单: id={}", id);

        boolean success = waitlistService.cancelWaitlistOrder(id);
        return Result.success("候补订单取消成功", success);
    }

    /**
     * 查询用户的候补订单列表
     */
    @GetMapping("/user/list")
    @Operation(summary = "查询用户候补订单列表", description = "获取当前用户的所有候补购票订单")
    public Result<List<WaitlistOrderDTO>> getUserWaitlistOrders() {
        Long userId = ThreadLocalUtil.getUserId();
        List<WaitlistOrderDTO> orders = waitlistService.getUserWaitlistOrders(userId);
        return Result.success(orders);
    }

    /**
     * 根据ID查询候补订单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询候补订单详情", description = "通过候补订单ID获取详细信息")
    public Result<WaitlistOrderDTO> getWaitlistOrderById(@PathVariable Long id) {
        WaitlistOrderDTO order = waitlistService.getWaitlistOrderById(id);
        return Result.success(order);
    }

    /**
     * 查询候补池可用票（按条件筛选）
     */
    @GetMapping("/query")
    @Operation(summary = "查询候补池可用票", description = "按条件筛选候补池中当前可用的车票")
    public Result<List<WaitlistTicketDTO>> queryAvailableTickets(
            @RequestParam(required = false) Long trainId,
            @RequestParam(required = false) Long startStationId,
            @RequestParam(required = false) Long endStationId,
            @RequestParam(required = false) String seatType,
            @RequestParam(required = false) String departureDate) {

        log.info("查询候补池可用票: trainId={}, start={}, end={}, seat={}, date={}",
                trainId, startStationId, endStationId, seatType, departureDate);

        List<WaitlistTicketDTO> tickets = waitlistService.queryAvailableTickets(
                trainId, startStationId, endStationId, seatType, departureDate);
        return Result.success(tickets);
    }

    /**
     * 查询所有候补池可用票
     */
    @GetMapping("/ticket/available")
    @Operation(summary = "查询所有候补池可用票", description = "查询候补池中所有当前可用的车票")
    public Result<List<WaitlistTicketDTO>> getAllAvailableTickets() {
        List<WaitlistTicketDTO> tickets = waitlistService.queryAvailableTickets(
                null, null, null, null, null);
        return Result.success(tickets);
    }

    /**
     * 候补订单支付成功回调（内部接口）
     */
    @PostMapping("/paid/{waitlistNumber}")
    @Operation(summary = "候补订单支付成功回调", description = "候补订单支付成功后，系统处理出票")
    public Result<Boolean> waitlistOrderPaid(@PathVariable String waitlistNumber) {
        log.info("候补订单支付成功: waitlistNumber={}", waitlistNumber);

        waitlistService.handleWaitlistOrderPaid(waitlistNumber);
        return Result.success("候补订单支付处理成功", true);
    }

    /**
     * 手动触发候补队列处理（管理接口）
     */
    @PostMapping("/process")
    @Operation(summary = "手动触发候补队列处理", description = "管理员手动触发候补购票队列处理任务")
    public Result<String> processWaitlist() {
        log.info("手动触发候补队列处理");

        waitlistService.autoProcessWaitlist();
        return Result.success("候补队列处理完成");
    }
}
