package com.ticket.system.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class SeatInfoDTO {

    private Long id;
    private Long carriageId;
    private Integer carriageIndex;
    private String seatNumber;
    private String seatType;
    private String seatTypeName;
    private Integer rowNum;
    private Integer colNum;
    private Integer status;
    private String statusText;
    private Date createTime;

    // 座位类型中文名称
    public String getSeatTypeName() {
        switch (seatType) {
            case "BUSINESS": return "商务座";
            case "FIRST": return "一等座";
            case "SECOND": return "二等座";
            case "SOFT_SLEEPER": return "软卧";
            case "HARD_SLEEPER": return "硬卧";
            case "SOFT_SEAT": return "软座";
            case "HARD_SEAT": return "硬座";
            default: return "其他";
        }
    }

    // 状态文本
    public String getStatusText() {
        switch (status) {
            case 0: return "不可用";
            case 1: return "可用";
            case 2: return "已售";
            case 3: return "锁定";
            default: return "未知";
        }
    }

    // 检查座位是否可用
    public boolean isAvailable() {
        return status == 1;
    }

    // 获取座位显示名称（如：01A, 02F等）
    public String getDisplayName() {
        return String.format("%02d", rowNum) + getColumnLetter(colNum);
    }

    private String getColumnLetter(int col) {
        if (col <= 0) return "";
        return String.valueOf((char) ('A' + col - 1));
    }
}