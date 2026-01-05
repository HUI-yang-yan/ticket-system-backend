package com.ticket.system.mapper;

import com.ticket.system.entity.TrainStation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.NotNull;
import java.util.List;

@Mapper
public interface TrainStationMapper {
    int insert(TrainStation trainStation);
    int update(TrainStation trainStation);
    int delete(Long id);
    TrainStation selectById(Long id);
    TrainStation selectByTrainAndStation(@Param("trainId") Long trainId,
                                         @Param("stationId") Long stationId);
    List<TrainStation> selectByTrainId(Long trainId);
    List<TrainStation> selectByStationId(Long stationId);
    TrainStation selectByTrainAndIndex(@Param("trainId") Long trainId,
                                       @Param("stationIndex") Integer stationIndex);
    List<TrainStation> selectTrainRoute(@Param("trainId") Long trainId);

    void deleteByTrainId(@Param("trainId") Long trainId);


    Integer selectStationIndexByTrainIdAndStationId(@Param("trainId") Long trainId, @NotNull(message = "出发站ID不能为空")
                                                    @Param("departureStationId") Long departureStationId);

    List<TrainStation> selectByTrainIdAndOrderIndex(@Param("trainId") Long trainId,
                                                    @Param("startStationIndex") Integer startStationIndex,
                                                    @Param("endStationIndex") Integer endStationIndex);

    TrainStation selectByTrainAndStationIndex(Long trainId, int stationIndex);

}