package cn.aiedge.dms.dispatch.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线路-配送员绑定（智能调度「区域分包 AREA」策略，表 dms_route_rider）
 *
 * <p>红线：线路是**线路档案**（`erp_route`，资料 → 配送管理 → 线路），DMS 侧只读引用、不复制主数据；
 * 本表只存绑定关系与编号/名称快照，供派单时按 `dms_task.route_id` 判定「谁负责这条线路」。</p>
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_route_rider")
public class DmsRouteRider {

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    public static final int STATUS_DISABLED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 线路档案ID（erp_route.id） */
    private Long routeId;

    /** 线路编号快照（erp_route.route_code） */
    private String routeCode;

    /** 线路名称快照（erp_route.route_name） */
    private String routeName;

    /** 配送员ID（dms_rider.id） */
    private Long riderId;

    /** 配送员姓名快照（dms_rider.real_name） */
    private String riderName;

    /** 优先级：数值越小越优先（0=默认） */
    private Integer priority;

    /** 状态：1-启用 0-停用（停用不参与区域分包命中） */
    private Integer status;

    /** 备注（允许清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
