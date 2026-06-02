package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.VoucherDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 记账凭证Service接口
 */
public interface VoucherService {

    /**
     * 创建凭证
     */
    VoucherDTO create(VoucherDTO dto);

    /**
     * 根据ID获取凭证
     */
    VoucherDTO getById(Long id);

    /**
     * 根据凭证编号获取凭证
     */
    VoucherDTO getByVoucherNo(String voucherNo);

    /**
     * 分页查询凭证
     */
    IPage<VoucherDTO> list(Integer fiscalYear, Integer fiscalPeriod, String status, Page<VoucherDTO> page);

    /**
     * 审核凭证（draft/audited -> audited）
     */
    VoucherDTO audit(Long id, String auditor);

    /**
     * 过账（audited -> posted，同时更新分类账）
     */
    VoucherDTO post(Long id, String poster);

    /**
     * 冲销（创建红字冲销凭证）
     */
    VoucherDTO reverse(Long id, String reason);

    /**
     * 生成凭证编号（格式：YYYYMM-XXXX）
     */
    String generateVoucherNo(Integer fiscalYear, Integer fiscalPeriod);
}
