package cn.aiedge.finance.repository;

import cn.aiedge.finance.entity.Invoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 发票数据访问层
 */
@Mapper
public interface InvoiceRepository extends BaseMapper<Invoice> {
    
    /**
     * 分页查询发票
     */
    IPage<Invoice> selectInvoicePage(Page<Invoice> page, @Param("param") Map<String, Object> param);
    
    /**
     * 根据发票号码查询
     */
    @Select("SELECT * FROM finance_invoice WHERE invoice_no = #{invoiceNo} AND deleted = 0 LIMIT 1")
    Invoice selectByInvoiceNo(@Param("invoiceNo") String invoiceNo);
    
    /**
     * 查询待认证发票列表
     */
    List<Invoice> selectPendingCertifyList(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);
    
    /**
     * 统计发票金额
     */
    Map<String, BigDecimal> statisticsAmount(@Param("param") Map<String, Object> param);
    
    /**
     * 查询即将到期发票（认证期限360天）
     */
    List<Invoice> selectExpiringInvoices(@Param("days") Integer days);
    
    /**
     * 按月统计发票
     */
    List<Map<String, Object>> statisticsByMonth(@Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("direction") Integer direction);
}
