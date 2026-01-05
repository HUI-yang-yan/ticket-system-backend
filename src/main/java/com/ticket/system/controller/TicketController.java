package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/query")
    public Result<List<TicketInfoDTO>> queryTickets(@RequestBody TicketQueryDTO queryDTO) {
        log.info("查询车票: departureStationId={}, arrivalStationId={}, departureDate={}",
                queryDTO.getDepartureStationId(), queryDTO.getArrivalStationId(), queryDTO.getDepartureDate());

        List<TicketInfoDTO> tickets = ticketService.queryTickets(queryDTO);
        return Result.success(tickets);
    }

    @GetMapping("/{ticketId}")
    public Result<TicketInfoDTO> getTicketDetail(@PathVariable Long ticketId) {
        TicketInfoDTO ticket = ticketService.getTicketDetail(ticketId);
        return Result.success(ticket);
    }

    @PostMapping("/purchase/{trainId}")
    public Result<Boolean> purchaseTicket(@PathVariable Long trainId) {
        log.info("购买车票: trainId={}", trainId);

        Long userId = getCurrentUserId();
        boolean success = ticketService.purchaseTicket(trainId, userId);
        return Result.success(success);
    }

    @PostMapping("/sync")
    public Result<String> syncTicketInventory() {
        ticketService.syncTicketInventory();
        return Result.success("同步票务库存成功");
    }

    @PostMapping("/add/ticket/{ticketIds}")
    public Result<String> updateTicketsByHands(@PathVariable List<Long> ticketIds) {
        ticketService.addTicketsByHands(ticketIds);
        return Result.success("手动更新票信息成功!");
    }

    private Long getCurrentUserId() {
        return ThreadLocalUtil.getUserId();
    }
}