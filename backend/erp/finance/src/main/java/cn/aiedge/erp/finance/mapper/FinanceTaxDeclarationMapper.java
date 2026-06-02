package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.FinanceTaxDeclaration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 税务申报Mapper接口
 */
@Mapper
public interface FinanceTaxDeclarationMapper extends BaseMapper<FinanceTaxDeclaration> {

    /**
     * 根据申报编号查询
     */
    @Select("SELECT * FROM finance_tax_declaration WHERE declaration_no = #{declarationNo} AND deleted_flag = 0")
    FinanceTaxDeclaration findByDeclarationNo(String declarationNo);

    /**
     * 根据税种查询
     */
    @Select("SELECT * FROM finance_tax_declaration WHERE tax_type = #{taxType} AND deleted_flag = 0")
    List<FinanceTaxDeclaration> findByTaxType(Integer taxType);

    /**
     * 根据申报期间查询
     */
    @Select("SELECT * FROM finance_tax_declaration WHERE declaration_period = #{declarationPeriod} AND deleted_flag = 0")
    List<FinanceTaxDeclaration> findByDeclarationPeriod(String declarationPeriod);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM finance_tax_declaration WHERE status = #{status} AND deleted_flag = 0")
    List<FinanceTaxDeclaration> findByStatus(Integer status);

    /**
     * 根据纳税人识别号查询
     */
    @Select("SELECT * FROM finance_tax_declaration WHERE taxpayer_id = #{taxpayerId} AND deleted_flag = 0")
    List<FinanceTaxDeclaration> findByTaxpayerId(String taxpayerId);

    /**
     * 根据租户ID查询
     */
    @Select("SELECT * FROM finance_tax_declaration WHERE tenant_id = #{tenantId} AND deleted_flag = 0")
    List<FinanceTaxDeclaration> findByTenantId(String tenantId);

    /**
     * 查询最近的申报记录
     */
    @Select("SELECT * FROM finance_tax_declaration WHERE deleted_flag = #{deletedFlag} ORDER BY declaration_date DESC")
    List<FinanceTaxDeclaration> findByDeletedFlagOrderByDeclarationDateDesc(Integer deletedFlag);
}
