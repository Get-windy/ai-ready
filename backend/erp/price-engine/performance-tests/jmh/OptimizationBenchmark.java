package cn.aiedge.erp.price.engine.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import cn.aiedge.erp.price.engine.dto.PriceStrategy;
import cn.aiedge.erp.price.engine.optimization.GeneticPriceOptimizationAlgorithm;

/**
 * 价格优化算法性能基准测试
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class OptimizationBenchmark {
    
    private GeneticPriceOptimizationAlgorithm optimizer;
    private List<PriceStrategy> smallDataSet;
    private List<PriceStrategy> mediumDataSet;
    private List<PriceStrategy> largeDataSet;
    
    @Setup(Level.Trial)
    public void setup() {
        // 初始化优化器
        optimizer = new GeneticPriceOptimizationAlgorithm();
        
        // 创建测试数据集
        smallDataSet = createTestData(100);
        mediumDataSet = createTestData(1000);
        largeDataSet = createTestData(10000);
    }
    
    private List<PriceStrategy> createTestData(int size) {
        List<PriceStrategy> strategies = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            PriceStrategy strategy = new PriceStrategy(
                "STRATEGY-" + i,
                "策略-" + i,
                i % 3 == 0 ? "CUSTOMER_LEVEL" : (i % 3 == 1 ? "QUANTITY" : "TIME"),
                i % 3 == 0 ? "GOLD" : (i % 3 == 1 ? "10" : "Q2"),
                i % 5 == 0 ? "FIXED_DISCOUNT" : "PERCENTAGE_DISCOUNT",
                i % 10 + 5,
                i % 20 + 50,
                "2026-01-01",
                "2026-12-31"
            );
            strategies.add(strategy);
        }
        return strategies;
    }
    
    @Benchmark
    public void benchmarkSmallOptimization(Blackhole blackhole) {
        // 小数据集优化
        List<PriceStrategy> result = optimizer.optimize(smallDataSet, 100);
        blackhole.consume(result);
    }
    
    @Benchmark
    public void benchmarkMediumOptimization(Blackhole blackhole) {
        // 中数据集优化
        List<PriceStrategy> result = optimizer.optimize(mediumDataSet, 500);
        blackhole.consume(result);
    }
    
    @Benchmark
    public void benchmarkLargeOptimization(Blackhole blackhole) {
        // 大数据集优化
        List<PriceStrategy> result = optimizer.optimize(largeDataSet, 1000);
        blackhole.consume(result);
    }
    
    @Benchmark
    public void benchmarkIterationImpact(Blackhole blackhole) {
        // 测试迭代次数对性能的影响
        List<PriceStrategy> result50 = optimizer.optimize(mediumDataSet, 50);
        List<PriceStrategy> result100 = optimizer.optimize(mediumDataSet, 100);
        List<PriceStrategy> result500 = optimizer.optimize(mediumDataSet, 500);
        
        blackhole.consume(result50);
        blackhole.consume(result100);
        blackhole.consume(result500);
    }
    
    @Benchmark
    @Threads(2)
    public void benchmarkConcurrentOptimization(Blackhole blackhole) {
        // 并发优化测试
        List<PriceStrategy> result = optimizer.optimize(smallDataSet, 100);
        blackhole.consume(result);
    }
    
    @Benchmark
    public void benchmarkMemoryUsage() {
        // 内存使用测试
        List<PriceStrategy> result = optimizer.optimize(largeDataSet, 1000);
        
        // 触发GC以测量内存
        System.gc();
        System.gc();
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 返回内存使用量（MB）
        double usedMemoryMB = usedMemory / (1024.0 * 1024.0);
        
        // 在基准测试中，我们通过Blackhole消耗结果
        // 实际内存测量需要专门的内存分析工具
    }
}