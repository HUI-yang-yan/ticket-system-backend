package com.ticket.system.mapper;

import com.ticket.system.entity.Train;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainMapper {
    int insert(Train train);
    int update(Train train);
    int delete(Long id);
    Train selectById(Long id);
    Train selectByTrainNumber(String trainNumber);
    List<Train> selectByStartStation(Long startStationId);
    List<Train> selectByEndStation(Long endStationId);
    List<Train> selectByType(String trainType);
    List<Train> selectAll();
    List<Train> selectByRoute(@Param("startStationId") Long startStationId,
                              @Param("endStationId") Long endStationId);
}