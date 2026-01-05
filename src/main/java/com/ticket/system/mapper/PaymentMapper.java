package com.ticket.system.mapper;

import com.ticket.system.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMapper {
    int insert(Payment payment);
    int update(Payment payment);
    int delete(Long id);
    Payment selectById(Long id);
    Payment selectByPaymentNumber(String paymentNumber);
    Payment selectByOrderId(Long orderId);
    List<Payment> selectByUserId(Long userId);
    int updatePaymentStatus(@Param("id") Long id, @Param("paymentStatus") Integer paymentStatus);
}