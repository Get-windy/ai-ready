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
 * 线路主数据（资料 → 配送管理 → 线路，表 erp_route）
 *
 * ⚠️ 与《配送路线单》执行单据（erp_delivery_route）严格区分：
 *   本表只存线路档案本身（类型 / 编号 / 名称 / 物流公司 / 状态 / 备注），不存配送员、进度、起讫时间。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_route")
public class RouteMaster {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 线路编号 */
    private String routeCode;

    /** 线路名称 */
    private String routeName;

    /** 线路类型-自配 0-否 1-是 */
    private Integer routeSelf;

    /** 线路类型-物流 0-否 1-是 */
    private Integer routeLogistics;

    /** 物流公司（线路类型含物流时维护） */
    private String expressName;

    /** 显示状态 ENABLED-已启用 DISABLED-已停用 */
    private String status;

    /** 备注 */
    private String remark;

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
