package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysTenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TenantMapper extends BaseMapper<SysTenant> {

    @Select("SELECT * FROM sys_tenant WHERE tenant_name = #{tenantName} AND deleted = 0")
    SysTenant selectByTenantName(@Param("tenantName") String tenantName);

    @Select("SELECT * FROM sys_tenant WHERE tenant_code = #{tenantCode} AND deleted = 0")
    SysTenant selectByTenantCode(@Param("tenantCode") String tenantCode);
}