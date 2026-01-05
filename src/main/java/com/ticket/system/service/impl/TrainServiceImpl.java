package com.ticket.system.service.impl;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.dto.request.TrainQueryDTO;
import com.ticket.system.dto.request.TrainStationDTO;
import com.ticket.system.dto.response.TrainInfoDTO;
import com.ticket.system.entity.Station;
import com.ticket.system.entity.Train;
import com.ticket.system.entity.TrainStation;
import com.ticket.system.mapper.StationMapper;
import com.ticket.system.mapper.TrainMapper;
import com.ticket.system.mapper.TrainStationMapper;
import com.ticket.system.service.TrainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TrainServiceImpl implements TrainService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private TrainStationMapper trainStationMapper;

    @Autowired
    private StationMapper stationMapper;

    @Override
    public TrainInfoDTO getTrainInfoById(Long id) {
        Train train = trainMapper.selectById(id);
        if (train == null) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_EXIST.getCode(), "列车不存在");
        }

        TrainInfoDTO trainInfoDTO = new TrainInfoDTO();
        BeanUtils.copyProperties(train, trainInfoDTO);
        return trainInfoDTO;
    }

    @Override
    public TrainInfoDTO getTrainByNumber(String trainNumber) {
        Train train = trainMapper.selectByTrainNumber(trainNumber);
        if (train == null) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_EXIST.getCode(), "列车不存在");
        }

        TrainInfoDTO trainInfoDTO = new TrainInfoDTO();
        BeanUtils.copyProperties(train, trainInfoDTO);
        return trainInfoDTO;
    }

    @Override
    public List<TrainInfoDTO> getAllTrains() {
        List<Train> trains = trainMapper.selectAll();
        return trains.stream()
                .map(train -> {
                    Station startStation = stationMapper.selectById(train.getStartStationId());
                    Station endStation = stationMapper.selectById(train.getEndStationId());

                    TrainInfoDTO dto = new TrainInfoDTO();
                    dto.setEndStationName(endStation.getStationName());
                    dto.setStartStationName(startStation.getStationName());

                    List<TrainStation> trainStations = trainStationMapper.selectByTrainId(train.getId());
                    List<TrainStationDTO> trainStationDTOS = new ArrayList<>();
                    trainStations.forEach(trainStation -> {
                        TrainStationDTO trainStationDTO = new TrainStationDTO();
                        trainStationDTO.setStationId(trainStation.getId());
                        trainStationDTO.setStationName(trainStation.getStationName());
                        trainStationDTO.setStationOrder(trainStation.getStationIndex());
                        trainStationDTO.setDepartureTime(trainStation.getDepartureTime());
                        trainStationDTO.setArrivalTime(trainStation.getArrivalTime());
                        trainStationDTO.setStopMinutes(trainStation.getStopDuration());
                        trainStationDTO.setIsSale(true);
                        trainStationDTOS.add(trainStationDTO);
                    });

                    BeanUtils.copyProperties(train, dto);
                    dto.setStationList(trainStationDTOS);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TrainInfoDTO> getTrainsByType(String trainType) {
        List<Train> trains = trainMapper.selectByType(trainType);
        return trains.stream()
                .map(train -> {
                    TrainInfoDTO dto = new TrainInfoDTO();
                    BeanUtils.copyProperties(train, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TrainInfoDTO> searchTrains(TrainQueryDTO queryDTO) {
        List<Train> trains;

        if (StringUtils.hasText(queryDTO.getTrainNumber())) {
            // 按车次号查询
            Train train = trainMapper.selectByTrainNumber(queryDTO.getTrainNumber());

            trains = train != null ? Collections.singletonList(train) : Collections.emptyList();

        } else if (queryDTO.getStartStationId() != null && queryDTO.getEndStationId() != null) {
            // 按起止站查询
            trains = trainMapper.selectByRoute(queryDTO.getStartStationId(), queryDTO.getEndStationId());
        } else {
            // 查询所有
            trains = trainMapper.selectAll();
        }

        return trains.stream()
                .map(train -> {
                    TrainInfoDTO dto = new TrainInfoDTO();
                    BeanUtils.copyProperties(train, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TrainInfoDTO> getTrainsByRoute(Long startStationId, Long endStationId) {
        List<Train> trains = trainMapper.selectByRoute(startStationId, endStationId);
        return trains.stream()
                .map(train -> {
                    TrainInfoDTO dto = new TrainInfoDTO();
                    BeanUtils.copyProperties(train, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void addTrain(TrainInfoDTO trainInfoDTO) {
        Train train = new Train();
        BeanUtils.copyProperties(trainInfoDTO, train);

        // 计算运行时长（分钟）
        if (train.getStartTime() != null && train.getEndTime() != null) {
            int duration = (train.getEndTime().getMinute() - train.getStartTime().getMinute()) ;
            train.setDuration(duration);
        }

        train.setCreateTime(new Date());
        train.setUpdateTime(new Date());

        int result = trainMapper.insert(train);
        for (TrainStationDTO trainStationDTO : trainInfoDTO.getStationList()) {
            TrainStation trainStation = new TrainStation();
            trainStation.setTrainId(train.getId());
            trainStation.setStationIndex(trainStationDTO.getStationOrder());
            trainStation.setStationId(trainStationDTO.getStationId());
            trainStation.setDepartureTime(trainStationDTO.getDepartureTime());
            trainStation.setArrivalTime(trainStationDTO.getArrivalTime());
            trainStation.setStopDuration(trainStationDTO.getStopMinutes());

            trainStationMapper.insert(trainStation);
        }

        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "添加列车失败");
        }
    }

    @Override
    public void updateTrain(TrainInfoDTO trainInfoDTO) {
        Train train = trainMapper.selectById(trainInfoDTO.getId());
        if (train == null) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_EXIST.getCode(), "列车不存在");
        }

        BeanUtils.copyProperties(trainInfoDTO, train);

        // 重新计算运行时长
        if (train.getStartTime() != null && train.getEndTime() != null) {
            int duration = (train.getEndTime().getMinute() - train.getStartTime().getMinute());
            train.setDuration(duration);
        }

        train.setUpdateTime(new Date());

        int result = trainMapper.update(train);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "更新列车失败");
        }
    }

    @Override
    @Transactional
    public void deleteTrain(Long id) {
        Train train = trainMapper.selectById(id);
        if (train == null) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_EXIST.getCode(), "列车不存在");
        }
        trainStationMapper.deleteByTrainId(train.getId());
        int result = trainMapper.delete(id);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "删除列车失败");
        }
    }

    @Override
    public Train getTrainById(Long trainId) {

        return trainMapper.selectById(trainId);
    }
}