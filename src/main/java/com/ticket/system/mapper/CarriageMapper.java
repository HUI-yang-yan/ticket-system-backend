package com.ticket.system.mapper;

import com.ticket.system.entity.Carriage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CarriageMapper {
    int insert(Carriage carriage);
    int update(Carriage carriage);
    int delete(Long id);
    Carriage selectById(Long id);
    List<Carriage> selectByTrainId(Long trainId);
    Carriage selectByTrainAndIndex(@Param("trainId") Long trainId,
                                   @Param("carriageIndex") Integer carriageIndex);
    List<Carriage> selectByTrainAndType(@Param("trainId") Long trainId,
                                        @Param("carriageType") String carriageType);
}