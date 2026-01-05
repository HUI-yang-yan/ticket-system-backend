package com.ticket.system.mapper;


import com.ticket.system.entity.TrainSegmentStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TrainSegmentStockMapper {

    @Select("select stock from ticket_system.train_segment_stock where train_id = #{trainId} and start_station_id = #{stationId}")
    Integer selectStockByTrainIdAndStationId(Long trainId, Long stationId);
    @Select("select * from ticket_system.train_segment_stock" +
            " where train_id = #{trainId} and travel_date  = #{departureDate} and seat_type = #{seatType}")
    TrainSegmentStock selectByTrainIDAndDateAndType(Long trainId, LocalDate departureDate, String seatType);

    Integer selectMinStock(Long trainId, LocalDate departureDate, String seatType, Integer startIndex, Integer endIndex);

    BigDecimal selectSumPrice(
            @Param("trainId") Long trainId,
            @Param("travelDate") LocalDate travelDate,
            @Param("seatType") String seatType,
            @Param("startIndex") Integer startIndex,
            @Param("endIndex") Integer endIndex
    );

    void insertBatch(List<TrainSegmentStock> trainSegmentStocks);

}
