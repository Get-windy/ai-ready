package cn.aiedge.erp.purchase;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试用最小启动类
 * erp-purchase 为类库模块，本身没有 @SpringBootConfiguration，
 * @WebMvcTest/@SpringBootTest 切片测试需要一个可发现的配置类。
 * 仅扫描 cn.aiedge.erp.purchase 包，避免加载无关配置。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@SpringBootApplication
public class TestApplication {
}
