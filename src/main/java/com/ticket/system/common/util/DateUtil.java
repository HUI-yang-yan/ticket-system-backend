package com.ticket.system.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
public class DateUtil {

    // 常用日期格式
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_FORMAT_CN = "yyyy年MM月dd日";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String TIME_FORMAT_SHORT = "HH:mm";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_FORMAT_CN = "yyyy年MM月dd日 HH时mm分ss秒";
    public static final String DATETIME_FORMAT_COMPACT = "yyyyMMddHHmmss";
    public static final String DATE_FORMAT_COMPACT = "yyyyMMdd";

    // 线程安全的格式化器
    private static final ThreadLocal<Map<String, SimpleDateFormat>> dateFormatThreadLocal =
            ThreadLocal.withInitial(HashMap::new);

    private static SimpleDateFormat getDateFormat(String pattern) {
        Map<String, SimpleDateFormat> formatterMap = dateFormatThreadLocal.get();
        SimpleDateFormat sdf = formatterMap.get(pattern);
        if (sdf == null) {
            sdf = new SimpleDateFormat(pattern);
            formatterMap.put(pattern, sdf);
        }
        return sdf;
    }

    // ==================== 日期格式化 ====================

    /**
     * 格式化日期为字符串
     */
    public static String formatDate(Date date) {
        return formatDate(date, DATE_FORMAT);
    }

    /**
     * 格式化日期为指定格式字符串
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        return getDateFormat(pattern).format(date);
    }

    /**
     * 格式化当前日期
     */
    public static String formatCurrentDate(String pattern) {
        return formatDate(new Date(), pattern);
    }

    /**
     * 格式化时间为字符串
     */
    public static String formatTime(Date date) {
        return formatDate(date, TIME_FORMAT);
    }

    /**
     * 格式化日期时间为字符串
     */
    public static String formatDateTime(Date date) {
        return formatDate(date, DATETIME_FORMAT);
    }

    // ==================== 日期解析 ====================

    /**
     * 解析字符串为日期
     */
    public static Date parseDate(String dateStr) {
        return parseDate(dateStr, DATE_FORMAT);
    }

