package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.AccessRecordPageItem;
import com.example.demo.entity.AccessDevice;
import com.example.demo.entity.AccessRecord;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.AccessDeviceMapper;
import com.example.demo.mapper.AccessRecordMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.AccessRecordService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 访问记录服务实现类
 */
@Service
@RequiredArgsConstructor
public class AccessRecordServiceImpl extends ServiceImpl<AccessRecordMapper, AccessRecord> implements AccessRecordService {

    private final AccessRecordMapper accessRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final AccessDeviceMapper accessDeviceMapper;

    @Override
    public Page<AccessRecordPageItem> getRecordPage(Page<AccessRecord> page, String keyword, String result,
                                                    Long deviceId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AccessRecord> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(result)) {
            wrapper.eq(AccessRecord::getResult, result);
        }

        if (deviceId != null) {
            wrapper.eq(AccessRecord::getDeviceId, deviceId);
        }

        if (startDate != null) {
            wrapper.ge(AccessRecord::getAccessTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }

        if (endDate != null) {
            wrapper.le(AccessRecord::getAccessTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }

        wrapper.orderByDesc(AccessRecord::getAccessTime);
        Page<AccessRecord> recordPage = accessRecordMapper.selectPage(page, wrapper);

        Set<Long> userIds = recordPage.getRecords().stream()
                .map(AccessRecord::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));

        Set<Long> deviceIds = recordPage.getRecords().stream()
                .map(AccessRecord::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AccessDevice> deviceMap = deviceIds.isEmpty()
                ? Map.of()
                : accessDeviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(AccessDevice::getId, device -> device));

        List<AccessRecordPageItem> items = new ArrayList<>();
        for (AccessRecord record : recordPage.getRecords()) {
            SysUser user = userMap.get(record.getUserId());
            AccessDevice device = deviceMap.get(record.getDeviceId());
            if (StringUtils.isNotBlank(keyword)) {
                String keywordValue = keyword.trim();
                boolean matched = (user != null && (
                        StringUtils.containsIgnoreCase(user.getRealName(), keywordValue)
                                || StringUtils.containsIgnoreCase(user.getUsername(), keywordValue)
                                || StringUtils.containsIgnoreCase(user.getEmployeeNo(), keywordValue)
                )) || (device != null && (
                        StringUtils.containsIgnoreCase(device.getDeviceName(), keywordValue)
                                || StringUtils.containsIgnoreCase(device.getDeviceCode(), keywordValue)
                ));
                if (!matched) {
                    continue;
                }
            }

            AccessRecordPageItem item = new AccessRecordPageItem();
            item.setId(record.getId());
            item.setUserId(record.getUserId());
            item.setRealName(user != null ? user.getRealName() : null);
            item.setDeviceId(record.getDeviceId());
            item.setDeviceName(device != null ? device.getDeviceName() : null);
            item.setSuccess("success".equalsIgnoreCase(record.getResult()));
            item.setSimilarityScore(record.getSimilarityScore());
            item.setMatchDistance(record.getMatchDistance());
            item.setThreshold(record.getThreshold());
            item.setSnapshotPath(record.getSnapshotPath());
            item.setFailReason(record.getFailReason());
            item.setAccessTime(record.getAccessTime());
            items.add(item);
        }

        Page<AccessRecordPageItem> resultPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        resultPage.setRecords(items);
        return resultPage;
    }

    @Override
    public Page<AccessRecordPageItem> getMyRecordPage(Long userId, Page<AccessRecord> page, String result,
                                                      Long deviceId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AccessRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessRecord::getUserId, userId);

        if (StringUtils.isNotBlank(result)) {
            wrapper.eq(AccessRecord::getResult, result);
        }
        if (deviceId != null) {
            wrapper.eq(AccessRecord::getDeviceId, deviceId);
        }
        if (startDate != null) {
            wrapper.ge(AccessRecord::getAccessTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(AccessRecord::getAccessTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }

        wrapper.orderByDesc(AccessRecord::getAccessTime);
        Page<AccessRecord> recordPage = accessRecordMapper.selectPage(page, wrapper);

        Set<Long> deviceIds = recordPage.getRecords().stream()
                .map(AccessRecord::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AccessDevice> deviceMap = deviceIds.isEmpty()
                ? Map.of()
                : accessDeviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(AccessDevice::getId, device -> device));

        SysUser currentUser = sysUserMapper.selectById(userId);
        List<AccessRecordPageItem> items = new ArrayList<>();
        for (AccessRecord record : recordPage.getRecords()) {
            AccessRecordPageItem item = new AccessRecordPageItem();
            item.setId(record.getId());
            item.setUserId(record.getUserId());
            item.setRealName(currentUser != null ? currentUser.getRealName() : null);
            item.setDeviceId(record.getDeviceId());
            item.setDeviceName(deviceMap.get(record.getDeviceId()) != null ? deviceMap.get(record.getDeviceId()).getDeviceName() : null);
            item.setSuccess("success".equalsIgnoreCase(record.getResult()));
            item.setSimilarityScore(record.getSimilarityScore());
            item.setMatchDistance(record.getMatchDistance());
            item.setThreshold(record.getThreshold());
            item.setSnapshotPath(record.getSnapshotPath());
            item.setFailReason(record.getFailReason());
            item.setAccessTime(record.getAccessTime());
            items.add(item);
        }

        Page<AccessRecordPageItem> resultPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        resultPage.setRecords(items);
        return resultPage;
    }

    @Override
    public boolean createRecord(AccessRecord record) {
        return accessRecordMapper.insert(record) > 0;
    }

    @Override
    public Map<String, Object> getTodayStats() {
        return accessRecordMapper.selectTodayStats(LocalDate.now());
    }

    @Override
    public Map<String, Object> getWeekTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> successCounts = new ArrayList<>();
        List<Long> failCounts = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());

            Map<String, Object> dayStats = accessRecordMapper.selectTodayStats(date);
            Long successCount = dayStats.get("successCount") != null ? 
                ((Number) dayStats.get("successCount")).longValue() : 0L;
            Long failCount = dayStats.get("failCount") != null ? 
                ((Number) dayStats.get("failCount")).longValue() : 0L;

            successCounts.add(successCount);
            failCounts.add(failCount);
        }

        result.put("dates", dates);
        result.put("successCounts", successCounts);
        result.put("failCounts", failCounts);

        return result;
    }
}
