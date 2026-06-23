package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.Payable;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 应付账款Mapper接口
 */
@Mapper
public interface PayableMapper extends BaseMapper<Payable> {

    /**
     * 根据供应商ID查询
     */
    @Select("SELECT * FROM finance_payable WHERE supplier_id = #{supplierId} AND deleted_flag = 0")
    List<Payable> findBySupplierId(String supplierId);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM finance_payable WHERE status = #{status} AND deleted_flag = 0")
    List<Payable> findByStatus(String status);

    /**
     * 查询到期日之前的记录
     */
    @Select("SELECT * FROM finance_payable WHERE due_date < #{dueDate} AND deleted_flag = 0")
    List<Payable> findByDueDateBefore(@Param("dueDate") LocalDate dueDate);

    /**
     * 根据来源业务类型和ID查询
     */
    @Select("SELECT * FROM finance_payable WHERE source_type = #{sourceType} AND source_id = #{sourceId} AND deleted_flag = 0")
    Optional<Payable> findBySourceTypeAndSourceId(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);
}
