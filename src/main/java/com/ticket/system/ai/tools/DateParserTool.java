package com.ticket.system.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期解析工具
 * 将自然语言日期转换为标准格式 YYYY-MM-DD
 *
 * 支持的表达：
 * - 今天、明天、后天、大后天
 * - 本周一 ~ 本周日、这个周一 ~ 这个周日、下周一 ~ 下周日
 * - 4月20日、四月廿、4/20
 * - 2026-04-20、2026/4/20
 */
@Slf4j
@Component
public class DateParserTool {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 将自然语言日期转换为标准格式 YYYY-MM-DD
     *
     * @param dateText 自然语言日期，如"今天"、"明天"、"下周三"、"4月20日"
     * @return YYYY-MM-DD 格式日期，解析失败返回 null
     */
    @Tool(name = "parse_date", description = "将自然语言日期转换为标准格式YYYY-MM-DD。当用户提到'明天'、'后天'、'下周三'、'4月20日'等相对日期或模糊日期时，必须先调用此工具将其转换为具体日期后再查询票务。")
    public String parseDate(
            @ToolParam(description = "自然语言日期，如'今天'、'明天'、'后天'、'下周三'、'4月20日'、'2026-04-20'等") String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }

        String input = dateText.trim();
        log.info("[DateParserTool] Parsing: {}", input);

