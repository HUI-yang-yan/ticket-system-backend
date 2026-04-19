package com.ticket.system.ai.tools;

import com.ticket.system.dto.request.TicketQueryDTO;
import com.ticket.system.dto.response.StationInfoDTO;
import com.ticket.system.dto.response.TicketInfoDTO;
import com.ticket.system.service.StationService;
import com.ticket.system.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 票务查询工具（只读）
 * 供 AI 调用查询余票信息
 * ⚠️ 此工具仅支持查询，不支持下单/购买
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketTools {

    private final TicketService ticketService;
    private final StationService stationService;

    /**
     * 查询余票信息
     * 支持按城市名查询（自动转换）、按座位类型筛选
     *
     * @param departureCity 出发城市名（中文），如"北京"、"上海"
     * @param arrivalCity  到达城市名（中文），如"上海"、"广州"
     * @param date         出发日期，格式 YYYY-MM-DD，如 "2026-04-20"
     * @param seatType     座位类型（可选），如 "二等座"、"一等座"、"硬卧"，null 或 "any" 表示不限
     * @return 符合条件的余票列表
     */
    @Tool(name = "query_tickets", description = "查询火车余票信息。这是核心查询工具，输入出发城市、到达城市、日期、座位类型，返回所有余票列表。包括车次号、座位类型、票价、余票数量、出发时间、到达时间等信息。⚠️ 此工具仅供查询，不能用于购票。")
    public List<TicketInfoDTO> queryTickets(
            @ToolParam(description = "出发城市名称（不带'市'字），如'北京'、'上海'、'广州'、'深圳'、'杭州'") String departureCity,
            @ToolParam(description = "到达城市名称（不带'市'字），如'上海'、'广州'、'深圳'、'杭州'、'成都'") String arrivalCity,
            @ToolParam(description = "出发日期，格式 YYYY-MM-DD，如 2026-04-20。如果输入'今天'、'明天'等自然日期，系统会自动转换。") String date,
            @ToolParam(description = "座位类型筛选（可选），可选值：二等座、一等座、商务座、硬座、硬卧、软卧、无座，null或any表示不限座位类型") String seatType) {
        log.info("[TicketTools] query_tickets called, from: {}, to: {}, date: {}, seatType: {}",
                departureCity, arrivalCity, date, seatType);

        try {
            // 1. 解析城市名为车站
            StationInfoDTO fromStation = resolveMainStation(departureCity);
            StationInfoDTO toStation = resolveMainStation(arrivalCity);

            if (fromStation == null) {
                log.warn("[TicketTools] Cannot resolve departure city: {}", departureCity);
                return List.of();
            }
            if (toStation == null) {
                log.warn("[TicketTools] Cannot resolve arrival city: {}", arrivalCity);
                return List.of();
            }

            // 2. 构建查询参数
            TicketQueryDTO queryDTO = new TicketQueryDTO();
            queryDTO.setDepartureStationId(fromStation.getId());
            queryDTO.setArrivalStationId(toStation.getId());

            // 3. 解析日期
            if (date != null && !date.isBlank()) {
                try {
                    queryDTO.setDepartureDate(LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (DateTimeParseException e) {
                    log.warn("[TicketTools] Invalid date format: {}, trying to parse", date);
                    // 尝试处理中文日期
                    date = parseChineseDate(date);
                    try {
                        queryDTO.setDepartureDate(LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE));
                    } catch (Exception ex) {
                        log.warn("[TicketTools] Cannot parse date: {}, ignoring", date);
                    }
                }
            }

            // 4. 座位类型筛选
            if (seatType != null && !seatType.isBlank() && !"any".equals(seatType)) {
                queryDTO.setSeatType(seatType);
            }

            // 5. 执行查询
            List<TicketInfoDTO> tickets = ticketService.queryTickets(queryDTO);
            log.info("[TicketTools] Found {} tickets", tickets.size());
            return tickets;

        } catch (Exception e) {
            log.error("[TicketTools] Error querying tickets", e);
            return List.of();
        }
    }

    /**
     * 解析城市名称为主车站
     * 策略：优先选择不带后缀的主站
     */
    private StationInfoDTO resolveMainStation(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return null;
        }

        try {
            // 去掉可能的"市"后缀
            String city = cityName.replace("市", "").trim();

            List<StationInfoDTO> stations = stationService.getStationsByCity(city);
            if (stations == null || stations.isEmpty()) {
                // 尝试模糊搜索
                stations = stationService.searchStations(city);
            }

            if (stations == null || stations.isEmpty()) {
                return null;
            }

            // 优先选择城市名完全匹配的站点
            for (StationInfoDTO station : stations) {
                if (station.getCity() != null &&
                        station.getCity().replace("市", "").equals(city.replace("市", ""))) {
                    // 如果有精确匹配市名的，返回
                    if (station.getStationName().equals(city) ||
                            station.getStationName().equals(city + "站")) {
                        return station;
                    }
                }
            }

            // 否则选择最短名称的站点（主站通常名称最短）
            return stations.stream()
                    .min(Comparator.comparingInt(s -> s.getStationName().length()))
                    .orElse(stations.get(0));

        } catch (Exception e) {
            log.error("[TicketTools] Error resolving main station for city: {}", cityName, e);
            return null;
        }
    }

    /**
     * 解析中文日期表达
     * 简单实现，支持"今天"、"明天"、"后天"
     */
    private String parseChineseDate(String dateStr) {
        if (dateStr == null) return null;
        LocalDate today = LocalDate.now();

        return switch (dateStr.trim()) {
            case "今天" -> today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "明天" -> today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "后天" -> today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
            default -> dateStr; // 尝试其他格式解析
        };
    }

    /**
     * 获取票详情
     * 根据票ID查询详细信息
     */
    @Tool(name = "get_ticket_detail", description = "根据票ID获取某张票的详细信息，包括座位类型、票价、余票数、出发到达时间等。输入票ID（ticketId），返回该票的完整信息。")
    public TicketInfoDTO getTicketDetail(
            @ToolParam(description = "票ID，从 query_tickets 返回的票列表中获取") Long ticketId) {
        log.info("[TicketTools] getTicketDetail called, ticketId: {}", ticketId);
        try {
            if (ticketId == null) {
                return null;
            }
            TicketInfoDTO ticket = ticketService.getTicketDetail(ticketId);
            return ticket;
        } catch (Exception e) {
            log.error("[TicketTools] Error getting ticket detail: {}", ticketId, e);
            return null;
        }
    }
}
