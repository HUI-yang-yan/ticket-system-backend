package com.ticket.system.mapper;

import com.ticket.system.entity.OrderSeat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderSeatMapper {
    int insert(OrderSeat orderSeat);
    int delete(Long id);
    OrderSeat selectById(Long id);
    List<OrderSeat> selectByOrderId(Long orderId);
    List<OrderSeat> selectBySeatId(Long seatId);
    int deleteByOrderId(Long orderId);
    OrderSeat selectByOrderAndSeat(@Param("orderId") Long orderId,
                                   @Param("seatId") Long seatId);
}