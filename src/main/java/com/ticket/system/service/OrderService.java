package com.ticket.system.service;

import com.ticket.system.dto.request.OrderCreateDTO;
import com.ticket.system.dto.response.OrderInfoDTO;
import java.util.List;

public interface OrderService {
    OrderInfoDTO createOrder(OrderCreateDTO orderCreateDTO);
    OrderInfoDTO getOrderById(Long orderId);
    OrderInfoDTO getOrderByNumber(String orderNumber);
    List<OrderInfoDTO> getUserOrders(Long userId);
    boolean cancelOrder(Long orderId);
    boolean payOrder(Long orderId);
    void checkOrderExpired();
    void autoCancelExpiredOrders();
    List<OrderInfoDTO> getOrdersByStatus(Integer status);
    void updateOrderStatus(Long orderId, Integer status);
}