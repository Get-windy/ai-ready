package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 辅助核算项目Mapper接口
 */
@Mapper
public interface FinanceAuxiliaryItemMapper extends BaseMapper<FinanceAuxiliaryItem> {

    /**
     * 根据项目编码查询
     */
    @Select("SELECT * FROM finance_auxiliary_item WHERE item_code = #{itemCode} AND auxiliary_type_id = #{auxiliaryTypeId} AND tenant_id = #{tenantId} AND deleted_flag = 0")
    Optional<FinanceAuxiliaryItem> findByItemCode(String itemCode, Long auxiliaryTypeId, Long tenantId);

    /**
     * 查询指定类型下的所有项目
     */
    @Select("SELECT * FROM finance_auxiliary_item WHERE auxiliary_type_id = #{auxiliaryTypeId} AND tenant_id = #{tenantId} AND deleted_flag = 0 ORDER BY sort ASC")
    List<FinanceAuxiliaryItem> findByTypeId(Long auxiliaryTypeId, Long tenantId);
}
