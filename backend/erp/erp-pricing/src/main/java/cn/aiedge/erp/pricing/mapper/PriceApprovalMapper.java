package cn.aiedge.erp.pricing.mapper;

import cn.aiedge.erp.pricing.entity.PriceApproval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 价格审批单 Mapper。
 *
 * <p>查询全部走 MyBatis-Plus 的条件构造器即可，无需自定义 SQL：
 * 租户条件由租户拦截器自动注入，逻辑删除由 {@code @TableLogic} 自动加 {@code deleted = 0}。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PriceApprovalMapper extends BaseMapper<PriceApproval> {
}
