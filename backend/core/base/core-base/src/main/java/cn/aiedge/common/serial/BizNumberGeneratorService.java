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
 * 格式: {prefix}{YYYYMMDD}{seq(seqLength位补零)}
 *
 * 示例:
 *   SN20260624001, SN20260624002, SN20260624003 ...
 *   B202606240001, B202606240002 ...
 *   SO202606240001 ...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizNumberGeneratorService {

    private final BizNumberSequenceMapper sequenceMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成下一个递增编号
     *
     * @param bizType 业务类型（如 SN, BN, INV, SO, PO 等）
     * @return 完整的递增编号字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public String nextNumber(String bizType) {
        String today = LocalDate.now().format(DATE_FMT);

        BizNumberSequence seq = sequenceMapper.selectForUpdate(bizType);
        if (seq == null) {
            throw new IllegalArgumentException("未配置编号序列: bizType=" + bizType + "，请先在 biz_number_sequence 表中初始化");
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
            throw new RuntimeException("编号序列已超出每日最大容量: bizType=" + bizType + ", maxSeq=" + seq.getMaxSeq());
        }

        String seqStr = String.format("%0" + seq.getSeqLength() + "d", seq.getCurrentSeq());
        String number = seq.getPrefix() + today + seqStr;
        log.debug("生成编号: bizType={}, number={}", bizType, number);
        return number;
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
     * 便捷方法：生成销售单号（SO前缀，4位序列）
     */
    public String nextSaleOrderNo() {
        return nextNumber("SO");
    }

    /**
     * 便捷方法：生成采购单号（PO前缀，4位序列）
     */
    public String nextPurchaseOrderNo() {
        return nextNumber("PO");
    }

    /**
     * 便捷方法：生成往来单位编号（P前缀，4位序列）
     */
    public String nextPartnerCode() {
        return nextNumber("PARTNER");
    }
}
