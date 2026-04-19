package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.TicketInventoryCreateDTO;
import com.ticket.system.dto.request.TicketInventoryQueryDTO;
import com.ticket.system.dto.response.TicketInventoryDTO;
import com.ticket.system.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ticket/inventory")
@Tag(name = "票务配置管理", description = "车票库存配置管理，用于配置车次座位类型的总票数、可用票数及票价")
public class TicketInventoryController {

    @Autowired
    private TicketService ticketService;

    /**
     * 创建票务配置
     * 根据车次ID和座位类型创建一条票务配置记录，用于后续补票和售票
     */
    @PostMapping
    @Operation(summary = "创建票务配置", description = "为指定车次和座位类型创建票务配置，创建后可配置总票数和票价")
    public Result<TicketInventoryDTO> create(@Validated @RequestBody TicketInventoryCreateDTO dto) {
        log.info("创建票务配置: trainId={}, seatType={}, totalCount={}",
                dto.getTrainId(), dto.getSeatType(), dto.getTotalCount());
        TicketInventoryDTO result = ticketService.createTicketInventory(dto);
        return Result.success(result);
    }

    /**
     * 批量创建票务配置
     */
    @PostMapping("/batch")
    @Operation(summary = "批量创建票务配置", description = "批量创建多条票务配置，支持一次为多个座位类型创建配置")
    public Result<String> batchCreate(@Validated @RequestBody List<TicketInventoryCreateDTO> dtos) {
        log.info("批量创建票务配置: count={}", dtos.size());
        ticketService.batchCreateTicketInventory(dtos);
        return Result.success("批量创建成功，数量：" + dtos.size());
    }

    /**
     * 更新票务配置
     * 用于修改某个车次座位类型的总票数、可用票数或票价
     */
    @PutMapping
    @Operation(summary = "更新票务配置", description = "更新指定车次和座位类型的票务配置信息，包括总票数、可用票数、票价、状态")
    public Result<TicketInventoryDTO> update(@Validated @RequestBody TicketInventoryCreateDTO dto) {
        log.info("更新票务配置: trainId={}, seatType={}",
                dto.getTrainId(), dto.getSeatType());
        TicketInventoryDTO result = ticketService.updateTicketInventory(dto);
        return Result.success(result);
    }

    /**
     * 删除票务配置
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除票务配置", description = "根据ID删除票务配置，删除后该配置将不再生效")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除票务配置: id={}", id);
        boolean result = ticketService.deleteTicketInventory(id);
        return Result.success(result);
    }

    /**
     * 查询单个票务配置
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询单个票务配置", description = "根据ID查询票务配置的详细信息")
    public Result<TicketInventoryDTO> getById(@PathVariable Long id) {
        TicketInventoryDTO dto = ticketService.getTicketInventoryById(id);
        return Result.success(dto);
    }

    /**
     * 条件查询票务配置列表
     */
    @GetMapping("/list")
    @Operation(summary = "条件查询票务配置", description = "根据车次ID、座位类型等条件查询票务配置列表")
    public Result<List<TicketInventoryDTO>> list(TicketInventoryQueryDTO queryDTO) {
        log.info("查询票务配置列表: trainId={}, seatType={}",
                queryDTO.getTrainId(), queryDTO.getSeatType());
        List<TicketInventoryDTO> results = ticketService.queryTicketInventories(queryDTO);
        return Result.success(results);
    }

    /**
     * 获取所有票务配置
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有票务配置", description = "查询系统中所有票务配置信息")
    public Result<List<TicketInventoryDTO>> getAll() {
        TicketInventoryQueryDTO queryDTO = new TicketInventoryQueryDTO();
        List<TicketInventoryDTO> results = ticketService.queryTicketInventories(queryDTO);
        return Result.success(results);
    }
}
