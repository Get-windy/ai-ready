package com.aiready.system.service.impl;

import com.aiready.system.entity.SystemLog;
import com.aiready.system.mapper.SystemLogMapper;
import com.aiready.system.service.SystemLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志服务实现类
 */
@Service
public class SystemLogServiceImpl extends ServiceImpl<SystemLogMapper, SystemLog> implements SystemLogService {

    @Override
    public List<SystemLog> listByType(Integer logType) {
        return this.list(
            new LambdaQueryWrapper<SystemLog>()
                .eq(SystemLog::getLogType, logType)
                .eq(SystemLog::getDeleted, 0)
                .orderByDesc(SystemLog::getCreateTime)
        );
    }

    @Override
    public List<SystemLog> listByUserId(Long userId) {
        return this.list(
            new LambdaQueryWrapper<SystemLog>()
                .eq(SystemLog::getUserId, userId)
                .eq(SystemLog::getDeleted, 0)
                .orderByDesc(SystemLog::getCreateTime)
        );
    }

    @Override
    public List<SystemLog> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return this.list(
            new LambdaQueryWrapper<SystemLog>()
                .ge(SystemLog::getCreateTime, startTime)
                .le(SystemLog::getCreateTime, endTime)
                .eq(SystemLog::getDeleted, 0)
                .orderByDesc(SystemLog::getCreateTime)
        );
    }

    @Override
    public List<SystemLog> listByModule(String moduleName) {
        return this.list(
            new LambdaQueryWrapper<SystemLog>()
                .eq(SystemLog::getModuleName, moduleName)
                .eq(SystemLog::getDeleted, 0)
                .orderByDesc(SystemLog::getCreateTime)
        );
    }

    @Override
    @Transactional
    public boolean cleanLogsBefore(LocalDateTime beforeTime) {
        return this.remove(
            new LambdaQueryWrapper<SystemLog>()
                .lt(SystemLog::getCreateTime, beforeTime)
        );
    }

    @Override
    public List<SystemLog> listByBusiness(String businessType, String businessKey) {
        return this.list(
            new LambdaQueryWrapper<SystemLog>()
                .eq(SystemLog::getBusinessType, businessType)
                .eq(SystemLog::getBusinessKey, businessKey)
                .eq(SystemLog::getDeleted, 0)
                .orderByDesc(SystemLog::getCreateTime)
        );
    }

    @Override
    @Transactional
    public boolean recordOperationLog(SystemLog log) {
        log.setLogType(1);
        log.setCreateTime(LocalDateTime.now());
        return this.save(log);
    }

    @Override
    @Transactional
    public boolean recordLoginLog(Long userId, String username, String ipAddress, Integer status, String errorMsg) {
        SystemLog log = new SystemLog();
        log.setLogType(2);
        log.setTitle(status == 1 ? "用户登录" : "登录失败");
        log.setOperationType(status == 1 ? "LOGIN" : "LOGIN_FAIL");
        log.setUserId(userId);
        log.setUsername(username);
        log.setIpAddress(ipAddress);
        log.setStatus(status);
        log.setErrorMsg(errorMsg);
        log.setCreateTime(LocalDateTime.now());
        return this.save(log);
    }

    @Override
    @Transactional
    public boolean recordExceptionLog(String title, String method, String errorMsg, String requestParams) {
        SystemLog log = new SystemLog();
        log.setLogType(3);
        log.setTitle(title);
        log.setMethod(method);
        log.setErrorMsg(errorMsg);
        log.setRequestParams(requestParams);
        log.setStatus(0);
        log.setCreateTime(LocalDateTime.now());
        return this.save(log);
    }

    @Override
    public List<SystemLog> getRecentLogsByUser(Long userId, Integer limit) {
        return this.list(
            new LambdaQueryWrapper<SystemLog>()
                .eq(SystemLog::getUserId, userId)
                .eq(SystemLog::getDeleted, 0)
                .orderByDesc(SystemLog::getCreateTime)
                .last("LIMIT " + limit)
        );
    }

    @Override
    public Long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return this.count(
            new LambdaQueryWrapper<SystemLog>()
                .ge(SystemLog::getCreateTime, startTime)
                .le(SystemLog::getCreateTime, endTime)
                .eq(SystemLog::getDeleted, 0)
        );
    }

    @Override
    public Long countByModule(String moduleName) {
        return this.count(
            new LambdaQueryWrapper<SystemLog>()
                .eq(SystemLog::getModuleName, moduleName)
                .eq(SystemLog::getDeleted, 0)
        );
    }

    @Override
    @Transactional
    public boolean batchDelete(List<Long> ids) {
        return this.removeByIds(ids);
    }
}
