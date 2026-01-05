package com.ticket.system.service.impl;

import com.ticket.system.common.constant.OrderConstant;
import com.ticket.system.common.constant.RedisConstant;
import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.RedisUtil;
import com.ticket.system.common.util.SnowflakeIdUtil;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.OrderCreateDTO;
import com.ticket.system.dto.response.OrderInfoDTO;
import com.ticket.system.entity.Order;
import com.ticket.system.entity.Station;
import com.ticket.system.entity.Train;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.OrderMapper;
import com.ticket.system.mapper.StationMapper;
import com.ticket.system.mapper.TrainMapper;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.mq.producer.OrderProducer;
import com.ticket.system.service.OrderService;
import com.ticket.system.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SnowflakeIdUtil snowflakeIdUtil;

    @Autowired
    private OrderProducer orderProducer;
    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PaymentService paymentService;

    @Override
    @Transactional
    public OrderInfoDTO createOrder(OrderCreateDTO orderCreateDTO) {
        Long userId = ThreadLocalUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN.getCode(), "用户未登录");
        }

        // 获取分布式锁
        String lockKey = RedisConstant.ORDER_LOCK_PREFIX + userId;
        String lockValue = UUID.randomUUID().toString();

        try {
            boolean locked = redisUtil.lock(lockKey, lockValue, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
            }

            // 检查列车是否存在
            Train train = trainMapper.selectById(orderCreateDTO.getTrainId());
            if (train == null) {
                throw new BusinessException(ErrorCode.TRAIN_NOT_EXIST.getCode(), "列车不存在");
            }

            // 创建订单
            Order order = new Order();
            BeanUtils.copyProperties(orderCreateDTO, order);

            order.setUserId(userId);
            order.setDepartureTime(LocalDateTime.of(orderCreateDTO.getDepartureDate(), train.getStartTime()));
            order.setArrivalTime(LocalDateTime.of(orderCreateDTO.getDepartureDate(), train.getEndTime()));
            order.setOrderNumber(snowflakeIdUtil.generateOrderNumber());
            order.setOrderStatus(OrderConstant.ORDER_STATUS_PENDING);
            order.setPayStatus(OrderConstant.PAY_STATUS_PENDING);
            order.setDepartureDate(orderCreateDTO.getDepartureDate().atStartOfDay());
            // 设置过期时间（30分钟后）
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 30);
            order.setExpireTime(calendar.getTime());

            order.setCreateTime(new Date());
            order.setUpdateTime(new Date());

            // 计算票价（简化处理）
            BigDecimal ticketPrice = calculateTicketPrice(orderCreateDTO);
            order.setTicketPrice(ticketPrice);

            // 保存订单
            int result = orderMapper.insert(order);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED.getCode(), "创建订单失败");
            }

            // 发送订单创建消息到MQ
            orderProducer.sendOrderCreateMessage(order.getId());

            // 发送延迟消息（30分钟后检查支付状态）
            orderProducer.sendOrderDelayMessage(order.getId());

            // 临时存储订单信息到Redis（5分钟）
            String tempOrderKey = RedisConstant.ORDER_TEMP_PREFIX + order.getId();
            redisUtil.set(tempOrderKey, order, 5, TimeUnit.MINUTES);

            // 返回订单信息
            OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
            BeanUtils.copyProperties(order, orderInfoDTO);
            Station departureStation = stationMapper.selectById(order.getDepartureStationId());
            orderInfoDTO.setDepartureStationName(departureStation.getStationName());
            Station arrivalStation = stationMapper.selectById(order.getDepartureStationId());
            orderInfoDTO.setArrivalStationName(arrivalStation.getStationName());
            orderInfoDTO.setDepartureDate(order.getDepartureDate());
            orderInfoDTO.setDepartureTime(order.getDepartureTime());
            orderInfoDTO.setArrivalTime(order.getArrivalTime());

            User user = userMapper.selectById(ThreadLocalUtil.getUserId());
            orderInfoDTO.setUsername(user.getUsername());
            orderInfoDTO.setTrainNumber(train.getTrainNumber());

            return orderInfoDTO;

        } finally {
            // 释放锁
            redisUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    public OrderInfoDTO getOrderById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
        }

        OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
        BeanUtils.copyProperties(order, orderInfoDTO);

        return orderInfoDTO;
    }

    @Override
    public OrderInfoDTO getOrderByNumber(String orderNumber) {
        Order order = orderMapper.selectByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
        }

        OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
        BeanUtils.copyProperties(order, orderInfoDTO);

        return orderInfoDTO;
    }

    @Override
    public List<OrderInfoDTO> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.selectByUserId(userId);
        return orders.stream()
                .map(order -> {
                    OrderInfoDTO dto = new OrderInfoDTO();
                    BeanUtils.copyProperties(order, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
        }

        // 检查订单状态
        if (order.getOrderStatus() != OrderConstant.ORDER_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单状态不允许取消");
        }

        // 更新订单状态为已取消
        order.setOrderStatus(OrderConstant.ORDER_STATUS_CANCELLED);
        order.setUpdateTime(new Date());

        int result = orderMapper.update(order);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "取消订单失败");
        }

        // TODO: 释放锁定的座位，恢复库存

        return true;
    }

    @Override
    @Transactional
    public boolean payOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
        }

        // 检查订单状态
        if (order.getOrderStatus() != OrderConstant.ORDER_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单状态不允许支付");
        }

        // 检查订单是否过期
        if (order.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.ORDER_EXPIRED.getCode(), "订单已过期");
        }

        // 更新订单状态为已支付
        order.setOrderStatus(OrderConstant.ORDER_STATUS_PAID);
        order.setPayStatus(OrderConstant.PAY_STATUS_PAID);
        order.setPayTime(new Date());
        order.setUpdateTime(new Date());

        int result = orderMapper.update(order);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED.getCode(), "支付失败");
        }

        return true;
    }

    @Override
    public void checkOrderExpired() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        List<Order> expiredOrders = orderMapper.selectExpiredOrders(currentTime);
        for (Order order : expiredOrders) {
            if (order.getOrderStatus() == OrderConstant.ORDER_STATUS_PENDING) {
                log.info("自动取消过期订单: orderId={}, orderNumber={}", order.getId(), order.getOrderNumber());
                cancelOrder(order.getId());
            }
        }
    }

    @Override
    public void autoCancelExpiredOrders() {
        // 定时任务调用
        checkOrderExpired();
    }

    @Override
    public List<OrderInfoDTO> getOrdersByStatus(Integer status) {
        List<Order> orders = orderMapper.selectByStatus(status);
        return orders.stream()
                .map(order -> {
                    OrderInfoDTO dto = new OrderInfoDTO();
                    BeanUtils.copyProperties(order, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void updateOrderStatus(Long orderId, Integer status) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_EXIST.getCode(), "订单不存在");
        }

        order.setOrderStatus(status);
        order.setUpdateTime(new Date());

        orderMapper.update(order);
    }

    private BigDecimal calculateTicketPrice(OrderCreateDTO orderCreateDTO) {
        // 简化票价计算逻辑
        // 实际应该根据列车类型、座位类型、距离等计算
        return new BigDecimal("100.00");
    }
}