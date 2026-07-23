package cn.aiedge.dms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 配送管理系统 (DMS) 配置
 * 作为核心应用的一部分运行，非独立服务
 */
@Configuration
@ComponentScan(basePackages = {"cn.aiedge.dms", "cn.aiedge.base", "cn.aiedge.common"})
public class DmsApplication {
}
