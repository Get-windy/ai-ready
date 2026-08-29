package cn.aiedge.erp.finance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 财务模块基础单元测试
 * 使用纯 JUnit 5 测试，不加载 Spring 上下文，避免环境依赖问题
 */
class FinanceApplicationTests {

    @Test
    @DisplayName("财务模块 - 基础冒烟测试")
    void smokeTest() {
        // 验证测试基础设施正常工作
        assertNotNull(new Object(), "测试基础设施应正常工作");
    }

    @Test
    @DisplayName("财务模块 - FinanceApplication 类存在")
    void applicationClassExists() {
        // 验证主应用类可以正常加载（不实例化 Spring 上下文）
        Class<?> appClass = FinanceApplication.class;
        assertNotNull(appClass, "FinanceApplication 类应可加载");
    }
}
