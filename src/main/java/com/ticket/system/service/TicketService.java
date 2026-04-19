package com.ticket.system.service;

import com.ticket.system.dto.request.TicketInventoryCreateDTO;
import com.ticket.system.dto.request.TicketInventoryQueryDTO;
import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.dto.response.TicketInventoryDTO;
import com.ticket.system.entity.TicketInventory;

import java.util.List;

public interface TicketService {
    List<TicketInfoDTO> queryTickets(TicketQueryDTO queryDTO);
    TicketInfoDTO getTicketDetail(Long ticketId);
    boolean unlockTicket(Long ticketId, Long userId);
    boolean purchaseTicket(Long trainId, Long userId, String departureDate, String seatType,
                          Long startStationId, Long endStationId);
    void syncTicketInventory();
    void addTicketsByHands(List<Long> ticketIds);
    List<TicketInventory> getAllTicketInventory();

    // 票务库存配置管理
    TicketInventoryDTO createTicketInventory(TicketInventoryCreateDTO dto);
    TicketInventoryDTO updateTicketInventory(TicketInventoryCreateDTO dto);
    boolean deleteTicketInventory(Long id);
    TicketInventoryDTO getTicketInventoryById(Long id);
    List<TicketInventoryDTO> queryTicketInventories(TicketInventoryQueryDTO queryDTO);
    void batchCreateTicketInventory(List<TicketInventoryCreateDTO> dtos);
}