package cn.aiedge.dms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 配送管理系统 (DMS) 配置
 * 作为核心应用的一部分运行，非独立服务
 */
@Configuration
@MapperScan({"cn.aiedge.dms.*.mapper", "cn.aiedge.base.mapper", "cn.aiedge.base.log.mapper", "cn.aiedge.common.mapper"})
@ComponentScan(basePackages = {"cn.aiedge.dms", "cn.aiedge.base", "cn.aiedge.common"})
public class DmsApplication {
}
