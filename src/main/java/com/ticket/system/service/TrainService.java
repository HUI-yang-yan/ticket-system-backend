package com.ticket.system.service;

import com.ticket.system.dto.request.TrainQueryDTO;
import com.ticket.system.dto.response.TrainInfoDTO;
import com.ticket.system.entity.Train;

import java.util.List;

public interface TrainService {
    TrainInfoDTO getTrainInfoById(Long id);
    TrainInfoDTO getTrainByNumber(String trainNumber);
    List<TrainInfoDTO> getAllTrains();
    List<TrainInfoDTO> getTrainsByType(String trainType);
    List<TrainInfoDTO> searchTrains(TrainQueryDTO queryDTO);
    List<TrainInfoDTO> getTrainsByRoute(Long startStationId, Long endStationId);
    void addTrain(TrainInfoDTO trainInfoDTO);
    void updateTrain(TrainInfoDTO trainInfoDTO);
    void deleteTrain(Long id);

    Train getTrainById(Long trainId);
}