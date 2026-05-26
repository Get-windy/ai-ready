package com.aiready;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单元测试基类
 * 所有单元测试类应继承此类
 * 
 * @author qa-lead
 * @date 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractTest {

    protected static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

    /**
     * 测试前置准备
     */
    @BeforeEach
    public void setUp() {
        log.info("Starting test: {}", this.getClass().getSimpleName());
        init();
    }

    /**
     * 子类可重写此方法进行初始化
     */
    protected void init() {
        // 子类重写
    }

    /**
     * 打印测试信息
     */
    protected void logTestInfo(String message) {
        log.info("[TEST] {}", message);
    }

    /**
     * 打印测试步骤
     */
    protected void logTestStep(String step, String detail) {
        log.info("[STEP-{}] {}", step, detail);
    }

    /**
     * 打印验证结果
     */
    protected void logVerify(String expected, String actual) {
        log.info("[VERIFY] Expected: {}, Actual: {}", expected, actual);
    }
}
