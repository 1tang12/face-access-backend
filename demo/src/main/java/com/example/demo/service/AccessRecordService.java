package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.AccessRecordPageItem;
import com.example.demo.entity.AccessRecord;
import java.time.LocalDate;
import java.util.Map;

/**
 * 访问记录服务接口
 */
public interface AccessRecordService extends IService<AccessRecord> {

    /**
     * 分页查询访问记录
     */
    Page<AccessRecordPageItem> getRecordPage(Page<AccessRecord> page, String keyword, String result, Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询当前用户自己的访问记录
     */
    Page<AccessRecordPageItem> getMyRecordPage(Long userId, Page<AccessRecord> page, String result, Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * 创建访问记录
     */
    boolean createRecord(AccessRecord record);

    /**
     * 统计今日访问数据
     */
    Map<String, Object> getTodayStats();

    /**
     * 统计最近7天访问趋势
     */
    Map<String, Object> getWeekTrend();
}
