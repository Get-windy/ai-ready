package cn.aiedge.erp.printing.mapper;

import cn.aiedge.erp.printing.entity.PrintLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PrintLogMapper extends BaseMapper<PrintLog> {

    @Select("SELECT COUNT(*) as total, SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as success, " +
            "SUM(CASE WHEN success = false THEN 1 ELSE 0 END) as failed, " +
            "SUM(print_duration) as total_duration, AVG(print_duration) as avg_duration " +
            "FROM erp_print_log WHERE print_time BETWEEN #{startTime} AND #{endTime}")
    Map<String, Object> selectStatistics(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);

    @Select("SELECT printer_name, COUNT(*) as count FROM erp_print_log " +
            "WHERE print_time BETWEEN #{startTime} AND #{endTime} GROUP BY printer_id, printer_name")
    List<Map<String, Object>> selectPrinterStats(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    @Select("SELECT template_name, COUNT(*) as count FROM erp_print_log " +
            "WHERE print_time BETWEEN #{startTime} AND #{endTime} GROUP BY template_id, template_name")
    List<Map<String, Object>> selectTemplateStats(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    @Select("SELECT document_type, COUNT(*) as count FROM erp_print_log " +
            "WHERE print_time BETWEEN #{startTime} AND #{endTime} GROUP BY document_type")
    List<Map<String, Object>> selectDocumentTypeStats(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);
}