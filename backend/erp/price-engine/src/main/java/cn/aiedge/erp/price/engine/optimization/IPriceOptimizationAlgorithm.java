package cn.aiedge.erp.price.engine.optimization;

import cn.aiedge.erp.price.engine.optimization.dto.OptimizationRequest;
import cn.aiedge.erp.price.engine.optimization.dto.OptimizationResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格优化算法接口
 */
public interface IPriceOptimizationAlgorithm {
    
    /**
     * 执行价格优化
     * @param request 优化请求
     * @return 优化结果
     */
    OptimizationResult optimize(OptimizationRequest request);
    
    /**
     * 批量优化
     * @param requests 优化请求列表
     * @return 优化结果列表
     */
    List<OptimizationResult> optimizeBatch(List<OptimizationRequest> requests);
    
    /**
     * 获取算法名称
     * @return 算法名称
     */
    String getAlgorithmName();
    
    /**
     * 获取算法描述
     * @return 算法描述
     */
    String getAlgorithmDescription();
    
    /**
     * 支持的优化类型
     * @return 支持的优化类型列表
     */
    List<String> getSupportedOptimizationTypes();
    
    /**
     * 算法参数配置
     * @return 参数配置描述
     */
    AlgorithmParameters getParameters();
    
    /**
     * 验证优化请求
     * @param request 优化请求
     * @return 验证结果
     */
    ValidationResult validateRequest(OptimizationRequest request);
    
    /**
     * 预热算法（加载模型等）
     */
    void warmUp();
    
    /**
     * 清理算法资源
     */
    void cleanup();
    
    /**
     * 算法性能统计
     * @return 性能统计
     */
    PerformanceStats getPerformanceStats();
    
    /**
     * 算法参数
     */
    record AlgorithmParameters(
            int maxIterations,
            int populationSize,
            double mutationRate,
            double crossoverRate,
            double convergenceThreshold,
            boolean supportsParallelExecution,
            boolean supportsConstraints,
            List<String> requiredContextFields
    ) {}
    
    /**
     * 验证结果
     */
    record ValidationResult(
            boolean isValid,
            List<String> errors,
            List<String> warnings,
            boolean isReadyForOptimization
    ) {}
    
    /**
     * 性能统计
     */
    record PerformanceStats(
            long totalExecutions,
            long successfulExecutions,
            double averageExecutionTimeMs,
            double successRate,
            List<ExecutionHistory> recentExecutions
    ) {}
    
    /**
     * 执行历史
     */
    record ExecutionHistory(
            String requestId,
            long executionTimeMs,
            boolean success,
            String algorithmUsed,
            LocalDateTime executionTime
    ) {}
}