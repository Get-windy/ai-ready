package cn.aiedge.erp.finance.arapadjust.dto;

import cn.aiedge.erp.finance.arapadjust.entity.ArApAdjustItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 应收应付调整保存请求(DTO)
 * 保存草稿/更新时传输头字段 + 科目明细。
 */
@Data
public class ArApAdjustSaveDTO {

    private Long id;

    private String docNo;

    private LocalDate docDate;

    /** 调整方向 1应收增加 2应收减少 3应付增加 4应付减少 */
    private Integer direction;

    /** 结算单位ID */
    private Long partnerId;

    private String partnerCode;

    private String partnerType;

    private String partnerName;

    /** 经手人 */
    private Long handlerId;

    private String handlerName;

    private Long deptId;

    private String deptName;

    /** 制单人 */
    private String creatorName;

    private String summary;

    private String remark;

    /** 本单金额 = Σ明细金额（保存时后端重算） */
    private BigDecimal totalAmount;

    /** 科目明细 */
    private List<ArApAdjustItem> items;
}
