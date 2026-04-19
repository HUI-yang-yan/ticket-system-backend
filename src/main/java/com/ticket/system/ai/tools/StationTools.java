package com.ticket.system.ai.tools;

import com.ticket.system.dto.response.StationInfoDTO;
import com.ticket.system.service.StationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 车站查询工具
 * 供 AI 调用，将城市名转换为车站列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationTools {

    private final StationService stationService;

    /**
     * 根据城市名获取车站列表
     * AI 需要查询票信息时，先调用此工具确定车站 ID
     *
     * @param city 城市名称（中文），如"北京"、"上海"、"广州"
     * @return 该城市下的所有车站列表
     */
    @Tool(name = "get_stations_by_city", description = "根据城市名称获取该城市所有的火车站站点列表。用户查询票时需要先确定具体车站。输入城市名，返回该城市的所有火车站列表（可能包含多个站，如北京站、北京南站、北京西站等）。")
    public List<StationInfoDTO> getStationsByCity(
            @ToolParam(description = "城市名称（不带'市'字），如'北京'、'上海'、'广州'、'深圳'、'杭州'、'成都'") String city) {
        log.info("[StationTools] getStationsByCity called, city: {}", city);
        try {
            if (city == null || city.isBlank()) {
                return List.of();
            }
            // 去掉可能的"市"后缀
            String cityName = city.replace("市", "").trim();
            List<StationInfoDTO> stations = stationService.getStationsByCity(cityName);
            log.info("[StationTools] Found {} stations for city: {}", stations.size(), cityName);
            return stations;
        } catch (Exception e) {
            log.error("[StationTools] Error getting stations by city: {}", city, e);
            return List.of();
        }
    }

    /**
     * 搜索车站
     * 支持模糊搜索，用于城市名不确定的情况
     *
     * @param keyword 搜索关键字
     * @return 匹配的车站列表
     */
    @Tool(name = "search_stations", description = "通过关键词模糊搜索火车站。可以用于城市名不明确或需要找最近车站的情况。输入关键词，返回匹配的车站列表。")
    public List<StationInfoDTO> searchStations(
            @ToolParam(description = "搜索关键字，可以是城市名、车站名或车站拼音") String keyword) {
        log.info("[StationTools] searchStations called, keyword: {}", keyword);
        try {
            if (keyword == null || keyword.isBlank()) {
                return List.of();
            }
            List<StationInfoDTO> stations = stationService.searchStations(keyword);
            log.info("[StationTools] Found {} stations for keyword: {}", stations.size(), keyword);
            return stations;
        } catch (Exception e) {
            log.error("[StationTools] Error searching stations: {}", keyword, e);
            return List.of();
        }
    }
}
