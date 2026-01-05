package com.ticket.system.mapper;

import com.ticket.system.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    int insert(Order order);
    int update(Order order);
    int delete(Long id);
    Order selectById(Long id);
    Order selectByOrderNumber(String orderNumber);
    List<Order> selectByUserId(Long userId);
    List<Order> selectByTrainAndDate(@Param("trainId") Long trainId,
                                     @Param("departureDate") String departureDate);
    List<Order> selectByStatus(Integer status);
    List<Order> selectExpiredOrders(@Param("currentTime") String currentTime);
    int updateOrderStatus(@Param("id") Long id, @Param("orderStatus") Integer orderStatus);
    int updatePayStatus(@Param("id") Long id, @Param("payStatus") Integer payStatus);
    int cancelOrder(@Param("id") Long id);
}