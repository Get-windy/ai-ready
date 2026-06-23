package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.FinanceReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务报表Mapper接口
 */
@Mapper
public interface FinanceReportMapper extends BaseMapper<FinanceReport> {

    /**
     * 根据报表编号查询
     */
    @Select("SELECT * FROM finance_report WHERE report_no = #{reportNo} AND deleted_flag = 0")
    FinanceReport findByReportNo(String reportNo);

    /**
     * 根据报表类型查询
     */
    @Select("SELECT * FROM finance_report WHERE report_type = #{reportType} AND deleted_flag = 0")
    List<FinanceReport> findByReportType(Integer reportType);

    /**
     * 根据报表期间查询
     */
    @Select("SELECT * FROM finance_report WHERE report_period = #{reportPeriod} AND deleted_flag = 0")
    List<FinanceReport> findByReportPeriod(String reportPeriod);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM finance_report WHERE status = #{status} AND deleted_flag = 0")
    List<FinanceReport> findByStatus(Integer status);

    /**
     * 根据租户ID查询
     */
    @Select("SELECT * FROM finance_report WHERE tenant_id = #{tenantId} AND deleted_flag = 0")
    List<FinanceReport> findByTenantId(String tenantId);

    /**
     * 查询最新的报表
     */
    @Select("SELECT * FROM finance_report WHERE deleted_flag = #{deletedFlag} ORDER BY generated_at DESC")
    List<FinanceReport> findByDeletedFlagOrderByGeneratedAtDesc(Integer deletedFlag);
}
