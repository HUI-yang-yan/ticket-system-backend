package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.TrainQueryDTO;
import com.ticket.system.dto.response.TrainInfoDTO;
import com.ticket.system.service.TrainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/train")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @GetMapping("/{id}")
    public Result<TrainInfoDTO> getTrainById(@PathVariable Long id) {
        TrainInfoDTO train = trainService.getTrainInfoById(id);
        return Result.success(train);
    }

    @GetMapping("/number/{trainNumber}")
    public Result<TrainInfoDTO> getTrainByNumber(@PathVariable String trainNumber) {
        TrainInfoDTO train = trainService.getTrainByNumber(trainNumber);
        return Result.success(train);
    }

    @GetMapping("/list")
    public Result<List<TrainInfoDTO>> getAllTrains() {
        List<TrainInfoDTO> trains = trainService.getAllTrains();
        return Result.success(trains);
    }

    @GetMapping("/type/{trainType}")
    public Result<List<TrainInfoDTO>> getTrainsByType(@PathVariable String trainType) {
        List<TrainInfoDTO> trains = trainService.getTrainsByType(trainType);
        return Result.success(trains);
    }

    @PostMapping("/search")
    public Result<List<TrainInfoDTO>> searchTrains(@RequestBody TrainQueryDTO queryDTO) {
        List<TrainInfoDTO> trains = trainService.searchTrains(queryDTO);
        return Result.success(trains);
    }

    @GetMapping("/route")
    public Result<List<TrainInfoDTO>> getTrainsByRoute(
            @RequestParam Long startStationId,
            @RequestParam Long endStationId) {
        List<TrainInfoDTO> trains = trainService.getTrainsByRoute(startStationId, endStationId);
        return Result.success(trains);
    }

    @PostMapping("/save")
    public Result<String> addTrain(@RequestBody TrainInfoDTO trainInfoDTO) {
        trainService.addTrain(trainInfoDTO);
        return Result.success("添加列车成功");
    }

    @PostMapping("/update")
    public Result<String> updateTrain(@RequestBody TrainInfoDTO trainInfoDTO) {
        trainService.updateTrain(trainInfoDTO);
        return Result.success("更新列车成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteTrain(@PathVariable Long id) {
        trainService.deleteTrain(id);
        return Result.success("删除列车成功");
    }


}