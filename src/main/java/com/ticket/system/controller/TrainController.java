package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.TrainQueryDTO;
import com.ticket.system.dto.response.TrainInfoDTO;
import com.ticket.system.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/train")
@Tag(name = "列车管理", description = "列车信息查询、添加、修改、删除")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询列车", description = "通过列车ID获取列车详细信息")
    public Result<TrainInfoDTO> getTrainById(@PathVariable Long id) {
        TrainInfoDTO train = trainService.getTrainInfoById(id);
        return Result.success(train);
    }

    @GetMapping("/number/{trainNumber}")
    @Operation(summary = "根据车次号查询", description = "通过车次号（如G1234）查询列车信息")
    public Result<TrainInfoDTO> getTrainByNumber(@PathVariable String trainNumber) {
        TrainInfoDTO train = trainService.getTrainByNumber(trainNumber);
        return Result.success(train);
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有列车", description = "查询系统中所有列车信息")
    public Result<List<TrainInfoDTO>> getAllTrains() {
        List<TrainInfoDTO> trains = trainService.getAllTrains();
        return Result.success(trains);
    }

    @GetMapping("/type/{trainType}")
    @Operation(summary = "根据列车类型查询", description = "根据列车类型（高铁、动车、特快等）筛选列车")
    public Result<List<TrainInfoDTO>> getTrainsByType(@PathVariable String trainType) {
        List<TrainInfoDTO> trains = trainService.getTrainsByType(trainType);
        return Result.success(trains);
    }

    @PostMapping("/search")
    @Operation(summary = "条件搜索列车", description = "根据出发站、到达站、日期等条件搜索列车")
    public Result<List<TrainInfoDTO>> searchTrains(@RequestBody TrainQueryDTO queryDTO) {
        List<TrainInfoDTO> trains = trainService.searchTrains(queryDTO);
        return Result.success(trains);
    }

    @GetMapping("/route")
    @Operation(summary = "根据路线查询列车", description = "根据出发站ID和到达站ID查询经过该路线的所有列车")
    public Result<List<TrainInfoDTO>> getTrainsByRoute(
            @RequestParam Long startStationId,
            @RequestParam Long endStationId) {
        List<TrainInfoDTO> trains = trainService.getTrainsByRoute(startStationId, endStationId);
        return Result.success(trains);
    }

    @PostMapping("/save")
    @Operation(summary = "添加列车", description = "新增列车信息到系统中")
    public Result<String> addTrain(@RequestBody TrainInfoDTO trainInfoDTO) {
        trainService.addTrain(trainInfoDTO);
        return Result.success("添加列车成功");
    }

    @PostMapping("/update")
    @Operation(summary = "更新列车信息", description = "修改列车详细信息")
    public Result<String> updateTrain(@RequestBody TrainInfoDTO trainInfoDTO) {
        trainService.updateTrain(trainInfoDTO);
        return Result.success("更新列车成功");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除列车", description = "根据ID删除列车信息")
    public Result<String> deleteTrain(@PathVariable Long id) {
        trainService.deleteTrain(id);
        return Result.success("删除列车成功");
    }
}