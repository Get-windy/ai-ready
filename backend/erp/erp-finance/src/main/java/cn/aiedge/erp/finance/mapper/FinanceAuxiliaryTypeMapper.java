package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 辅助核算类型Mapper接口
 */
@Mapper
public interface FinanceAuxiliaryTypeMapper extends BaseMapper<FinanceAuxiliaryType> {

    /**
     * 根据类型编码查询
     */
    @Select("SELECT * FROM finance_auxiliary_type WHERE type_code = #{typeCode} AND tenant_id = #{tenantId} AND deleted_flag = 0")
    Optional<FinanceAuxiliaryType> findByTypeCode(String typeCode, Long tenantId);
}
