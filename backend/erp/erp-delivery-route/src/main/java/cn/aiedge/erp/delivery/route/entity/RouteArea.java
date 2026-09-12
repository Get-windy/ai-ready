package cn.aiedge.erp.delivery.route.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 线路配送区域子表（表 erp_route_area）
 *
 * 对标 ql361 线路表单子表：配送区域类型 gptype（单选）+ 配送区域编码 gpcode。
 * 落地口径：区域类型 = 行政区划级别（PROVINCE/CITY/DISTRICT），区域编码 = sys_region.code。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_route_area")
public class RouteArea {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 所属线路 erp_route.id */
    private Long routeId;

    /** 配送区域类型 PROVINCE-省 CITY-市 DISTRICT-区县 */
    private String areaType;

    /** 配送区域编码（行政区划编码） */
    private String areaCode;

    /** 配送区域名称（行政区划名称快照） */
    private String areaName;

    private Integer sortNo;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
