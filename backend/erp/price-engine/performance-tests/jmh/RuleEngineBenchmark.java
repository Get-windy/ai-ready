package cn.aiedge.erp.price.engine.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import cn.aiedge.erp.price.engine.dto.PriceRequest;
import cn.aiedge.erp.price.engine.dto.PriceResponse;

import java.util.concurrent.TimeUnit;

/**
 * Drools规则引擎性能基准测试
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class RuleEngineBenchmark {
    
    private KieContainer kieContainer;
    private KieSession kieSession;
    private PriceRequest priceRequest;
    
    @Setup(Level.Trial)
    public void setup() {
        // 初始化Drools规则引擎
        KieServices kieServices = KieServices.Factory.get();
        kieContainer = kieServices.newKieClasspathContainer();
        
        // 创建测试数据
        priceRequest = new PriceRequest(
            "P001",           // 产品ID
            "CUST001",        // 客户ID
            "GOLD",           // 客户等级
            100.0,            // 原价
            10,               // 购买数量
            "2026-05-01",     // 交易日期
            "CNY"             // 货币
        );
    }
    
    @Setup(Level.Iteration)
    public void setupSession() {
        // 每次迭代创建新的KieSession
        kieSession = kieContainer.newKieSession("price-rules-session");
    }
    
    @TearDown(Level.Iteration)
    public void tearDownSession() {
        // 清理会话
        if (kieSession != null) {
            kieSession.dispose();
        }
    }
    
    @Benchmark
    public void benchmarkRuleExecution(Blackhole blackhole) {
        // 执行规则引擎测试
        kieSession.insert(priceRequest);
        kieSession.fireAllRules();
        
        // 获取结果（模拟）
        PriceResponse response = new PriceResponse(
            priceRequest.getProductId(),
            90.0,  // 折扣后价格
            10.0,  // 折扣金额
            "GOLD_MEMBER_DISCOUNT"
        );
        
        blackhole.consume(response);
    }
    
    @Benchmark
    public void benchmarkMultipleRules(Blackhole blackhole) {
        // 测试多规则执行
        for (int i = 0; i < 10; i++) {
            PriceRequest request = new PriceRequest(
                "P00" + i,
                "CUST00" + i,
                i % 3 == 0 ? "GOLD" : (i % 3 == 1 ? "SILVER" : "BRONZE"),
                100.0 + i,
                i + 1,
                "2026-05-01",
                "CNY"
            );
            
            kieSession.insert(request);
        }
        
        int firedRules = kieSession.fireAllRules();
        blackhole.consume(firedRules);
    }
    
    @Benchmark
    @Threads(4)
    public void benchmarkConcurrentRuleExecution(Blackhole blackhole) {
        // 并发规则执行测试
        kieSession.insert(priceRequest);
        int firedRules = kieSession.fireAllRules();
        blackhole.consume(firedRules);
    }
}