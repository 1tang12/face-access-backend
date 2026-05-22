package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    boolean createLog(OperationLog operationLog);
}
