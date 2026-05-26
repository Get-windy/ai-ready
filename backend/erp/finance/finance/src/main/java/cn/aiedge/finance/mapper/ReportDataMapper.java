package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.ReportData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportDataMapper extends BaseMapper<ReportData> {
    
    @Select("SELECT * FROM finance_report_data WHERE tenant_id = #{tenantId} AND deleted = 0 AND report_id = #{reportId} ORDER BY row_no")
    List<ReportData> listByReportId(@Param("tenantId") Long tenantId, @Param("reportId") Long reportId);
    
    @Select("SELECT * FROM finance_report_data WHERE tenant_id = #{tenantId} AND deleted = 0 AND report_id = #{reportId} AND row_code = #{rowCode}")
    ReportData getByReportIdAndRowCode(@Param("tenantId") Long tenantId, @Param("reportId") Long reportId, @Param("rowCode") String rowCode);
}