package cn.aiedge.erp.fixedasset.service.integration;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 固定资产模块记账集成服务
 * 直接调用财务模块的 {@link BusinessAccountingService} 创建折旧凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetAccountingService {

    private final BusinessAccountingService businessAccountingService;

    /**
     * 计提折旧时创建凭证
     * Dr. Expense (管理费用/制造费用等 - 折旧费)
     * Cr. Accumulated Depreciation (累计折旧)
     *
     * @param assetId      资产ID
     * @param assetCode    资产编号
     * @param assetName    资产名称
     * @param amount       折旧金额
     * @param fiscalYear   会计年度
     * @param fiscalPeriod 会计期间
     * @return 凭证编号
     */
    public String postDepreciationVoucher(Long assetId, String assetCode, String assetName,
                                          BigDecimal amount, Integer fiscalYear, Integer fiscalPeriod) {
        log.info("创建折旧凭证: assetCode={}, amount={}, period={}-{}",
                assetCode, amount, fiscalYear, fiscalPeriod);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("FIXED_ASSET_DEPRECIATION");
        voucherRequest.setSourceId(assetId);
        voucherRequest.setSourceNo(assetCode);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("计提折旧 - " + assetName + "(" + assetCode + ")");
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. Expense (折旧费), Cr. Accumulated Depreciation
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("计提折旧费用");
        debitEntry.setSubjectCode("6604");  // 折旧费
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("累计折旧");
        creditEntry.setSubjectCode("1602");  // 累计折旧
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO result = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        String voucherNo = result.getVoucherNo() != null ? result.getVoucherNo() : "";
        log.info("折旧凭证创建成功: voucherNo={}", voucherNo);
        return voucherNo;
    }
}
