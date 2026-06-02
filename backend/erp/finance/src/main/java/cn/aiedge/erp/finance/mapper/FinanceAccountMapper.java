package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务账户Mapper接口
 */
@Mapper
public interface FinanceAccountMapper extends BaseMapper<FinanceAccount> {

    /**
     * 根据账户名称模糊查询
     */
    @Select("SELECT * FROM finance_account WHERE account_name LIKE #{accountName} AND deleted_flag = 0")
    List<FinanceAccount> findByAccountNameLike(String accountName);

    /**
     * 根据账户类型查询
     */
    @Select("SELECT * FROM finance_account WHERE account_type = #{accountType} AND deleted_flag = 0")
    List<FinanceAccount> findByAccountType(Integer accountType);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM finance_account WHERE status = #{status} AND deleted_flag = 0")
    List<FinanceAccount> findByStatus(Integer status);

    /**
     * 根据租户ID查询
     */
    @Select("SELECT * FROM finance_account WHERE tenant_id = #{tenantId} AND deleted_flag = 0")
    List<FinanceAccount> findByTenantId(String tenantId);

    /**
     * 查询所有启用且未删除的账户
     */
    @Select("SELECT * FROM finance_account WHERE status = #{status} AND deleted_flag = #{deletedFlag}")
    List<FinanceAccount> findByStatusAndDeletedFlag(Integer status, Integer deletedFlag);
}
