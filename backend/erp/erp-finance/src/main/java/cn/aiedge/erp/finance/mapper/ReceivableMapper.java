package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.Receivable;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 应收账款Mapper接口
 */
@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    /**
     * 根据客户ID查询
     */
    @Select("SELECT * FROM finance_receivable WHERE customer_id = #{customerId} AND deleted_flag = 0")
    List<Receivable> findByCustomerId(String customerId);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM finance_receivable WHERE status = #{status} AND deleted_flag = 0")
    List<Receivable> findByStatus(String status);

    /**
     * 查询到期日之前的记录
     */
    @Select("SELECT * FROM finance_receivable WHERE due_date < #{dueDate} AND deleted_flag = 0")
    List<Receivable> findByDueDateBefore(@Param("dueDate") LocalDate dueDate);

    /**
     * 根据来源业务类型和ID查询
     */
    @Select("SELECT * FROM finance_receivable WHERE source_type = #{sourceType} AND source_id = #{sourceId} AND deleted_flag = 0")
    Optional<Receivable> findBySourceTypeAndSourceId(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);

    /**
     * 根据来源业务类型和ID列表查询
     */
    @Select("SELECT * FROM finance_receivable WHERE source_type = #{sourceType} AND source_id IN (${sourceIds}) AND deleted_flag = 0")
    List<Receivable> findBySourceTypeAndSourceIdIn(@Param("sourceType") String sourceType, @Param("sourceIds") List<Long> sourceIds);
}
