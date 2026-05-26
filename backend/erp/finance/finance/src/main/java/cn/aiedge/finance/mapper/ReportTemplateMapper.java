package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.ReportTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportTemplateMapper extends BaseMapper<ReportTemplate> {
    
    @Select("SELECT * FROM finance_report_template WHERE tenant_id = #{tenantId} AND deleted = 0 AND report_type = #{reportType} AND enabled = 1 ORDER BY display_order")
    List<ReportTemplate> listByReportType(@Param("tenantId") Long tenantId, @Param("reportType") Integer reportType);
    
    @Select("SELECT * FROM finance_report_template WHERE tenant_id = #{tenantId} AND deleted = 0 AND template_code = #{templateCode}")
    ReportTemplate getByCode(@Param("tenantId") Long tenantId, @Param("templateCode") String templateCode);
    
    @Select("SELECT MAX(row_no) FROM finance_report_template WHERE tenant_id = #{tenantId} AND report_type = #{reportType}")
    Integer getMaxRowNo(@Param("tenantId") Long tenantId, @Param("reportType") Integer reportType);
}