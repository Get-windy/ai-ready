package cn.aiedge.erp.printing.mapper;

import cn.aiedge.erp.printing.entity.v2.SysPrintClient;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysPrintClientMapper extends BaseMapper<SysPrintClient> {

    /**
     * 根据机器标识和租户 ID 查找客户端
     */
    @Select("SELECT * FROM sys_print_client WHERE machine_id = #{machineId} AND tenant_id = #{tenantId} AND deleted = 0")
    SysPrintClient selectByMachineId(@Param("machineId") String machineId, @Param("tenantId") Long tenantId);
}
