package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.AccessDevice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门禁设备Mapper接口
 */
@Mapper
public interface AccessDeviceMapper extends BaseMapper<AccessDevice> {
}
