package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.AccessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.Map;

/**
 * 访问记录Mapper接口
 */
@Mapper
public interface AccessRecordMapper extends BaseMapper<AccessRecord> {

    /**
     * 统计今日访问数据
     */
    @Select("SELECT " +
            "COUNT(*) as totalCount, " +
            "SUM(CASE WHEN result = 'success' THEN 1 ELSE 0 END) as successCount, " +
            "SUM(CASE WHEN result = 'fail' THEN 1 ELSE 0 END) as failCount " +
            "FROM access_record " +
            "WHERE DATE(access_time) = #{date}")
    Map<String, Object> selectTodayStats(LocalDate date);
}
