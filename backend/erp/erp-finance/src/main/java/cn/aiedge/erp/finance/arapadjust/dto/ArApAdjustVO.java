package cn.aiedge.erp.finance.arapadjust.dto;

import cn.aiedge.erp.finance.arapadjust.entity.ArApAdjustItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应收应付调整详情 VO（含科目明细 items）
 */
@Data
public class ArApAdjustVO {

    private Long id;

    private Long tenantId;

    private String docNo;

    private LocalDate docDate;

    private Integer direction;

    private String directionName;

    private String partnerType;

    private Long partnerId;

    private String partnerCode;

    private String partnerName;

    private Long handlerId;

    private String handlerName;

    private Long deptId;

    private String deptName;

    private BigDecimal totalAmount;

    private Integer status;

    private String creatorName;

    private Long bookkeeperId;

    private String bookkeeperName;

    private LocalDateTime bookkeepingTime;

    private String summary;

    private String attachment;

    private String remark;

    private Integer printCount;

    private Integer redFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 科目明细 */
    private List<ArApAdjustItem> items;
}
