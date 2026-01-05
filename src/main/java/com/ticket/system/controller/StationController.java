package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.response.StationInfoDTO;
import com.ticket.system.service.StationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/station")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping("/{id}")
    public Result<StationInfoDTO> getStationById(@PathVariable Long id) {
        StationInfoDTO station = stationService.getStationById(id);
        return Result.success(station);
    }

    @GetMapping("/code/{stationCode}")
    public Result<StationInfoDTO> getStationByCode(@PathVariable String stationCode) {
        StationInfoDTO station = stationService.getStationByCode(stationCode);
        return Result.success(station);
    }

    @GetMapping("/name/{stationName}")
    public Result<StationInfoDTO> getStationByName(@PathVariable String stationName) {
        StationInfoDTO station = stationService.getStationByName(stationName);
        return Result.success(station);
    }

    @GetMapping("/list")
    public Result<List<StationInfoDTO>> getAllStations() {
        List<StationInfoDTO> stations = stationService.getAllStations();
        return Result.success(stations);
    }

    @GetMapping("/city/{city}")
    public Result<List<StationInfoDTO>> getStationsByCity(@PathVariable String city) {
        List<StationInfoDTO> stations = stationService.getStationsByCity(city);
        return Result.success(stations);
    }

    @GetMapping("/province/{province}")
    public Result<List<StationInfoDTO>> getStationsByProvince(@PathVariable String province) {
        List<StationInfoDTO> stations = stationService.getStationsByProvince(province);
        return Result.success(stations);
    }

    @GetMapping("/search")
    public Result<List<StationInfoDTO>> searchStations(@RequestParam String keyword) {
        List<StationInfoDTO> stations = stationService.searchStations(keyword);
        return Result.success(stations);
    }

    @PostMapping("/add")
    public Result<String> addStation(@RequestBody StationInfoDTO stationInfoDTO) {
        stationService.addStation(stationInfoDTO);
        return Result.success("添加车站成功");
    }

    @PutMapping("/update")
    public Result<String> updateStation(@RequestBody StationInfoDTO stationInfoDTO) {
        stationService.updateStation(stationInfoDTO);
        return Result.success("更新车站成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.success("删除车站成功");
    }
}