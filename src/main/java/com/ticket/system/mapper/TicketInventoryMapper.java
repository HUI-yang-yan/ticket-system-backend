package com.ticket.system.mapper;

import com.ticket.system.entity.TicketInventory;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Mapper
public interface TicketInventoryMapper {
    int insert(TicketInventory inventory);
    int update(TicketInventory inventory);
    int delete(Long id);
    TicketInventory selectById(Long id);
    TicketInventory selectByTrainAndDateAndType(@Param("trainId") Long trainId,
                                                @Param("departureDate") LocalDate departureDate,
                                                @Param("seatType") String seatType);
    List<TicketInventory> selectByTrainAndDate(@Param("trainId") Long trainId,
                                               @Param("departureDate") String departureDate);
    int reduceInventory(@Param("id") Long id,
                        @Param("availableCount") Integer availableCount,
                        @Param("version") Integer version);
    int increaseInventory(@Param("id") Long id,
                          @Param("availableCount") Integer availableCount,
                          @Param("version") Integer version);
    TicketInventory selectForUpdate(@Param("trainId") Long trainId,
                                    @Param("departureDate") String departureDate,
                                    @Param("seatType") String seatType);

    TicketInventory selectByTrainIds(List<Long> ticketIds);

    List<TicketInventory> selectAll();

}