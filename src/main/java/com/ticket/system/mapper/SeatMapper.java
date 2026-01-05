package com.ticket.system.mapper;

import com.ticket.system.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeatMapper {
    int insert(Seat seat);
    int update(Seat seat);
    int delete(Long id);
    Seat selectById(Long id);
    List<Seat> selectByCarriageId(Long carriageId);
    Seat selectByCarriageAndNumber(@Param("carriageId") Long carriageId,
                                   @Param("seatNumber") String seatNumber);
    List<Seat> selectAvailableSeats(@Param("carriageId") Long carriageId);
    int updateSeatStatus(@Param("id") Long id, @Param("status") Integer status);
    List<Seat> selectByTrainAndDate(@Param("trainId") Long trainId,
                                    @Param("departureDate") String departureDate);
}