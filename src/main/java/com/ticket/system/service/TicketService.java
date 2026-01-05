package com.ticket.system.service;

import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.entity.TicketInventory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface TicketService {
    List<TicketInfoDTO> queryTickets(TicketQueryDTO queryDTO);
    TicketInfoDTO getTicketDetail(Long ticketId);
    boolean unlockTicket(Long ticketId, Long userId);
    boolean purchaseTicket(Long ticketId, Long userId);
    void syncTicketInventory();
    void addTicketsByHands(List<Long> ticketIds);

    List<TicketInventory> getAllTicketInventory();

}