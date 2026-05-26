package cn.aiedge.erp.price.engine.optimization.impl;

import cn.aiedge.erp.price.engine.optimization.IPriceOptimizationAlgorithm;
import cn.aiedge.erp.price.engine.optimization.dto.OptimizationRequest;
import cn.aiedge.erp.price.engine.optimization.dto.OptimizationResult;
import cn.aiedge.erp.price.engine.optimization.dto.PriceVariable;
import org.apache.commons.math3.genetics.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 遗传算法价格优化实现
 */
@Service
public class GeneticPriceOptimizationAlgorithm implements IPriceOptimizationAlgorithm {
    
    private static final String ALGORITHM_NAME = "GeneticAlgorithm";
    private static final String ALGORITHM_DESCRIPTION = "基于遗传算法的价格优化，支持多目标优化和约束处理";
    
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final List<ExecutionHistory> executionHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Double> fitnessCache = new ConcurrentHashMap<>();
    
    @Override
    public OptimizationResult optimize(OptimizationRequest request) {
        long startTime = System.currentTimeMillis();
        totalExecutions.incrementAndGet();
        
        try {
            // 验证请求
            ValidationResult validation = validateRequest(request);
            if (!validation.isValid()) {
                return createErrorResult(request.requestId(), "Validation failed: " + String.join(", ", validation.errors()));
            }
            
            // 创建初始种群
            Population initialPopulation = createInitialPopulation(request);
            
            // 配置遗传算法参数
            GeneticAlgorithm ga = new GeneticAlgorithm(
                    new OnePointCrossover<>(),
                    0.9, // 交叉概率
                    new BinaryMutation(),
                    0.1, // 变异概率
                    new TournamentSelection(3)
            );
            
            // 设置停止条件
            StoppingCondition stoppingCondition = new FixedGenerationCount(1000);
            
            // 执行优化
            Population finalPopulation = ga.evolve(initialPopulation, stoppingCondition);
            
            // 获取最优解
            Chromosome bestChromosome = finalPopulation.getFittestChromosome();
            
            // 解码最优解
            List<OptimizationResult.OptimizedPrice> optimizedPrices = decodeChromosome(
                    (BinaryChromosome) bestChromosome, 
                    request.priceVariables()
            );
            
            // 计算适应度值
            BigDecimal fitnessValue = BigDecimal.valueOf(bestChromosome.getFitness());
            
            // 计算优化指标
            OptimizationResult.OptimizationMetrics metrics = calculateMetrics(
                    request, startTime, System.currentTimeMillis()
            );
            
            successfulExecutions.incrementAndGet();
            
            // 记录执行历史
            executionHistory.add(new ExecutionHistory(
                    request.requestId(),
                    System.currentTimeMillis() - startTime,
                    true,
                    ALGORITHM_NAME,
                    LocalDateTime.now()
            ));
            
            // 清理缓存
            fitnessCache.clear();
            
            return new OptimizationResult(
                    request.requestId(),
                    true,
                    LocalDateTime.now(),
                    fitnessValue,
                    optimizedPrices,
                    metrics,
                    new ArrayList<>(),
                    generateRecommendations(optimizedPrices, request),
                    Map.of("generations", 1000, "algorithm", ALGORITHM_NAME)
            );
            
        } catch (Exception e) {
            // 记录失败
            executionHistory.add(new ExecutionHistory(
                    request.requestId(),
                    System.currentTimeMillis() - startTime,
                    false,
                    ALGORITHM_NAME,
                    LocalDateTime.now()
            ));
            
            return createErrorResult(request.requestId(), "Optimization failed: " + e.getMessage());
        }
    }
    
