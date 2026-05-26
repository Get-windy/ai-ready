package cn.aiedge.gateway.mapper;

import cn.aiedge.gateway.entity.GatewayLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 网关日志映射器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface GatewayLogMapper extends BaseMapper<GatewayLog> {
    // 继承BaseMapper提供的基础CRUD方法
}
