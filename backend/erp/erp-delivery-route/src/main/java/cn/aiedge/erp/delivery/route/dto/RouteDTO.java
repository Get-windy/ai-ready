package cn.aiedge.erp.delivery.route.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 线路主数据 DTO（资料 → 配送管理 → 线路）
 *
 * 列表列（对标实测 6 列）：线路编号 / 线路名称 / 线路类型 / 物流公司 / 配送区域 / 备注
 * 表单字段：线路类型*(自配/物流) / 线路编号* / 线路名称* / 物流公司 / 配送区域子表 / 备注
 */
@Data
public class RouteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 线路编号 */
    private String routeCode;

    /** 线路名称 */
    private String routeName;

    /** 线路类型-自配 0-否 1-是 */
    private Integer routeSelf;

    /** 线路类型-物流 0-否 1-是 */
    private Integer routeLogistics;

    /** 物流公司 */
    private String expressName;

    /** 显示状态 ENABLED-已启用 DISABLED-已停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 配送区域子表 */
    private List<RouteAreaDTO> areas;

    /** 派生：线路类型文本（自配 / 物流 / 自配、物流） */
    private String routeTypeText;

    /** 派生：配送区域文本（多区域以「、」拼接，列表列展示） */
    private String areaText;

    /** 派生：状态文本 */
    private String statusText;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
