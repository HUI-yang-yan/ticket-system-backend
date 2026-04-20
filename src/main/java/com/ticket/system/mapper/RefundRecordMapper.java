package com.ticket.system.mapper;

import com.ticket.system.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RefundRecordMapper {

    int insert(RefundRecord refundRecord);

    int update(RefundRecord refundRecord);

    RefundRecord selectById(Long id);

    RefundRecord selectByRefundNumber(@Param("refundNumber") String refundNumber);

    RefundRecord selectByOrderNumber(@Param("orderNumber") String orderNumber);

    List<RefundRecord> selectByUserId(@Param("userId") Long userId);

    List<RefundRecord> selectByStatus(@Param("status") String status);
}
