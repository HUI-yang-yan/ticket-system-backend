package com.ticket.system.service;

import com.ticket.system.dto.request.WaitlistCreateDTO;
import com.ticket.system.dto.response.WaitlistOrderDTO;
import com.ticket.system.dto.response.WaitlistTicketDTO;

import java.util.List;

public interface WaitlistService {

    /**
     * 创建候补购票请求
     */
    WaitlistOrderDTO createWaitlistOrder(WaitlistCreateDTO dto);

    /**
     * 取消候补订单
     */
    boolean cancelWaitlistOrder(Long waitlistOrderId);

    /**
     * 查询用户的候补订单列表
     */
    List<WaitlistOrderDTO> getUserWaitlistOrders(Long userId);

    /**
     * 根据ID查询候补订单详情
     */
    WaitlistOrderDTO getWaitlistOrderById(Long id);

    /**
     * 查询候补池可用票（按车次、日期）
     */
    List<WaitlistTicketDTO> queryAvailableTickets(Long trainId, Long startStationId,
                                                   Long endStationId, String seatType,
                                                   String departureDate);

    /**
     * 退款成功后，将票转入候补池
     */
    void transferRefundTicketToWaitlist(Long orderId);

    /**
     * 匹配候补订单（当有新的候补票时触发）
     */
    void matchWaitlistWithRefund(Long trainId, Long startStationId, Long endStationId,
                                 String seatType, String departureDate);

    /**
     * 自动处理候补队列（定时任务调用）
     * 1. 扫描待匹配订单，尝试匹配可用票
     * 2. 处理已匹配但超时的订单
     */
    void autoProcessWaitlist();

    /**
     * 候补订单支付成功后处理
     */
    void handleWaitlistOrderPaid(String waitlistNumber);
}
