package cn.aiedge.erp.delivery.route.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 线路配送区域子表 DTO（对标表单子表：配送区域类型 gptype + 配送区域编码 gpcode）
 */
@Data
public class RouteAreaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 所属线路ID */
    private Long routeId;

    /** 配送区域类型 PROVINCE-省 CITY-市 DISTRICT-区县 */
    private String areaType;

    /** 配送区域编码（行政区划编码） */
    private String areaCode;

    /** 配送区域名称 */
    private String areaName;

    private Integer sortNo;
}
