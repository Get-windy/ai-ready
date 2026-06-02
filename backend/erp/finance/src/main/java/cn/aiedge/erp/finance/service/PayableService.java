package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.PayableDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 应付账款Service接口
 */
public interface PayableService {

    /**
     * 创建应付账款
     */
    PayableDTO create(PayableDTO dto);

    /**
     * 根据ID获取应付账款
     */
    PayableDTO getById(Long id);

    /**
     * 分页查询应付账款
     */
    IPage<PayableDTO> list(String supplierId, String status, Page<PayableDTO> page);

    /**
     * 获取账龄分析
     */
    List<Map<String, Object>> getAgingAnalysis();

    /**
     * 核销（部分/全额核销）
     */
    PayableDTO writeOff(Long id, BigDecimal amount);
}
