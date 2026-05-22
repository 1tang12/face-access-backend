package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.AttendanceRecordPageItem;
import com.example.demo.entity.AttendanceRecord;
import com.example.demo.entity.AttendanceRule;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.AttendanceRecordMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.AttendanceRecordService;
import com.example.demo.service.AttendanceRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceRecordServiceImpl extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord> implements AttendanceRecordService {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final AttendanceRuleService attendanceRuleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord recordAttendance(Long userId, LocalDateTime accessTime) {
        LocalDate attendanceDate = accessTime.toLocalDate();
        AttendanceRule rule = attendanceRuleService.getByDate(attendanceDate);
        if (rule == null || !Integer.valueOf(1).equals(rule.getNeedAttendance()) || !Integer.valueOf(1).equals(rule.getStatus())) {
            return getByUserIdAndDate(userId, attendanceDate);
        }

        LocalTime currentTime = accessTime.toLocalTime();
        boolean inCheckInWindow = isBetween(currentTime, rule.getCheckInStartTime(), rule.getCheckInEndTime());
        boolean inCheckOutWindow = isBetween(currentTime, rule.getCheckOutStartTime(), rule.getCheckOutEndTime());
        if (!inCheckInWindow && !inCheckOutWindow) {
            return getByUserIdAndDate(userId, attendanceDate);
        }

        AttendanceRecord record = getByUserIdAndDate(userId, attendanceDate);
        if (record == null) {
            record = new AttendanceRecord();
            record.setUserId(userId);
            record.setAttendanceDate(attendanceDate);
            record.setLateMinutes(0);
            record.setEarlyMinutes(0);
            record.setStatus("normal");
            record.setRemark("门禁识别生成考勤");
        }

        if (inCheckInWindow && record.getCheckInTime() == null) {
            record.setCheckInTime(accessTime);
            record.setLateMinutes(calculateLateMinutes(currentTime, rule.getWorkStartTime()));
            record.setRemark("门禁识别签到");
        }

        if (inCheckOutWindow && (record.getCheckOutTime() == null || accessTime.isAfter(record.getCheckOutTime()))) {
            record.setCheckOutTime(accessTime);
            if (record.getCheckInTime() != null) {
                record.setWorkDuration((int) Duration.between(record.getCheckInTime(), accessTime).toMinutes());
            }
            record.setEarlyMinutes(calculateEarlyMinutes(currentTime, rule.getWorkEndTime()));
            record.setRemark("门禁识别签退");
        }

        record.setStatus(resolveStatus(record));
        if (record.getId() == null) {
            attendanceRecordMapper.insert(record);
        } else {
            attendanceRecordMapper.updateById(record);
        }
        return record;
    }

    @Override
    public AttendanceRecord getByUserIdAndDate(Long userId, LocalDate attendanceDate) {
        return attendanceRecordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getUserId, userId)
                        .eq(AttendanceRecord::getAttendanceDate, attendanceDate)
        );
    }

    @Override
    public Page<AttendanceRecord> getMyAttendancePage(Long userId, Page<AttendanceRecord> page, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getUserId, userId);
        if (startDate != null) {
            wrapper.ge(AttendanceRecord::getAttendanceDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(AttendanceRecord::getAttendanceDate, endDate);
        }
        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate);
        return attendanceRecordMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<AttendanceRecordPageItem> getAttendancePage(Page<AttendanceRecord> page, String keyword, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(AttendanceRecord::getAttendanceDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(AttendanceRecord::getAttendanceDate, endDate);
        }
        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate);
        Page<AttendanceRecord> recordsPage = attendanceRecordMapper.selectPage(page, wrapper);

        Set<Long> userIds = recordsPage.getRecords().stream()
                .map(AttendanceRecord::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));

        String keywordValue = StringUtils.trimToEmpty(keyword);
        List<AttendanceRecordPageItem> items = new ArrayList<>();
        for (AttendanceRecord record : recordsPage.getRecords()) {
            SysUser user = userMap.get(record.getUserId());
            if (StringUtils.isNotBlank(keywordValue)) {
                boolean matched = user != null && (
                        StringUtils.containsIgnoreCase(user.getUsername(), keywordValue)
                                || StringUtils.containsIgnoreCase(user.getRealName(), keywordValue)
                                || StringUtils.containsIgnoreCase(user.getEmployeeNo(), keywordValue)
                );
                if (!matched) {
                    continue;
                }
            }
            items.add(toPageItem(record, user));
        }

        Page<AttendanceRecordPageItem> result = new Page<>(
                recordsPage.getCurrent(),
                recordsPage.getSize(),
                StringUtils.isBlank(keywordValue) ? recordsPage.getTotal() : items.size()
        );
        result.setRecords(items);
        return result;
    }

    private AttendanceRecordPageItem toPageItem(AttendanceRecord record, SysUser user) {
        AttendanceRecordPageItem item = new AttendanceRecordPageItem();
        item.setId(record.getId());
        item.setUserId(record.getUserId());
        item.setRealName(user != null ? user.getRealName() : null);
        item.setAttendanceDate(record.getAttendanceDate());
        item.setCheckInTime(record.getCheckInTime());
        item.setCheckOutTime(record.getCheckOutTime());
        item.setWorkDuration(record.getWorkDuration());
        item.setStatus(record.getStatus());
        item.setLateMinutes(record.getLateMinutes());
        item.setEarlyMinutes(record.getEarlyMinutes());
        item.setRemark(record.getRemark());
        item.setCreateTime(record.getCreateTime());
        item.setUpdateTime(record.getUpdateTime());
        return item;
    }

    @Override
    public Map<String, Object> getAttendanceSummary(LocalDate startDate, LocalDate endDate) {
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(6);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AttendanceRecord::getAttendanceDate, from, to);
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", records.size());
        result.put("normalCount", records.stream().filter(item -> "normal".equals(item.getStatus())).count());
        result.put("lateCount", records.stream().filter(item -> "late".equals(item.getStatus()) || "late_early".equals(item.getStatus())).count());
        result.put("earlyCount", records.stream().filter(item -> "early".equals(item.getStatus()) || "late_early".equals(item.getStatus())).count());
        result.put("checkedOutCount", records.stream().filter(item -> item.getCheckOutTime() != null).count());
        return result;
    }

    private int calculateLateMinutes(LocalTime checkInTime, LocalTime workStartTime) {
        if (!checkInTime.isAfter(workStartTime)) {
            return 0;
        }
        return (int) Duration.between(workStartTime, checkInTime).toMinutes();
    }

    private int calculateEarlyMinutes(LocalTime checkOutTime, LocalTime workEndTime) {
        if (!checkOutTime.isBefore(workEndTime)) {
            return 0;
        }
        return (int) Duration.between(checkOutTime, workEndTime).toMinutes();
    }

    private boolean isBetween(LocalTime currentTime, LocalTime startTime, LocalTime endTime) {
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }

    private String resolveStatus(AttendanceRecord record) {
        boolean late = record.getLateMinutes() != null && record.getLateMinutes() > 0;
        boolean early = record.getEarlyMinutes() != null && record.getEarlyMinutes() > 0;
        if (late && early) {
            return "late_early";
        }
        if (late) {
            return "late";
        }
        if (early) {
            return "early";
        }
        return "normal";
    }
}
