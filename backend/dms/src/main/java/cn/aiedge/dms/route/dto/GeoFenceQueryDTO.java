package cn.aiedge.dms.route.dto;

import lombok.Data;

/**
 * 电子围栏查询条件
 */
@Data
public class GeoFenceQueryDTO {

    /** 围栏编码 / 名称 / 绑定对象 模糊搜索 */
    private String keyword;

    /** CIRCLE / POLYGON */
    private String fenceType;

    /** ENABLED / DISABLED */
    private String status;

    /** ROUTE / CHANNEL / WAREHOUSE / OTHER */
    private String bizType;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
