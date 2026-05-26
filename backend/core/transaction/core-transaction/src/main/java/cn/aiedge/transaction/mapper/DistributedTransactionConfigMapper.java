package cn.aiedge.transaction.mapper;

import cn.aiedge.transaction.entity.DistributedTransactionConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分布式事务配置映射器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface DistributedTransactionConfigMapper extends BaseMapper<DistributedTransactionConfig> {
    // 继承BaseMapper提供的基础CRUD方法
}