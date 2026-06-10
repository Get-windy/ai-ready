package cn.aiedge.erp.printing.mapper;

import cn.aiedge.erp.printing.entity.PrintTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrintTaskMapper extends BaseMapper<PrintTask> {

    @Select("SELECT * FROM erp_print_task WHERE tenant_id = #{tenantId} AND status = 'PENDING' ORDER BY priority DESC, create_time ASC LIMIT #{limit}")
    List<PrintTask> selectPendingTasks(@Param("limit") int limit, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_print_task WHERE tenant_id = #{tenantId} AND status = #{status}")
    int countByStatus(@Param("status") String status, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_print_task WHERE tenant_id = #{tenantId} AND printer_id = #{printerId} AND status IN ('PENDING', 'QUEUED', 'PRINTING')")
    int countActiveTasksByPrinter(@Param("printerId") Long printerId, @Param("tenantId") Long tenantId);
}