package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.AttendanceRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤规则Mapper接口
 */
@Mapper
public interface AttendanceRuleMapper extends BaseMapper<AttendanceRule> {
}
