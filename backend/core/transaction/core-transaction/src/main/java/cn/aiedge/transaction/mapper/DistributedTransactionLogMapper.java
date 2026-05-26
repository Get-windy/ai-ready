package cn.aiedge.transaction.mapper;

import cn.aiedge.transaction.entity.DistributedTransactionLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分布式事务日志映射器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface DistributedTransactionLogMapper extends BaseMapper<DistributedTransactionLog> {
    // 继承BaseMapper提供的基础CRUD方法
}