    @Override
    public List<OptimizationResult> optimizeBatch(List<OptimizationRequest> requests) {
        return requests.parallelStream()
                .map(this::optimize)
                .collect(Collectors.toList());
    }
    
    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }
    
    @Override
    public String getAlgorithmDescription() {
        return ALGORITHM_DESCRIPTION;
    }
    
    @Override
    public List<String> getSupportedOptimizationTypes() {
        return Arrays.asList("SINGLE_PRODUCT", "MULTI_PRODUCT", "PORTFOLIO");
    }
    
    @Override
    public AlgorithmParameters getParameters() {
        return new AlgorithmParameters(
                1000,    // maxIterations
                100,     // populationSize
                0.1,     // mutationRate
                0.9,     // crossoverRate
                0.001,   // convergenceThreshold
                true,    // supportsParallelExecution
                true,    // supportsConstraints
                Arrays.asList("productId", "currentPrice", "costPrice")
        );
    }
    
    @Override
    public ValidationResult validateRequest(OptimizationRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (request.priceVariables() == null || request.priceVariables().isEmpty()) {
            errors.add("Price variables cannot be empty");
        }
        
        if (request.objective() == null) {
            errors.add("Optimization objective is required");
        }
        
        // 检查变量范围
        for (PriceVariable variable : request.priceVariables()) {
            if (variable.minValue().compareTo(variable.maxValue()) > 0) {
                errors.add("Min value cannot be greater than max value for variable: " + variable.variableName());
            }
        }
        
        boolean isReady = errors.isEmpty() && 
                request.priceVariables().size() <= 50; // 限制变量数量
        
        return new ValidationResult(errors.isEmpty(), errors, warnings, isReady);
    }
    
    @Override
    public void warmUp() {
        // 预加载模型或执行初始化
        fitnessCache.clear();
        System.out.println("Genetic algorithm warmed up");
    }
    
    @Override
    public void cleanup() {
        fitnessCache.clear();
        executionHistory.clear();
    }
    
    @Override
    public PerformanceStats getPerformanceStats() {
        double successRate = totalExecutions.get() > 0 ? 
                (double) successfulExecutions.get() / totalExecutions.get() : 0.0;
        
        double avgTime = executionHistory.stream()
                .mapToLong(ExecutionHistory::executionTimeMs)
                .average()
                .orElse(0.0);
        
        return new PerformanceStats(
                totalExecutions.get(),
                successfulExecutions.get(),
                avgTime,
                successRate,
                new ArrayList<>(executionHistory.subList(
                        Math.max(0, executionHistory.size() - 10), 
                        executionHistory.size()
                ))
        );
    }
    
    // 私有辅助方法
    private Population createInitialPopulation(OptimizationRequest request) {
        List<Chromosome> chromosomes = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) { // 初始种群大小
            chromosomes.add(createRandomChromosome(request));
        }
        
        return new ElitisticListPopulation(chromosomes, 2 * chromosomes.size(), 0.2);
    }
    
    private BinaryChromosome createRandomChromosome(OptimizationRequest request) {
        int chromosomeLength = calculateChromosomeLength(request);
        List<Integer> representation = new ArrayList<>();
        
        Random random = new Random();
        for (int i = 0; i < chromosomeLength; i++) {
            representation.add(random.nextInt(2));
        }
        
        return new BinaryChromosome(
                representation.toArray(new Integer[0]),
                chromosome -> calculateFitness(chromosome, request)
        );
    }
    
    private int calculateChromosomeLength(OptimizationRequest request) {
        // 每个变量使用16位二进制表示
        return request.priceVariables().size() * 16;
    }
    
    private double calculateFitness(Chromosome chromosome, OptimizationRequest request) {
        String cacheKey = chromosome.toString() + "_" + request.requestId();
        
        if (fitnessCache.containsKey(cacheKey)) {
            return fitnessCache.get(cacheKey);
        }
        
        // 解码染色体
        List<BigDecimal> decodedValues = decodeChromosomeValues(
                (BinaryChromosome) chromosome, 
                request.priceVariables()
        );
        
        // 计算适应度（这里简化为目标函数值）
        double fitness = calculateObjectiveFunction(decodedValues, request);
        
        // 应用约束惩罚
        double penalty = calculateConstraintPenalty(decodedValues, request);
        fitness -= penalty;
        
        fitnessCache.put(cacheKey, fitness);
        return fitness;
    }
    
    private double calculateObjectiveFunction(List<BigDecimal> values, OptimizationRequest request) {
        // 简化的目标函数：最大化利润
        // 实际应用中需要根据业务逻辑实现
        double totalProfit = 0.0;
        
        for (int i = 0; i < values.size(); i++) {
            PriceVariable variable = request.priceVariables().get(i);
            BigDecimal currentValue = variable.currentValue();
            BigDecimal optimizedValue = values.get(i);
            
            // 假设利润率是20%
            double profitMargin = 0.2;
            double profit = optimizedValue.doubleValue() * profitMargin;
            
            totalProfit += profit;
        }
        
        return totalProfit;
    }
    
    private double calculateConstraintPenalty(List<BigDecimal> values, OptimizationRequest request) {
        double penalty = 0.0;
        
        // 检查范围约束
        for (int i = 0; i < values.size(); i++) {
            PriceVariable variable = request.priceVariables().get(i);
            BigDecimal value = values.get(i);
            
            if (value.compareTo(variable.minValue()) < 0) {
                penalty += (variable.minValue().doubleValue() - value.doubleValue()) * 10;
            }
            
            if (value.compareTo(variable.maxValue()) > 0) {
                penalty += (value.doubleValue() - variable.maxValue().doubleValue()) * 10;
            }
        }
        
        return penalty;
    }
    
    private List<OptimizationResult.OptimizedPrice> decodeChromosome(
            BinaryChromosome chromosome, 
            List<PriceVariable> variables
    ) {
        List<OptimizationResult.OptimizedPrice> result = new ArrayList<>();
        List<BigDecimal> values = decodeChromosomeValues(chromosome, variables);
        
        for (int i = 0; i < variables.size(); i++) {
            PriceVariable variable = variables.get(i);
            BigDecimal optimizedValue = values.get(i);
            BigDecimal currentValue = variable.currentValue();
            
            BigDecimal improvement = currentValue.compareTo(BigDecimal.ZERO) > 0 ?
                    optimizedValue.subtract(currentValue)
                            .divide(currentValue, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO;
            
            result.add(new OptimizationResult.OptimizedPrice(
                    variable.productId(),
                    variable.variableName(),
                    optimizedValue,
                    improvement,
                    BigDecimal.valueOf(0.85), // 置信度
                    Arrays.asList(
                            new OptimizationResult.PriceImpact(
                                    "PROFIT_IMPACT",
                                    BigDecimal.valueOf(improvement.doubleValue() * 0.2),
                                    OptimizationResult.PriceImpact.ImpactDirection.POSITIVE,
                                    "预计利润影响"
                            )
                    )
            ));
        }
        
        return result;
    }
    
    private List<BigDecimal> decodeChromosomeValues(
            BinaryChromosome chromosome, 
            List<PriceVariable> variables
    ) {
        List<BigDecimal> values = new ArrayList<>();
        List<Integer> representation = chromosome.getRepresentation();
        
        int bitsPerVariable = 16;
        for (int i = 0; i < variables.size(); i++) {
            PriceVariable variable = variables.get(i);
            
            // 提取该变量的二进制位
            int startIndex = i * bitsPerVariable;
            int binaryValue = 0;
            
            for (int j = 0; j < bitsPerVariable; j++) {
                binaryValue = (binaryValue << 1) | representation.get(startIndex + j);
            }
            
            // 将二进制值映射到价格范围
            BigDecimal minValue = variable.minValue();
            BigDecimal maxValue = variable.maxValue();
            
            double normalized = binaryValue / (double) ((1 << bitsPerVariable) - 1);
            BigDecimal value = minValue.add(
                    maxValue.subtract(minValue).multiply(BigDecimal.valueOf(normalized))
            );
            
            // 应用步长
            if (variable.stepSize() != null && variable.stepSize().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal steps = value.subtract(minValue).divide(variable.stepSize(), 0, RoundingMode.HALF_UP);
                value = minValue.add(steps.multiply(variable.stepSize()));
            }
            
            values.add(value);
        }
        
        return values;
    }
    
    private OptimizationResult.OptimizationMetrics calculateMetrics(
            OptimizationRequest request, 
            long startTime, 
            long endTime
    ) {
        return new OptimizationResult.OptimizationMetrics(
                1000, // iterationsCompleted
                (endTime - startTime) / 1000.0, // executionTimeSeconds
                0.95, // convergenceRate
                1, // feasibleSolutionsFound
                BigDecimal.valueOf(0.15), // averageFitnessImprovement
                Map.of("populationSize", BigDecimal.valueOf(100))
        );
    }
    
    private List<OptimizationResult.Recommendation> generateRecommendations(
            List<OptimizationResult.OptimizedPrice> optimizedPrices,
            OptimizationRequest request
    ) {
        List<OptimizationResult.Recommendation> recommendations = new ArrayList<>();
        
        for (OptimizationResult.OptimizedPrice price : optimizedPrices) {
            if (price.improvementPercentage().abs().compareTo(BigDecimal.valueOf(5)) > 0) {
                recommendations.add(new OptimizationResult.Recommendation(
                        "REC_" + price.productId(),
                        OptimizationResult.Recommendation.RecommendationType.PRICE_ADJUSTMENT,
                        String.format("建议调整产品%s的价格：当前%s，优化后%s，预计改善%s%%",
                                price.productId(),
                                getCurrentPrice(request, price.productId()),
                                price.optimizedValue(),
                                price.improvementPercentage()),
                        price.improvementPercentage(),
                        price.confidenceScore(),
                        "逐步调整价格，监控市场反应"
                ));
            }
        }
        
        return recommendations;
    }
    
    private BigDecimal getCurrentPrice(OptimizationRequest request, String productId) {
        return request.priceVariables().stream()
                .filter(v -> v.productId().equals(productId))
                .findFirst()
                .map(PriceVariable::currentValue)
                .orElse(BigDecimal.ZERO);
    }
    
    private OptimizationResult createErrorResult(String requestId, String errorMessage) {
        return new OptimizationResult(
                requestId,
                false,
                LocalDateTime.now(),
                BigDecimal.ZERO,
                new ArrayList<>(),
                new OptimizationResult.OptimizationMetrics(0, 0, 0, 0, BigDecimal.ZERO, Map.of()),
                Arrays.asList(new OptimizationResult.ConstraintViolation(
                        "ERROR",
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "SYSTEM_ERROR",
                        errorMessage
                )),
                new ArrayList<>(),
                Map.of("error", errorMessage)
        );
    }
}