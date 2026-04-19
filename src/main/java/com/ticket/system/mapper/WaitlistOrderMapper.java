package com.ticket.system.mapper;

import com.ticket.system.entity.WaitlistOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WaitlistOrderMapper {

    int insert(WaitlistOrder waitlistOrder);

    int update(WaitlistOrder waitlistOrder);

    int delete(Long id);

    WaitlistOrder selectById(Long id);

    WaitlistOrder selectByWaitlistNumber(String waitlistNumber);

    /** 根据用户ID查询候补订单列表 */
    List<WaitlistOrder> selectByUserId(Long userId);

    /** 根据用户ID和状态查询候补订单列表 */
    List<WaitlistOrder> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /** 查询待匹配的候补订单（按创建时间升序，先到先得） */
    List<WaitlistOrder> selectPendingOrders(@Param("trainId") Long trainId,
                                            @Param("startStationId") Long startStationId,
                                            @Param("endStationId") Long endStationId,
                                            @Param("seatType") String seatType,
                                            @Param("departureDate") String departureDate);

    /** 查询所有待匹配的候补订单（按创建时间升序） */
    List<WaitlistOrder> selectAllPendingOrders();

    /** 查询已过期的候补订单（匹配后24小时未支付） */
    List<WaitlistOrder> selectExpiredMatchedOrders(@Param("currentTime") String currentTime);

    /** 更新候补订单状态 */
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updateTime") String updateTime);

    /** 关联正式订单号 */
    int bindOrderNumber(@Param("id") Long id, @Param("orderNumber") String orderNumber);
}
