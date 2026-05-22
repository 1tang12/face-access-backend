package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.OperationLog;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public boolean createLog(OperationLog operationLog) {
        return operationLogMapper.insert(operationLog) > 0;
    }
}
