package com.ticket.system.ai.tools;

import com.ticket.system.dto.request.TrainQueryDTO;
import com.ticket.system.dto.response.TrainInfoDTO;
import com.ticket.system.service.TrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 车次查询工具
 * 供 AI 调用，查询两站之间的车次信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainTools {

    private final TrainService trainService;

    /**
     * 根据起始站和到达站查询车次
     * 可选：按列车类型筛选、按日期筛选
     *
     * @param departureStationId 出发站 ID
     * @param arrivalStationId  到达站 ID
     * @param trainType         列车类型（可选），如 G（高铁）、D（动车）、C（城际）、Z（直达）、T（特快）、K（快速）
     * @param date              日期（可选），格式 YYYY-MM-DD
     * @return 符合条件的车次列表
     */
    @Tool(name = "search_trains", description = "根据出发站和到达站查询所有经过的车次信息。返回车次号、类型、出发时间、到达时间、历时等信息。如果需要查询余票，应优先使用 query_tickets 工具。")
    public List<TrainInfoDTO> searchTrains(
            @ToolParam(description = "出发站 ID（非车站名称，需先通过 get_stations_by_city 获取车站 ID）") Long departureStationId,
            @ToolParam(description = "到达站 ID（非车站名称，需先通过 get_stations_by_city 获取车站 ID）") Long arrivalStationId,
            @ToolParam(description = "列车类型筛选（可选），可选值：G（高铁）、D（动车）、C（城际）、Z（直达）、T（特快）、K（快速）、any（不限）") String trainType,
            @ToolParam(description = "出发日期（可选），格式 YYYY-MM-DD，如 2026-04-20") String date) {
        log.info("[TrainTools] searchTrains called, departure: {}, arrival: {}, type: {}, date: {}",
                departureStationId, arrivalStationId, trainType, date);

        try {
            if (departureStationId == null || arrivalStationId == null) {
                log.warn("[TrainTools] Station ID is required");
                return List.of();
            }

            TrainQueryDTO queryDTO = new TrainQueryDTO();
            queryDTO.setStartStationId(departureStationId);
            queryDTO.setEndStationId(arrivalStationId);

            // 设置日期
            if (date != null && !date.isBlank()) {
                try {
                    queryDTO.setDepartureDate(LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (DateTimeParseException e) {
                    log.warn("[TrainTools] Invalid date format: {}, ignoring", date);
                }
            }

            // 设置车型筛选
            if (trainType != null && !trainType.isBlank() && !"any".equalsIgnoreCase(trainType)) {
                queryDTO.setTrainType(trainType);
            }

            List<TrainInfoDTO> trains = trainService.searchTrains(queryDTO);
            log.info("[TrainTools] Found {} trains", trains.size());
            return trains;

        } catch (Exception e) {
            log.error("[TrainTools] Error searching trains", e);
            return List.of();
        }
    }

    /**
     * 根据车次号查询具体车次信息
     *
     * @param trainNumber 车次号，如 G1234、D2345、K5678
     * @return 车次详细信息
     */
    @Tool(name = "get_train_by_number", description = "根据车次号精确查询车次信息。输入车次号（如G1234），返回该车次的详细信息包括出发站、到达站、出发时间、到达时间、历时等。")
    public TrainInfoDTO getTrainByNumber(
            @ToolParam(description = "车次号，如 G1234、D2345、Z56、K123") String trainNumber) {
        log.info("[TrainTools] getTrainByNumber called, trainNumber: {}", trainNumber);
        try {
            if (trainNumber == null || trainNumber.isBlank()) {
                return null;
            }
            TrainInfoDTO train = trainService.getTrainByNumber(trainNumber);
            log.info("[TrainTools] Found train: {}", train != null ? train.getTrainNumber() : "null");
            return train;
        } catch (Exception e) {
            log.error("[TrainTools] Error getting train by number: {}", trainNumber, e);
            return null;
        }
    }
}
