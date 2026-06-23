package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.FinanceTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务交易Mapper接口
 */
@Mapper
public interface FinanceTransactionMapper extends BaseMapper<FinanceTransaction> {

    /**
     * 根据交易编号查询
     */
    @Select("SELECT * FROM finance_transaction WHERE transaction_no = #{transactionNo} AND deleted_flag = 0")
    FinanceTransaction findByTransactionNo(String transactionNo);

    /**
     * 根据交易类型查询
     */
    @Select("SELECT * FROM finance_transaction WHERE transaction_type = #{transactionType} AND deleted_flag = 0")
    List<FinanceTransaction> findByTransactionType(Integer transactionType);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM finance_transaction WHERE status = #{status} AND deleted_flag = 0")
    List<FinanceTransaction> findByStatus(Integer status);

    /**
     * 根据交易时间范围查询
     */
    @Select("SELECT * FROM finance_transaction WHERE transaction_time BETWEEN #{startDate} AND #{endDate} AND deleted_flag = 0 ORDER BY transaction_time DESC")
    List<FinanceTransaction> findByTransactionTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 根据业务类型和业务ID查询
     */
    @Select("SELECT * FROM finance_transaction WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND deleted_flag = 0")
    List<FinanceTransaction> findByBizTypeAndBizId(@Param("bizType") Integer bizType, @Param("bizId") Long bizId);

    /**
     * 根据租户ID查询
     */
    @Select("SELECT * FROM finance_transaction WHERE tenant_id = #{tenantId} AND deleted_flag = 0")
    List<FinanceTransaction> findByTenantId(String tenantId);

    /**
     * 查询最近的交易记录
     */
    @Select("SELECT * FROM finance_transaction WHERE deleted_flag = #{deletedFlag} ORDER BY transaction_time DESC")
    List<FinanceTransaction> findByDeletedFlagOrderByTransactionTimeDesc(Integer deletedFlag);
}
