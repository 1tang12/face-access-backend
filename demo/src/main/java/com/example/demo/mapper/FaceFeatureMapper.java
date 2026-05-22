package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.FaceFeature;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人脸特征Mapper接口
 */
@Mapper
public interface FaceFeatureMapper extends BaseMapper<FaceFeature> {
}
