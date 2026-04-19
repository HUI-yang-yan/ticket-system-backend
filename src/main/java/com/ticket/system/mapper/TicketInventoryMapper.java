package com.ticket.system.mapper;

import com.ticket.system.entity.TicketInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketInventoryMapper {

    int insert(TicketInventory inventory);

    int insertBatch(List<TicketInventory> inventories);

    int update(TicketInventory inventory);

    int updateAvailableCount(@Param("id") Long id, @Param("availableCount") Integer availableCount);

    int delete(Long id);

    int deleteByTrainId(@Param("trainId") Long trainId);

    TicketInventory selectById(Long id);

    TicketInventory selectByTrainAndType(@Param("trainId") Long trainId,
                                        @Param("seatType") String seatType);

    List<TicketInventory> selectByTrainId(@Param("trainId") Long trainId);

    List<TicketInventory> selectByCondition(@Param("trainId") Long trainId,
                                           @Param("seatType") String seatType,
                                           @Param("status") Integer status);

    int reduceInventory(@Param("id") Long id,
                        @Param("availableCount") Integer availableCount,
                        @Param("version") Integer version);

    int increaseInventory(@Param("id") Long id,
                          @Param("availableCount") Integer availableCount,
                          @Param("version") Integer version);

    /**
     * 原子增加库存（解决并发丢失更新问题）
     * @param id 库存记录ID
     * @param increment 增加的数量
     * @return 影响行数
     */
    int increaseInventoryAtomic(@Param("id") Long id, @Param("increment") Integer increment);

    TicketInventory selectForUpdate(@Param("trainId") Long trainId,
                                    @Param("seatType") String seatType);

    TicketInventory selectForUpdateById(@Param("id") Long id);

    List<TicketInventory> selectAll();

    List<TicketInventory> selectByTrainIds(@Param("trainIds") List<Long> trainIds);
}
