package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.AuxiliaryAccounting;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuxiliaryAccountingMapper extends BaseMapper<AuxiliaryAccounting> {
    
    @Select("SELECT * FROM finance_auxiliary_accounting WHERE tenant_id = #{tenantId} AND deleted = 0 AND auxiliary_type = #{auxiliaryType} AND enabled = 1 ORDER BY auxiliary_code")
    List<AuxiliaryAccounting> listByType(@Param("tenantId") Long tenantId, @Param("auxiliaryType") Integer auxiliaryType);
    
    @Select("SELECT * FROM finance_auxiliary_accounting WHERE tenant_id = #{tenantId} AND deleted = 0 AND auxiliary_code = #{auxiliaryCode}")
    AuxiliaryAccounting getByCode(@Param("tenantId") Long tenantId, @Param("auxiliaryCode") String auxiliaryCode);
    
    @Select("SELECT * FROM finance_auxiliary_accounting WHERE tenant_id = #{tenantId} AND deleted = 0 AND auxiliary_type = #{auxiliaryType} AND ref_id = #{refId}")
    AuxiliaryAccounting getByTypeAndRefId(@Param("tenantId") Long tenantId, @Param("auxiliaryType") Integer auxiliaryType, @Param("refId") Long refId);
}