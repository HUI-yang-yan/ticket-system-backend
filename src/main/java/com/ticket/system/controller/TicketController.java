package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ticket")
@Tag(name = "票务查询", description = "车票查询、购买、同步")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/query")
    @Operation(summary = "查询车票", description = "根据出发站、到达站、日期查询可用车票")
    public Result<List<TicketInfoDTO>> queryTickets(@RequestBody TicketQueryDTO queryDTO) {
        log.info("查询车票: departureStationId={}, arrivalStationId={}, departureDate={}",
                queryDTO.getDepartureStationId(), queryDTO.getArrivalStationId(), queryDTO.getDepartureDate());

        List<TicketInfoDTO> tickets = ticketService.queryTickets(queryDTO);
        return Result.success(tickets);
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "获取车票详情", description = "通过车票ID获取车票详细信息")
    public Result<TicketInfoDTO> getTicketDetail(@PathVariable Long ticketId) {
        TicketInfoDTO ticket = ticketService.getTicketDetail(ticketId);
        return Result.success(ticket);
    }

    @PostMapping("/purchase")
    @Operation(summary = "购买车票", description = "购买指定车次的车票，支持选择座位类型和区间")
    public Result<Boolean> purchaseTicket(@RequestParam Long trainId,
                                         @RequestParam String departureDate,
                                         @RequestParam String seatType,
                                         @RequestParam(required = false) Long startStationId,
                                         @RequestParam(required = false) Long endStationId) {
        log.info("购买车票: trainId={}, departureDate={}, seatType={}", trainId, departureDate, seatType);

        Long userId = getCurrentUserId();
        boolean success = ticketService.purchaseTicket(trainId, userId, departureDate, seatType, startStationId, endStationId);
        return Result.success(success);
    }

    @PostMapping("/sync")
    @Operation(summary = "同步票务库存", description = "从外部系统同步最新票务库存数据")
    public Result<String> syncTicketInventory() {
        ticketService.syncTicketInventory();
        return Result.success("同步票务库存成功");
    }

    @PostMapping("/add/ticket/{ticketIds}")
    @Operation(summary = "手动更新票信息", description = "手动更新指定车票ID的票信息")
    public Result<String> updateTicketsByHands(@PathVariable List<Long> ticketIds) {
        ticketService.addTicketsByHands(ticketIds);
        return Result.success("手动更新票信息成功!");
    }

    private Long getCurrentUserId() {
        return ThreadLocalUtil.getUserId();
    }
}