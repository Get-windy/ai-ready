package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;

/** 配送路线单点位入参 */
@Data
public class RoutePointSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 顺序号；留空按数组顺序 1..n 自动编号 */
    private Integer pointOrder;

    /** 来源单据类型：SO-销售订单 / OUT-销售出库单（为空视为手工点位） */
    private String sourceType;

    /** 来源单据ID */
    private Long sourceId;

    private String orderId;

    private String orderNo;

    private String customerName;

    private String customerPhone;

    /** 地址（必填） */
    private String address;

    private String latitude;

    private String longitude;

    private String remark;
}
