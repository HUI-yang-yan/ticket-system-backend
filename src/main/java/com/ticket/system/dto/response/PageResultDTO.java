package com.ticket.system.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class PageResultDTO<T> {

    private Integer pageNum;        // 当前页码
    private Integer pageSize;       // 每页条数
    private Integer totalPages;     // 总页数
    private Long totalRecords;      // 总记录数
    private List<T> records;        // 数据列表

    // 是否有上一页
    public boolean hasPrevious() {
        return pageNum > 1;
    }

    // 是否有下一页
    public boolean hasNext() {
        return pageNum < totalPages;
    }

    // 计算总页数
    public Integer getTotalPages() {
        if (totalRecords == null || pageSize == null || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    // 创建分页结果
    public static <T> PageResultDTO<T> of(List<T> records, Integer pageNum, Integer pageSize, Long totalRecords) {
        PageResultDTO<T> result = new PageResultDTO<>();
        result.setRecords(records);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalRecords(totalRecords);
        result.setTotalPages(result.getTotalPages());
        return result;
    }
}