        String result = parse(input);
        log.info("[DateParserTool] Result: {}", result);
        return result;
    }

    private String parse(String input) {
        LocalDate today = LocalDate.now();

        // 1. 绝对相对日期：今天、明天、后天、大后天
        String result = parseRelativeDays(input, today);
        if (result != null) return result;

        // 2. 星期相关：下周一、本周二、这个周五等
        result = parseWeekdayReference(input, today);
        if (result != null) return result;

        // 3. 月份日期：4月20日、四月廿、4/20
        result = parseMonthDay(input, today);
        if (result != null) return result;

        // 4. 标准格式：2026-04-20、2026/4/20
        result = parseStandardFormat(input);
        if (result != null) return result;

        // 5. dateutil 风格的模糊解析（仅支持"Apr 20, 2026"等英文格式）
        return parseEnglishDate(input, today);
    }

    /**
     * 解析绝对相对日期：今天、明天、后天、大后天
     */
    private String parseRelativeDays(String input, LocalDate today) {
        return switch (input) {
            case "今天", "今日" -> today.format(ISO_FORMATTER);
            case "明天", "明日" -> today.plusDays(1).format(ISO_FORMATTER);
            case "后天", "后日" -> today.plusDays(2).format(ISO_FORMATTER);
            case "大后天", "大后日" -> today.plusDays(3).format(ISO_FORMATTER);
            default -> null;
        };
    }

    /**
     * 解析星期相关日期：下周一、本周二、这个周五等
     */
    private String parseWeekdayReference(String input, LocalDate today) {
        // 下周一、下周二 ... 下周日
        Pattern nextWeekPattern = Pattern.compile("下周([一二三四五六日])");
        Matcher nextWeekMatcher = nextWeekPattern.matcher(input);
        if (nextWeekMatcher.matches()) {
            return getNextWeekday(nextWeekMatcher.group(1), today, 7);
        }

        // 本周一、本周二 ... 本周日
        Pattern thisWeekPattern = Pattern.compile("本周([一二三四五六日])");
        Matcher thisWeekMatcher = thisWeekPattern.matcher(input);
        if (thisWeekMatcher.matches()) {
            return getThisWeekday(thisWeekMatcher.group(1), today);
        }

        // 这个周一、这个周二 ... 这个周日
        Pattern thatWeekPattern = Pattern.compile("这个([一二三四五六日])");
        Matcher thatWeekMatcher = thatWeekPattern.matcher(input);
        if (thatWeekMatcher.matches()) {
            return getThisWeekday(thatWeekMatcher.group(1), today);
        }

        // 下下周一的格式匹配
        Pattern doubleNextPattern = Pattern.compile("下下周([一二三四五六日])");
        Matcher doubleNextMatcher = doubleNextPattern.matcher(input);
        if (doubleNextMatcher.matches()) {
            return getNextWeekday(doubleNextMatcher.group(1), today, 14);
        }

        return null;
    }

    /**
     * 获取本周指定星期几的日期
     */
    private String getThisWeekday(String dayName, LocalDate today) {
        int targetDay = getWeekdayIndex(dayName);
        int currentDay = today.getDayOfWeek().getValue(); // 1=周一, 7=周日
        int diff = targetDay - currentDay;
        if (diff < 0) diff += 7; // 已经过了，向后推到下周
        return today.plusDays(diff).format(ISO_FORMATTER);
    }

    /**
     * 获取下周指定星期几的日期
     */
    private String getNextWeekday(String dayName, LocalDate today, int daysAhead) {
        int targetDay = getWeekdayIndex(dayName);
        int currentDay = today.getDayOfWeek().getValue();
        int diff = (7 - currentDay) + targetDay + daysAhead - 7;
        if (diff <= 7) diff += 7; // 确保是下周
        return today.plusDays(diff).format(ISO_FORMATTER);
    }

    private int getWeekdayIndex(String dayName) {
        return switch (dayName) {
            case "一" -> 1;
            case "二" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "日" -> 7;
            default -> -1;
        };
    }

    /**
     * 解析月份日期：4月20日、四月廿、4/20
     */
    private String parseMonthDay(String input, LocalDate today) {
        // 4月20日、四月廿
        Pattern chinesePattern = Pattern.compile("(\\d{1,2})月(\\d{1,2})日?");
        Matcher chineseMatcher = chinesePattern.matcher(input);
        if (chineseMatcher.matches()) {
            int month = Integer.parseInt(chineseMatcher.group(1));
            int day = Integer.parseInt(chineseMatcher.group(2));
            return tryCreateDate(today.getYear(), month, day);
        }

        // 廿 = 20，如四月廿 = 4月20日
        Pattern rihuPattern = Pattern.compile("(\\d{1,2})月(\\d{1,2})廿");
        Matcher rihuMatcher = rihuPattern.matcher(input);
        if (rihuMatcher.matches()) {
            int month = Integer.parseInt(rihuMatcher.group(1));
            int day = 20 + Integer.parseInt(rihuMatcher.group(2));
            return tryCreateDate(today.getYear(), month, day);
        }

        // 4/20 或 04/20
        Pattern slashPattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})");
        Matcher slashMatcher = slashPattern.matcher(input);
        if (slashMatcher.matches()) {
            int month = Integer.parseInt(slashMatcher.group(1));
            int day = Integer.parseInt(slashMatcher.group(2));
            return tryCreateDate(today.getYear(), month, day);
        }

        return null;
    }

    /**
     * 尝试创建日期，处理无效日期（如2月30日）
     */
    private String tryCreateDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day).format(ISO_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析标准格式：2026-04-20、2026/4/20
     */
    private String parseStandardFormat(String input) {
        // yyyy-MM-dd
        Pattern isoPattern = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
        Matcher isoMatcher = isoPattern.matcher(input);
        if (isoMatcher.matches()) {
            int year = Integer.parseInt(isoMatcher.group(1));
            int month = Integer.parseInt(isoMatcher.group(2));
            int day = Integer.parseInt(isoMatcher.group(3));
            return tryCreateDate(year, month, day);
        }

        // yyyy/MM/dd
        Pattern slashPattern = Pattern.compile("(\\d{4})/(\\d{1,2})/(\\d{1,2})");
        Matcher slashMatcher = slashPattern.matcher(input);
        if (slashMatcher.matches()) {
            int year = Integer.parseInt(slashMatcher.group(1));
            int month = Integer.parseInt(slashMatcher.group(2));
            int day = Integer.parseInt(slashMatcher.group(3));
            return tryCreateDate(year, month, day);
        }

        return null;
    }

    /**
     * 解析英文日期：April 20, 2026 或 Apr 20
     */
    private String parseEnglishDate(String input, LocalDate today) {
        String[] engMonths = {"january", "february", "march", "april", "may", "june",
                              "july", "august", "september", "october", "november", "december"};
        String[] engMonthsShort = {"jan", "feb", "mar", "apr", "may", "jun",
                                   "jul", "aug", "sep", "oct", "nov", "dec"};

        String lower = input.toLowerCase();
        for (int i = 0; i < engMonths.length; i++) {
            Pattern p = Pattern.compile(engMonths[i] + "\\s+(\\d{1,2})(?:,?\\s+(\\d{4}))?");
            Matcher m = p.matcher(lower);
            if (m.find()) {
                int month = i + 1;
                int day = Integer.parseInt(m.group(1));
                int year = m.group(2) != null ? Integer.parseInt(m.group(2)) : today.getYear();
                return tryCreateDate(year, month, day);
            }

            Pattern p2 = Pattern.compile(engMonthsShort[i] + "\\s+(\\d{1,2})(?:,?\\s+(\\d{4}))?");
            Matcher m2 = p2.matcher(lower);
            if (m2.find()) {
                int month = i + 1;
                int day = Integer.parseInt(m2.group(1));
                int year = m2.group(2) != null ? Integer.parseInt(m2.group(2)) : today.getYear();
                return tryCreateDate(year, month, day);
            }
        }

        return null;
    }
}
