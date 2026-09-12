package cn.aiedge.erp.delivery.route.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 线路主数据查询参数（对标固定查询项，无页面配置弹窗）
 *
 * 对标查询区：线路编号/线路名称/配送区域（输入框） + 显示状态（默认已启用） + 线路类型（默认全部）
 */
@Data
public class RouteQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 筛选条件：线路编号 / 线路名称 / 配送区域（模糊匹配） */
    private String keyword;

    /** 显示状态 ENABLED / DISABLED；不传且 showDisabled!=1 时默认只看启用 */
    private String status;

    /** 线路类型 SELF-自配 LOGISTICS-物流；不传为全部 */
    private String routeType;

    /** 显示停用 1=同时显示停用数据 */
    private Integer showDisabled;

    private Integer pageNum;

    private Integer pageSize;
}
