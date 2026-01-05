package com.ticket.system.service.impl;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.dto.response.StationInfoDTO;
import com.ticket.system.entity.Station;
import com.ticket.system.entity.TrainStation;
import com.ticket.system.mapper.StationMapper;
import com.ticket.system.mapper.TrainStationMapper;
import com.ticket.system.service.StationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StationServiceImpl implements StationService {

    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private TrainStationMapper trainStationMapper;

    @Override
    public StationInfoDTO getStationById(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new BusinessException(ErrorCode.STATION_NOT_EXIST.getCode(), "车站不存在");
        }

        StationInfoDTO stationInfoDTO = new StationInfoDTO();
        BeanUtils.copyProperties(station, stationInfoDTO);
        return stationInfoDTO;
    }

    @Override
    public StationInfoDTO getStationByCode(String stationCode) {
        Station station = stationMapper.selectByStationCode(stationCode);
        if (station == null) {
            throw new BusinessException(ErrorCode.STATION_NOT_EXIST.getCode(), "车站不存在");
        }

        StationInfoDTO stationInfoDTO = new StationInfoDTO();
        BeanUtils.copyProperties(station, stationInfoDTO);
        return stationInfoDTO;
    }

    @Override
    public StationInfoDTO getStationByName(String stationName) {
        Station station = stationMapper.selectByStationName(stationName);
        if (station == null) {
            throw new BusinessException(ErrorCode.STATION_NOT_EXIST.getCode(), "车站不存在");
        }

        StationInfoDTO stationInfoDTO = new StationInfoDTO();
        BeanUtils.copyProperties(station, stationInfoDTO);
        return stationInfoDTO;
    }

    @Override
    public List<StationInfoDTO> getAllStations() {
        List<Station> stations = stationMapper.selectAll();
        return stations.stream()
                .map(station -> {
                    StationInfoDTO dto = new StationInfoDTO();
                    BeanUtils.copyProperties(station, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<StationInfoDTO> getStationsByCity(String city) {
        List<Station> stations = stationMapper.selectByCity(city);
        return stations.stream()
                .map(station -> {
                    StationInfoDTO dto = new StationInfoDTO();
                    BeanUtils.copyProperties(station, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<StationInfoDTO> getStationsByProvince(String province) {
        List<Station> stations = stationMapper.selectByProvince(province);
        return stations.stream()
                .map(station -> {
                    StationInfoDTO dto = new StationInfoDTO();
                    BeanUtils.copyProperties(station, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<StationInfoDTO> searchStations(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAllStations();
        }

        List<Station> stations = stationMapper.searchByKeyword(keyword);
        return stations.stream()
                .map(station -> {
                    StationInfoDTO dto = new StationInfoDTO();
                    BeanUtils.copyProperties(station, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void addStation(StationInfoDTO stationInfoDTO) {
        Station station = new Station();
        BeanUtils.copyProperties(stationInfoDTO, station);

        station.setCreateTime(new Date());
        station.setUpdateTime(new Date());

        int result = stationMapper.insert(station);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "添加车站失败");
        }
    }

    @Override
    public void updateStation(StationInfoDTO stationInfoDTO) {
        Station station = stationMapper.selectById(stationInfoDTO.getId());
        if (station == null) {
            throw new BusinessException(ErrorCode.STATION_NOT_EXIST.getCode(), "车站不存在");
        }

        BeanUtils.copyProperties(stationInfoDTO, station);
        station.setUpdateTime(new Date());

        int result = stationMapper.update(station);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "更新车站失败");
        }
    }

    @Override
    public void deleteStation(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new BusinessException(ErrorCode.STATION_NOT_EXIST.getCode(), "车站不存在");
        }

        int result = stationMapper.delete(id);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "删除车站失败");
        }
    }

    @Override
    public List<TrainStation> getTrainStationTrainId(Long id) {
        return trainStationMapper.selectByTrainId(id);
    }

    @Override
    public TrainStation getByTrainIdAndIndex(Long id, int endIndex) {
        return trainStationMapper.selectByTrainAndStationIndex(id,endIndex);
    }
}