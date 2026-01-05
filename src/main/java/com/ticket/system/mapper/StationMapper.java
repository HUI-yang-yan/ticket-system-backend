package com.ticket.system.mapper;

import com.ticket.system.entity.Station;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface StationMapper {
    int insert(Station station);
    int update(Station station);
    int delete(Long id);
    Station selectById(Long id);
    Station selectByStationCode(String stationCode);
    Station selectByStationName(String stationName);
    List<Station> selectAll();
    List<Station> selectByCity(String city);
    List<Station> selectByProvince(String province);
    List<Station> searchByKeyword(String keyword);
}