package com.ticket.system.mapper;

import com.ticket.system.entity.WaitlistTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WaitlistTicketMapper {

    int insert(WaitlistTicket waitlistTicket);

    int update(WaitlistTicket waitlistTicket);

    int delete(Long id);

    WaitlistTicket selectById(Long id);

    WaitlistTicket selectByTicketNumber(String ticketNumber);

    /** 查询可候补的票（按创建时间升序） */
    List<WaitlistTicket> selectAvailableTickets(@Param("trainId") Long trainId,
                                                  @Param("startStationId") Long startStationId,
                                                  @Param("endStationId") Long endStationId,
                                                  @Param("seatType") String seatType,
                                                  @Param("departureDate") String departureDate);

    /** 查询所有可候补的票 */
    List<WaitlistTicket> selectAllAvailableTickets();

    /** 更新候补票状态 */
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updateTime") String updateTime);

    /** 根据原订单号查询候补票 */
    WaitlistTicket selectByOriginalOrderNumber(String originalOrderNumber);
}
