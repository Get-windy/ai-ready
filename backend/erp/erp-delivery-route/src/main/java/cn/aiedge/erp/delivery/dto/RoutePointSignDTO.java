package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;

/** 点位签收入参（多点签收，逐点独立） */
@Data
public class RoutePointSignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标状态：IN_ROUTE-在途 ARRIVED-已到达 DELIVERED-已送达 FAILED-配送失败 */
    private String status;

    /** 签收人（DELIVERED 时填写） */
    private String signee;

    /** 配送失败原因（FAILED 时填写） */
    private String failReason;

    private String remark;
}
