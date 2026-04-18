package com.ticket.system.service;

import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.entity.TicketInventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Date;
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

}