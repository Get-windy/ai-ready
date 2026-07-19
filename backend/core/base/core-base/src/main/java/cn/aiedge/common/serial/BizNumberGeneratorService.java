package cn.aiedge.common.serial;

import cn.aiedge.common.serial.mapper.BizNumberSequenceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务编号生成器
 * 使用数据库行锁实现跨进程安全的递增序列号生成
 * 支持多语言环境（中文拼音前缀/英文缩写前缀）
 * 格式: {prefix}-{YYYYMMDD}-{seq(seqLength位补零)}
 *
 * 示例:
 *   中文: XSDD-20260624-0001, CGDD-20260624-0001 ...
 *   英文: SO-20260624-0001, PO-20260624-0001 ...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizNumberGeneratorService {

    private final BizNumberSequenceMapper sequenceMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成下一个递增编号（默认使用租户ID 1和中文环境）
     *
     * @param bizType 业务类型（如 SN, BN, INV, SO, PO 等）
     * @return 完整的递增编号字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public String nextNumber(String bizType) {
        return nextNumber(bizType, 1L, "zh_CN"); // 默认使用中文环境
    }

    /**
     * 生成下一个递增编号（指定租户和语言环境）
     *
     * @param bizType 业务类型（如 SN, BN, INV, SO, PO 等）
     * @param tenantId 租户ID
     * @param locale 语言环境（如 zh_CN, en_US）
     * @return 完整的递增编号字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public String nextNumber(String bizType, Long tenantId, String locale) {
        String today = LocalDate.now().format(DATE_FMT);

        BizNumberSequence seq = sequenceMapper.selectForUpdateWithLocale(bizType, locale, tenantId);
        if (seq == null) {
            // 尝试使用默认语言环境
            seq = sequenceMapper.selectForUpdate(bizType, tenantId);
            if (seq == null) {
                throw new IllegalArgumentException("未配置编号序列: bizType=" + bizType + ", locale=" + locale + ", tenantId=" + tenantId + "，请先在 biz_number_sequence 表中初始化");
            }
        }

        // 日期变化时重置序列
        if (!today.equals(seq.getSeqDate())) {
            sequenceMapper.resetAndIncrement(seq.getId(), today);
            seq.setCurrentSeq(1);
            seq.setSeqDate(today);
        } else {
            sequenceMapper.incrementSeq(seq.getId());
            seq.setCurrentSeq(seq.getCurrentSeq() + 1);
        }

        // 检查是否超出最大序列值
        if (seq.getCurrentSeq() > seq.getMaxSeq()) {
            throw new RuntimeException("编号序列已超出每日最大容量: bizType=" + bizType + ", locale=" + locale + ", maxSeq=" + seq.getMaxSeq());
        }

        String seqStr = String.format("%0" + seq.getSeqLength() + "d", seq.getCurrentSeq());
        String number = seq.getPrefix() + "-" + today + "-" + seqStr;
        log.debug("生成编号: bizType={}, locale={}, tenantId={}, number={}", bizType, locale, tenantId, number);
        return number;
    }

    /**
     * 生成下一个递增编号（指定租户，使用中文环境）
     *
     * @param bizType 业务类型（如 SN, BN, INV, SO, PO 等）
     * @param tenantId 租户ID
     * @return 完整的递增编号字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public String nextNumber(String bizType, Long tenantId) {
        return nextNumber(bizType, tenantId, "zh_CN");
    }

    /**
     * 便捷方法：生成序列号（SN前缀，6位序列）
     */
    public String nextSerialNo() {
        return nextNumber("SN");
    }

    /**
     * 便捷方法：生成批次号（B前缀，4位序列）
     */
    public String nextBatchNo() {
        return nextNumber("BN");
    }

    /**
     * 便捷方法：生成发票号（INV前缀，5位序列）
     */
    public String nextInvoiceNo() {
        return nextNumber("INV");
    }

    /**
     * 便捷方法：生成发票申请号（APP前缀，5位序列）
     */
    public String nextApplicationNo() {
        return nextNumber("APP");
    }

    /**
     * 便捷方法：生成销售单号（XSDD前缀，4位序列）
     */
    public String nextSaleOrderNo() {
        return nextNumber("XSDD");
    }

    public String nextPreOrderNo() {
        return nextNumber("YDHD");
    }

    /**
     * 便捷方法：生成采购单号（CGDD前缀，4位序列）
     */
    public String nextPurchaseOrderNo() {
        return nextNumber("CGDD");
    }

    /**
     * 便捷方法：生成退货单号（THDD前缀，4位序列）
     */
    public String nextReturnOrderNo() {
        return nextNumber("THDD");
    }

    /**
     * 便捷方法：生成往来单位编号（DWBM前缀，4位序列）
     */
    public String nextPartnerCode() {
        return nextNumber("DWBM");
    }

    /**
     * 便捷方法：生成客户编号（KH前缀，4位序列）
     */
    public String nextCustomerCode() {
        return nextNumber("KH");
    }

    /**
     * 便捷方法：生成供应商编号（GYS前缀，4位序列）
     */
    public String nextSupplierCode() {
        return nextNumber("GYS");
    }

    /**
     * 便捷方法：生成物流公司编号（WLGS前缀，4位序列）
     */
    public String nextLogisticsCode() {
        return nextNumber("WLGS");
    }

    /**
     * 生成销售订单号（支持英文环境）
     */
    public String nextSaleOrderNo(Long tenantId, String locale) {
        return nextNumber("SO", tenantId, locale);
    }

    /**
     * 生成采购订单号（支持英文环境）
     */
    public String nextPurchaseOrderNo(Long tenantId, String locale) {
        return nextNumber("PO", tenantId, locale);
    }
}
