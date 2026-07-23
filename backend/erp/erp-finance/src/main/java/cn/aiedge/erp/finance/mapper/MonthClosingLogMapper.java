package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.MonthClosingLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 月结操作日志Mapper接口
 */
@Mapper
public interface MonthClosingLogMapper extends BaseMapper<MonthClosingLog> {

    /**
     * 查询该期间最近一次月结/反月结日志
     */
    @Select("SELECT * FROM fin_month_closing_log WHERE tenant_id = #{tenantId} AND period_code = #{periodCode} ORDER BY id DESC LIMIT 1")
    Optional<MonthClosingLog> findLatestByPeriodCode(Long tenantId, String periodCode);
}
