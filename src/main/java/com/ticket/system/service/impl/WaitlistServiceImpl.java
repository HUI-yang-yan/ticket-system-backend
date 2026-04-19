package com.ticket.system.service.impl;

import com.ticket.system.common.constant.OrderConstant;
import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.RedisUtil;
import com.ticket.system.common.util.SnowflakeIdUtil;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.WaitlistCreateDTO;
import com.ticket.system.dto.response.WaitlistOrderDTO;
import com.ticket.system.dto.response.WaitlistTicketDTO;
import com.ticket.system.entity.*;
import com.ticket.system.mapper.*;
import com.ticket.system.service.WaitlistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WaitlistServiceImpl implements WaitlistService {

    @Autowired
    private WaitlistOrderMapper waitlistOrderMapper;

    @Autowired
    private WaitlistTicketMapper waitlistTicketMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private SnowflakeIdUtil snowflakeIdUtil;

    @Autowired
    private RedisUtil redisUtil;

    private static final String WAITLIST_LOCK_PREFIX = "waitlist:lock:";
    private static final String WAITLIST_NUMBER_PREFIX = "W";

    @Override
    @Transactional
    public WaitlistOrderDTO createWaitlistOrder(WaitlistCreateDTO dto) {
        Long userId = ThreadLocalUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN.getCode(), "用户未登录");
        }

        // 获取分布式锁
        String lockKey = WAITLIST_LOCK_PREFIX + userId;
        String lockValue = UUID.randomUUID().toString();
        try {
            boolean locked = redisUtil.lock(lockKey, lockValue, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
            }

            // 检查列车是否存在
            Train train = trainMapper.selectById(dto.getTrainId());
            if (train == null) {
                throw new BusinessException(ErrorCode.TRAIN_NOT_EXIST.getCode(), "列车不存在");
            }

            // 检查站点是否存在
            Station startStation = stationMapper.selectById(dto.getStartStationId());
            if (startStation == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "出发站不存在");
            }
            Station endStation = stationMapper.selectById(dto.getEndStationId());
            if (endStation == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "到达站不存在");
            }

            // 创建候补订单
            WaitlistOrder waitlistOrder = new WaitlistOrder();
            BeanUtils.copyProperties(dto, waitlistOrder);
            waitlistOrder.setWaitlistNumber(snowflakeIdUtil.generateOrderNumber().replace("TICKET", WAITLIST_NUMBER_PREFIX));
            waitlistOrder.setUserId(userId);
            waitlistOrder.setStatus(OrderConstant.WAITLIST_STATUS_PENDING);
            waitlistOrder.setCreateTime(new Date());
            waitlistOrder.setUpdateTime(new Date());

            int result = waitlistOrderMapper.insert(waitlistOrder);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "创建候补订单失败");
            }

            // 尝试立即匹配候补票
            matchWaitlistWithRefund(dto.getTrainId(), dto.getStartStationId(),
                    dto.getEndStationId(), dto.getSeatType(),
                    dto.getDepartureDate().toString());

            return convertToWaitlistOrderDTO(waitlistOrder, train, startStation, endStation);

        } finally {
            redisUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional
    public boolean cancelWaitlistOrder(Long waitlistOrderId) {
        Long userId = ThreadLocalUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN.getCode(), "用户未登录");
        }

        WaitlistOrder waitlistOrder = waitlistOrderMapper.selectById(waitlistOrderId);
        if (waitlistOrder == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "候补订单不存在");
        }

        // 检查是否是当前用户的订单
        if (!waitlistOrder.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "无权取消此候补订单");
        }

        // 检查订单状态
        if (!OrderConstant.WAITLIST_STATUS_PENDING.equals(waitlistOrder.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "当前状态不允许取消");
        }

        waitlistOrder.setStatus(OrderConstant.WAITLIST_STATUS_CANCELLED);
        waitlistOrder.setUpdateTime(new Date());

        int result = waitlistOrderMapper.update(waitlistOrder);
        return result > 0;
    }

    @Override
    public List<WaitlistOrderDTO> getUserWaitlistOrders(Long userId) {
        List<WaitlistOrder> orders = waitlistOrderMapper.selectByUserId(userId);
        return orders.stream()
                .map(this::convertToWaitlistOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WaitlistOrderDTO getWaitlistOrderById(Long id) {
        WaitlistOrder waitlistOrder = waitlistOrderMapper.selectById(id);
        if (waitlistOrder == null) {
            return null;
        }
        return convertToWaitlistOrderDTO(waitlistOrder);
    }

    @Override
    public List<WaitlistTicketDTO> queryAvailableTickets(Long trainId, Long startStationId,
                                                         Long endStationId, String seatType,
                                                         String departureDate) {
        List<WaitlistTicket> tickets;
        if (trainId != null && startStationId != null && endStationId != null
                && seatType != null && departureDate != null) {
            tickets = waitlistTicketMapper.selectAvailableTickets(
                    trainId, startStationId, endStationId, seatType, departureDate);
        } else {
            tickets = waitlistTicketMapper.selectAllAvailableTickets();
        }
        return tickets.stream()
                .map(this::convertToWaitlistTicketDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void transferRefundTicketToWaitlist(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("退款订单不存在，无法转入候补池: orderId={}", orderId);
            return;
        }

        // 检查是否已经转入过候补池（防止重复处理）
        WaitlistTicket existingTicket = waitlistTicketMapper.selectByOriginalOrderNumber(order.getOrderNumber());
        if (existingTicket != null) {
            log.info("该订单已转入候补池: orderNumber={}", order.getOrderNumber());
            return;
        }

        // 创建候补票
        WaitlistTicket waitlistTicket = new WaitlistTicket();
        waitlistTicket.setTicketNumber(snowflakeIdUtil.generateOrderNumber().replace("TICKET", "TKT"));
        waitlistTicket.setOriginalOrderNumber(order.getOrderNumber());
        waitlistTicket.setTrainId(order.getTrainId());
        waitlistTicket.setStartStationId(order.getDepartureStationId());
        waitlistTicket.setEndStationId(order.getArrivalStationId());
        waitlistTicket.setDepartureDate(order.getDepartureDate().toLocalDate());
        waitlistTicket.setSeatType(order.getSeatType());
        waitlistTicket.setPrice(order.getTicketPrice());
        waitlistTicket.setStatus(OrderConstant.WAITLIST_TICKET_AVAILABLE);
        waitlistTicket.setSource(OrderConstant.WAITLIST_TICKET_SOURCE_REFUND);
        waitlistTicket.setCreateTime(new Date());
        waitlistTicket.setUpdateTime(new Date());

        waitlistTicketMapper.insert(waitlistTicket);
        log.info("退款订单已转入候补池: orderNumber={}, ticketNumber={}",
                order.getOrderNumber(), waitlistTicket.getTicketNumber());

        // 触发候补匹配
        matchWaitlistWithRefund(order.getTrainId(), order.getDepartureStationId(),
                order.getArrivalStationId(), order.getSeatType(),
                order.getDepartureDate().toLocalDate().toString());
    }

    @Override
    @Transactional
    public void matchWaitlistWithRefund(Long trainId, Long startStationId, Long endStationId,
                                         String seatType, String departureDate) {
        // 按 车次+日期+座位类型+区间 加分布式锁，防止并发重复匹配
        String lockKey = "waitlist:match:lock:" + trainId + ":" + startStationId + ":" + endStationId + ":" + seatType + ":" + departureDate;
        String lockValue = UUID.randomUUID().toString();
        try {
            boolean locked = redisUtil.lock(lockKey, lockValue, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.debug("获取候补匹配锁失败，跳过: trainId={}, date={}", trainId, departureDate);
                return; // 获取锁失败，下次定时任务或下次触发再处理
            }

            // 循环匹配直到无可用匹配
            boolean matched = true;
            while (matched) {
                matched = matchOneTicketToOneOrder(trainId, startStationId, endStationId, seatType, departureDate);
            }

        } finally {
            redisUtil.unlock(lockKey, lockValue);
        }
    }

    /**
     * 匹配一个候补票到一个候补订单
     * @return true表示匹配成功，false表示无可匹配
     */
    private boolean matchOneTicketToOneOrder(Long trainId, Long startStationId, Long endStationId,
                                              String seatType, String departureDate) {
        // 查询可用候补票（按创建时间升序，先到先得）
        List<WaitlistTicket> availableTickets = waitlistTicketMapper.selectAvailableTickets(
                trainId, startStationId, endStationId, seatType, departureDate);
        if (availableTickets == null || availableTickets.isEmpty()) {
            return false;
        }

        // 查询待匹配的候补订单（先到先得，按创建时间排序）
        List<WaitlistOrder> pendingOrders = waitlistOrderMapper.selectPendingOrders(
                trainId, startStationId, endStationId, seatType, departureDate);
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            return false;
        }

        // 匹配逻辑：一个候补票匹配一个候补订单
        WaitlistTicket ticket = availableTickets.get(0);
        WaitlistOrder order = pendingOrders.get(0);

        // 更新候补票状态为已被候补购得
        waitlistTicketMapper.updateStatus(ticket.getId(), OrderConstant.WAITLIST_TICKET_CLAIMED,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // 更新候补订单状态为已匹配
        order.setStatus(OrderConstant.WAITLIST_STATUS_MATCHED);
        order.setOrderNumber(ticket.getTicketNumber());
        // 设置24小时过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        order.setExpireTime(calendar.getTime());
        order.setUpdateTime(new Date());
        waitlistOrderMapper.update(order);

        log.info("候补匹配成功: waitlistNumber={}, ticketNumber={}, orderId={}, userId={}",
                order.getWaitlistNumber(), ticket.getTicketNumber(), order.getId(), order.getUserId());

        return true; // 匹配成功，继续尝试匹配下一个
    }

    @Override
    @Transactional
    public void autoProcessWaitlist() {
        // 1. 扫描所有待匹配订单，尝试匹配可用票
        List<WaitlistOrder> allPendingOrders = waitlistOrderMapper.selectAllPendingOrders();

        for (WaitlistOrder order : allPendingOrders) {
            try {
                matchWaitlistWithRefund(order.getTrainId(), order.getStartStationId(),
                        order.getEndStationId(), order.getSeatType(),
                        order.getDepartureDate().toString());
            } catch (Exception e) {
                log.error("自动匹配候补订单异常: waitlistNumber={}", order.getWaitlistNumber(), e);
            }
        }

        // 2. 处理已匹配但超时的订单
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        List<WaitlistOrder> expiredOrders = waitlistOrderMapper.selectExpiredMatchedOrders(currentTime);

        for (WaitlistOrder order : expiredOrders) {
            try {
                // 将候补订单状态改为已过期
                order.setStatus(OrderConstant.WAITLIST_STATUS_EXPIRED);
                order.setUpdateTime(new Date());
                waitlistOrderMapper.update(order);

                // 释放候补票回可用状态
                WaitlistTicket ticket = waitlistTicketMapper.selectByTicketNumber(order.getOrderNumber());
                if (ticket != null && OrderConstant.WAITLIST_TICKET_CLAIMED.equals(ticket.getStatus())) {
                    ticket.setStatus(OrderConstant.WAITLIST_TICKET_AVAILABLE);
                    ticket.setUpdateTime(new Date());
                    waitlistTicketMapper.update(ticket);
                    log.info("候补订单超时已释放: waitlistNumber={}, ticketNumber={}",
                            order.getWaitlistNumber(), ticket.getTicketNumber());
                }
            } catch (Exception e) {
                log.error("处理过期候补订单异常: waitlistNumber={}", order.getWaitlistNumber(), e);
            }
        }
    }

    @Override
    @Transactional
    public void handleWaitlistOrderPaid(String waitlistNumber) {
        WaitlistOrder waitlistOrder = waitlistOrderMapper.selectByWaitlistNumber(waitlistNumber);
        if (waitlistOrder == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "候补订单不存在");
        }

        if (!OrderConstant.WAITLIST_STATUS_MATCHED.equals(waitlistOrder.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "候补订单状态不允许支付");
        }

        // 1. 获取候补票信息
        WaitlistTicket ticket = waitlistTicketMapper.selectByTicketNumber(waitlistOrder.getOrderNumber());
        if (ticket == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "候补票信息不存在");
        }

        // 2. 创建正式订单
        Train train = trainMapper.selectById(waitlistOrder.getTrainId());
        Station startStation = stationMapper.selectById(waitlistOrder.getStartStationId());
        Station endStation = stationMapper.selectById(waitlistOrder.getEndStationId());

        Order order = new Order();
        order.setOrderNumber(snowflakeIdUtil.generateOrderNumber());
        order.setUserId(waitlistOrder.getUserId());
        order.setTrainId(waitlistOrder.getTrainId());
        order.setDepartureStationId(waitlistOrder.getStartStationId());
        order.setArrivalStationId(waitlistOrder.getEndStationId());
        order.setDepartureDate(waitlistOrder.getDepartureDate().atStartOfDay());
        order.setSeatType(waitlistOrder.getSeatType());
        order.setTicketPrice(ticket.getPrice());
        order.setOrderStatus(OrderConstant.ORDER_STATUS_PAID); // 直接设置为已支付
        order.setPayStatus(OrderConstant.PAY_STATUS_PAID);
        order.setPayTime(new Date());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        if (train != null) {
            LocalDateTime departure = order.getDepartureDate();
            order.setDepartureTime(LocalDateTime.of(departure.toLocalDate(), train.getStartTime()));
            order.setArrivalTime(LocalDateTime.of(departure.toLocalDate(), train.getEndTime()));
        }

        orderMapper.insert(order);

        // 3. 标记候补订单成功
        waitlistOrder.setStatus(OrderConstant.WAITLIST_STATUS_SUCCESS);
        waitlistOrder.setUpdateTime(new Date());
        waitlistOrderMapper.update(waitlistOrder);

        // 4. 标记候补票已使用（不变，还是 CLAIMED）
        ticket.setUpdateTime(new Date());
        waitlistTicketMapper.update(ticket);

        log.info("候补购票成功: waitlistNumber={}, orderNumber={}, userId={}",
                waitlistNumber, order.getOrderNumber(), waitlistOrder.getUserId());
    }

    private WaitlistOrderDTO convertToWaitlistOrderDTO(WaitlistOrder waitlistOrder) {
        WaitlistOrderDTO dto = new WaitlistOrderDTO();
        BeanUtils.copyProperties(waitlistOrder, dto);

        // 填充额外信息
        Train train = trainMapper.selectById(waitlistOrder.getTrainId());
        if (train != null) {
            dto.setTrainNumber(train.getTrainNumber());
        }
        Station startStation = stationMapper.selectById(waitlistOrder.getStartStationId());
        if (startStation != null) {
            dto.setStartStationName(startStation.getStationName());
        }
        Station endStation = stationMapper.selectById(waitlistOrder.getEndStationId());
        if (endStation != null) {
            dto.setEndStationName(endStation.getStationName());
        }

        return dto;
    }

    private WaitlistOrderDTO convertToWaitlistOrderDTO(WaitlistOrder waitlistOrder, Train train,
                                                        Station startStation, Station endStation) {
        WaitlistOrderDTO dto = new WaitlistOrderDTO();
        BeanUtils.copyProperties(waitlistOrder, dto);

        if (train != null) {
            dto.setTrainNumber(train.getTrainNumber());
        }
        if (startStation != null) {
            dto.setStartStationName(startStation.getStationName());
        }
        if (endStation != null) {
            dto.setEndStationName(endStation.getStationName());
        }

        return dto;
    }

    private WaitlistTicketDTO convertToWaitlistTicketDTO(WaitlistTicket ticket) {
        WaitlistTicketDTO dto = new WaitlistTicketDTO();
        BeanUtils.copyProperties(ticket, dto);

        // 填充额外信息
        Train train = trainMapper.selectById(ticket.getTrainId());
        if (train != null) {
            dto.setTrainNumber(train.getTrainNumber());
        }
        Station startStation = stationMapper.selectById(ticket.getStartStationId());
        if (startStation != null) {
            dto.setStartStationName(startStation.getStationName());
        }
        Station endStation = stationMapper.selectById(ticket.getEndStationId());
        if (endStation != null) {
            dto.setEndStationName(endStation.getStationName());
        }

        return dto;
    }
}