    /**
     * 解析字符串为指定格式日期
     */
    public static Date parseDate(String dateStr, String pattern) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return getDateFormat(pattern).parse(dateStr.trim());
        } catch (ParseException e) {
            log.error("日期解析失败: {}, pattern: {}", dateStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析字符串为日期时间
     */
    public static Date parseDateTime(String dateTimeStr) {
        return parseDate(dateTimeStr, DATETIME_FORMAT);
    }

    /**
     * 安全解析日期，解析失败返回默认值
     */
    public static Date parseDateSafe(String dateStr, Date defaultValue) {
        Date result = parseDate(dateStr);
        return result != null ? result : defaultValue;
    }

    // ==================== 日期计算 ====================

    /**
     * 增加天数
     */
    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    /**
     * 增加小时
     */
    public static Date addHours(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    /**
     * 增加分钟
     */
    public static Date addMinutes(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    /**
     * 增加秒数
     */
    public static Date addSeconds(Date date, int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    /**
     * 减少天数
     */
    public static Date minusDays(Date date, int days) {
        return addDays(date, -days);
    }

    /**
     * 减少小时
     */
    public static Date minusHours(Date date, int hours) {
        return addHours(date, -hours);
    }

    /**
     * 减少分钟
     */
    public static Date minusMinutes(Date date, int minutes) {
        return addMinutes(date, -minutes);
    }

    // ==================== 日期比较 ====================

    /**
     * 获取两个日期的天数差
     */
    public static int daysBetween(Date startDate, Date endDate) {
        LocalDate start = convertToLocalDate(startDate);
        LocalDate end = convertToLocalDate(endDate);
        return (int) ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 获取两个日期的小时差
     */
    public static long hoursBetween(Date startDate, Date endDate) {
        Duration duration = Duration.between(
                convertToLocalDateTime(startDate),
                convertToLocalDateTime(endDate)
        );
        return duration.toHours();
    }

    /**
     * 获取两个日期的分钟差
     */
    public static long minutesBetween(Date startDate, Date endDate) {
        Duration duration = Duration.between(
                convertToLocalDateTime(startDate),
                convertToLocalDateTime(endDate)
        );
        return duration.toMinutes();
    }

    /**
     * 比较两个日期（忽略时间）
     */
    public static int compareDate(Date date1, Date date2) {
        LocalDate localDate1 = convertToLocalDate(date1);
        LocalDate localDate2 = convertToLocalDate(date2);
        return localDate1.compareTo(localDate2);
    }

    /**
     * 检查日期是否在范围内
     */
    public static boolean isDateInRange(Date date, Date startDate, Date endDate) {
        return !date.before(startDate) && !date.after(endDate);
    }

    /**
     * 检查是否过期
     */
    public static boolean isExpired(Date date) {
        return date.before(new Date());
    }

    // ==================== 日期获取 ====================

    /**
     * 获取当天的开始时间（00:00:00）
     */
    public static Date beginOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获取当天的结束时间（23:59:59）
     */
    public static Date endOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /**
     * 获取本周的开始时间（周一）
     */
    public static Date beginOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获取本月的开始时间
     */
    public static Date beginOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获取年份
     */
    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 获取月份（1-12）
     */
    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取日期（1-31）
     */
    public static int getDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取小时（0-23）
     */
    public static int getHour(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取分钟（0-59）
     */
    public static int getMinute(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }

    // ==================== 日期转换 ====================

    /**
     * Date 转 LocalDate
     */
    public static LocalDate convertToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Date 转 LocalDateTime
     */
    public static LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * LocalDate 转 Date
     */
    public static Date convertToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    /**
     * LocalDateTime 转 Date
     */
    public static Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    /**
     * 时间戳转Date
     */
    public static Date timestampToDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new Date(timestamp);
    }

    // ==================== 星期相关 ====================

    /**
     * 获取星期几（1-7，1表示星期一）
     */
    public static int getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Calendar中1表示星期日，2表示星期一...7表示星期六
        return dayOfWeek == Calendar.SUNDAY ? 7 : dayOfWeek - 1;
    }

    /**
     * 获取星期几中文名称
     */
    public static String getDayOfWeekCN(Date date) {
        int dayOfWeek = getDayOfWeek(date);
        switch (dayOfWeek) {
            case 1: return "星期一";
            case 2: return "星期二";
            case 3: return "星期三";
            case 4: return "星期四";
            case 5: return "星期五";
            case 6: return "星期六";
            case 7: return "星期日";
            default: return "";
        }
    }

    /**
     * 判断是否是工作日（周一到周五）
     */
    public static boolean isWeekday(Date date) {
        int dayOfWeek = getDayOfWeek(date);
        return dayOfWeek >= 1 && dayOfWeek <= 5;
    }

    /**
     * 判断是否是周末（周六到周日）
     */
    public static boolean isWeekend(Date date) {
        int dayOfWeek = getDayOfWeek(date);
        return dayOfWeek == 6 || dayOfWeek == 7;
    }

    // ==================== 车票系统专用方法 ====================

    /**
     * 检查是否在预售期内
     * @param departureDate 出发日期
     * @param preSaleDays 预售期天数
     * @return 是否可购买
     */
    public static boolean isInPreSalePeriod(Date departureDate, int preSaleDays) {
        Date today = beginOfDay(new Date());
        Date maxPurchaseDate = addDays(today, preSaleDays);
        return !departureDate.before(today) && !departureDate.after(maxPurchaseDate);
    }

    /**
     * 获取预售期内的日期列表
     * @param preSaleDays 预售期天数
     * @return 日期列表
     */
    public static List<Date> getPreSaleDateList(int preSaleDays) {
        List<Date> dateList = new ArrayList<>();
        Date today = beginOfDay(new Date());

        for (int i = 0; i <= preSaleDays; i++) {
            dateList.add(addDays(today, i));
        }

        return dateList;
    }

    /**
     * 获取出发日期范围文本
     */
    public static String getDateRangeText(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("M月d日");
        if (getYear(startDate) == getYear(endDate)) {
            if (getMonth(startDate) == getMonth(endDate)) {
                // 同月
                return getMonth(startDate) + "月" + getDay(startDate) + "-" + getDay(endDate) + "日";
            } else {
                // 不同月但同年
                return sdf.format(startDate) + "-" + sdf.format(endDate);
            }
        } else {
            // 不同年
            SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy年M月d日");
            return fullSdf.format(startDate) + "-" + fullSdf.format(endDate);
        }
    }

    /**
     * 计算运行时长（分钟转时分格式）
     */
    public static String formatDurationMinutes(int minutes) {
        if (minutes <= 0) {
            return "0分钟";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (remainingMinutes > 0) {
            sb.append(remainingMinutes).append("分钟");
        }

        return sb.toString();
    }

    /**
     * 格式化车次出发到达时间
     */
    public static String formatTrainSchedule(Date departureTime, Date arrivalTime) {
        if (departureTime == null || arrivalTime == null) {
            return "";
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT_SHORT);
        long durationMinutes = minutesBetween(departureTime, arrivalTime);

        return String.format("%s - %s （%s）",
                timeFormat.format(departureTime),
                timeFormat.format(arrivalTime),
                formatDurationMinutes((int) durationMinutes));
    }

    /**
     * 获取订单过期时间（默认30分钟后）
     */
    public static Date getOrderExpireTime() {
        return addMinutes(new Date(), 30);
    }

    /**
     * 计算剩余支付时间（秒）
     */
    public static long getRemainingPaySeconds(Date expireTime) {
        if (expireTime == null) {
            return 0;
        }

        long remaining = expireTime.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    /**
     * 格式化剩余支付时间（时:分:秒）
     */
    public static String formatRemainingPayTime(Date expireTime) {
        long seconds = getRemainingPaySeconds(expireTime);
        if (seconds <= 0) {
            return "已过期";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    // ==================== 节假日判断（简化版） ====================

    /**
     * 判断是否是节假日（需要维护节假日数据）
     */
    public static boolean isHoliday(Date date) {
        // 这里应该查询数据库或配置文件中的节假日数据
        // 暂时返回false
        return false;
    }

    /**
     * 判断是否是特殊日期（春运、国庆等）
     */
    public static boolean isSpecialPeriod(Date date) {
        int month = getMonth(date);
        int day = getDay(date);

        // 春运期间（1月-2月）
        if (month == 1 || month == 2) {
            return true;
        }

        // 国庆期间（10月1日-10月7日）
        if (month == 10 && day >= 1 && day <= 7) {
            return true;
        }

        // 五一期间（5月1日-5月3日）
        if (month == 5 && day >= 1 && day <= 3) {
            return true;
        }

        return false;
    }

    // ==================== 时间验证 ====================

    /**
     * 验证日期字符串格式
     */
    public static boolean isValidDate(String dateStr, String pattern) {
        if (!StringUtils.hasText(dateStr)) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false); // 严格模式
            sdf.parse(dateStr.trim());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 验证时间范围（如8:00-22:00）
     */
    public static boolean isValidTimeRange(Date time, String startTimeStr, String endTimeStr) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
            Date startTime = timeFormat.parse(startTimeStr + ":00");
            Date endTime = timeFormat.parse(endTimeStr + ":00");

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time);
            calendar.set(Calendar.YEAR, 1970);
            calendar.set(Calendar.MONTH, 0);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date checkTime = calendar.getTime();

            return !checkTime.before(startTime) && !checkTime.after(endTime);
        } catch (ParseException e) {
            log.error("时间范围验证失败", e);
            return false;
        }
    }

    // ==================== 其他工具方法 ====================

    /**
     * 获取当前时间戳（秒）
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public static long getCurrentTimestampMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取指定日期的Unix时间戳（秒）
     */
    public static long getUnixTimestamp(Date date) {
        return date.getTime() / 1000;
    }

    /**
     * 清除ThreadLocal，防止内存泄漏
     */
    public static void clearThreadLocal() {
        dateFormatThreadLocal.remove();
    }
}