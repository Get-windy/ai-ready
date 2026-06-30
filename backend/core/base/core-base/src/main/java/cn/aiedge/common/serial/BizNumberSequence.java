package cn.aiedge.common.serial;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务编号序列表
 * 用于按业务类型生成递增编号，替代随机编号生成
 */
@Data
@TableName("biz_number_sequence")
public class BizNumberSequence {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型（SN=序列号, BN=批次号, INV=发票, SO=销售单, PO=采购单 等） */
    private String bizType;

    /** 语言区域（zh_CN=中文, en_US=英文 等） */
    private String locale;

    /** 当前日期 YYYYMMDD，每日重置 */
    private String seqDate;

    /** 当前序列值 */
    private Integer currentSeq;

    /** 最大序列值 */
    private Integer maxSeq;

    /** 编号前缀 */
    private String prefix;

    /** 序列号补零长度 */
    private Integer seqLength;

    private LocalDateTime updateTime;
}
