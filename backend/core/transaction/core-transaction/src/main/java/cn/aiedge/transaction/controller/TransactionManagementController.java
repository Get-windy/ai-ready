package cn.aiedge.transaction.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.transaction.entity.DistributedTransactionLog;
import cn.aiedge.transaction.model.TransactionStatus;
import cn.aiedge.transaction.service.TransactionManager;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分布式事务管理控制器
 * 提供事务状态查询、异常事务处理、事务统计分析等管理功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionManagementController {

    private final TransactionManager transactionManager;

    /**
     * 分页查询事务日志
     * 
     * @param page 页码
     * @param size 页面大小
     * @param status 事务状态
     * @param transactionMode 事务模式
     * @return 事务日志分页结果
     */
    @GetMapping("/logs/page")
    public Result<Page<DistributedTransactionLog>> getTransactionLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String transactionMode) {
        
        Page<DistributedTransactionLog> pager = new Page<>(page, size);
        QueryWrapper<DistributedTransactionLog> queryWrapper = new QueryWrapper<>();
        
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        if (transactionMode != null && !transactionMode.isEmpty()) {
            queryWrapper.eq("transaction_mode", transactionMode);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        Page<DistributedTransactionLog> result = new Page<>();
        // 实际项目中需要注入mapper来查询数据
        // Page<DistributedTransactionLog> result = transactionLogMapper.selectPage(pager, queryWrapper);
        
        return Result.success(result);
    }

    /**
     * 查询事务状态
     * 
     * @param txId 事务ID
     * @return 事务状态
     */
    @GetMapping("/status/{txId}")
    public Result<TransactionStatus> getTransactionStatus(@PathVariable String txId) {
        TransactionStatus status = transactionManager.checkTransactionStatus(txId);
        return Result.success(status);
    }

    /**
     * 重试失败的事务
     * 
     * @param txId 事务ID
     * @return 操作结果
     */
    @PostMapping("/retry/{txId}")
    public Result<Boolean> retryFailedTransaction(@PathVariable String txId) {
        Boolean result = transactionManager.retryFailedTransaction(txId);
        return Result.success(result);
    }

    /**
     * 处理超时事务
     * 
     * @param txId 事务ID
     * @return 操作结果
     */
    @PostMapping("/handle-timeout/{txId}")
    public Result<Boolean> handleTimeoutTransaction(@PathVariable String txId) {
        Boolean result = transactionManager.handleTimeoutTransaction(txId);
        return Result.success(result);
    }

    /**
     * 获取事务统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result<Object> getTransactionStatistics() {
        // 这里应该查询统计数据并返回
        // 实际项目中需要从数据库获取统计信息
        
        return Result.success(null);
    }

    /**
     * 获取异常事务列表
     * 
     * @return 异常事务列表
     */
    @GetMapping("/exceptions")
    public Result<List<DistributedTransactionLog>> getExceptionTransactions() {
        QueryWrapper<DistributedTransactionLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("status", TransactionStatus.FAILED.getCode(), 
                       TransactionStatus.TIMEOUT.getCode(), 
                       TransactionStatus.WAITING_COMPENSATION.getCode());
        
        // 实际项目中需要注入mapper来查询数据
        // List<DistributedTransactionLog> result = transactionLogMapper.selectList(queryWrapper);
        List<DistributedTransactionLog> result = List.of(); // 空列表作为占位符
        
        return Result.success(result);
    }
}