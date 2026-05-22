package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤记录Mapper接口
 */
@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {
}
