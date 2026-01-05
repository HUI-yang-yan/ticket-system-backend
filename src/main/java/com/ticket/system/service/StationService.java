package com.ticket.system.service;

import com.ticket.system.dto.response.StationInfoDTO;
import com.ticket.system.entity.TrainStation;

import java.util.List;

public interface StationService {
    StationInfoDTO getStationById(Long id);
    StationInfoDTO getStationByCode(String stationCode);
    StationInfoDTO getStationByName(String stationName);
    List<StationInfoDTO> getAllStations();
    List<StationInfoDTO> getStationsByCity(String city);
    List<StationInfoDTO> getStationsByProvince(String province);
    List<StationInfoDTO> searchStations(String keyword);
    void addStation(StationInfoDTO stationInfoDTO);
    void updateStation(StationInfoDTO stationInfoDTO);
    void deleteStation(Long id);

    List<TrainStation> getTrainStationTrainId(Long id);

    TrainStation getByTrainIdAndIndex(Long id, int endIndex);
}