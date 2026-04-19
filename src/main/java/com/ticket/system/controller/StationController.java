package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.response.StationInfoDTO;
import com.ticket.system.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/station")
@Tag(name = "车站管理", description = "车站信息查询、添加、修改、删除")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询车站", description = "通过车站ID获取车站详细信息")
    public Result<StationInfoDTO> getStationById(@PathVariable Long id) {
        StationInfoDTO station = stationService.getStationById(id);
        return Result.success(station);
    }

    @GetMapping("/code/{stationCode}")
    @Operation(summary = "根据车站代码查询", description = "通过车站代码（如G1234）查询车站信息")
    public Result<StationInfoDTO> getStationByCode(@PathVariable String stationCode) {
        StationInfoDTO station = stationService.getStationByCode(stationCode);
        return Result.success(station);
    }

    @GetMapping("/name/{stationName}")
    @Operation(summary = "根据车站名称查询", description = "通过车站名称精确查询车站信息")
    public Result<StationInfoDTO> getStationByName(@PathVariable String stationName) {
        StationInfoDTO station = stationService.getStationByName(stationName);
        return Result.success(station);
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有车站列表", description = "查询系统中所有车站信息")
    public Result<List<StationInfoDTO>> getAllStations() {
        List<StationInfoDTO> stations = stationService.getAllStations();
        return Result.success(stations);
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "根据城市查询车站", description = "查询指定城市下的所有车站")
    public Result<List<StationInfoDTO>> getStationsByCity(@PathVariable String city) {
        List<StationInfoDTO> stations = stationService.getStationsByCity(city);
        return Result.success(stations);
    }

    @GetMapping("/province/{province}")
    @Operation(summary = "根据省份查询车站", description = "查询指定省份下的所有车站")
    public Result<List<StationInfoDTO>> getStationsByProvince(@PathVariable String province) {
        List<StationInfoDTO> stations = stationService.getStationsByProvince(province);
        return Result.success(stations);
    }

    @GetMapping("/search")
    @Operation(summary = "关键字搜索车站", description = "根据关键字模糊搜索车站名称或代码")
    public Result<List<StationInfoDTO>> searchStations(@RequestParam String keyword) {
        List<StationInfoDTO> stations = stationService.searchStations(keyword);
        return Result.success(stations);
    }

    @PostMapping("/add")
    @Operation(summary = "添加车站", description = "新增车站信息到系统中")
    public Result<String> addStation(@RequestBody StationInfoDTO stationInfoDTO) {
        stationService.addStation(stationInfoDTO);
        return Result.success("添加车站成功");
    }

    @PutMapping("/update")
    @Operation(summary = "更新车站信息", description = "修改车站详细信息")
    public Result<String> updateStation(@RequestBody StationInfoDTO stationInfoDTO) {
        stationService.updateStation(stationInfoDTO);
        return Result.success("更新车站成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除车站", description = "根据ID删除车站信息")
    public Result<String> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.success("删除车站成功");
    }
}