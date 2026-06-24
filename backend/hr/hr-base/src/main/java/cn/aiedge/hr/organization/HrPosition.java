package cn.aiedge.hr.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 岗位实体
 * 组织架构中的岗位定义
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_position")
public class HrPosition {

    /**
     * 岗位ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 岗位编码
     */
    private String positionCode;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 岗位级别（1-高管 2-中层 3-基层 4-普通）
     */
    private Integer positionLevel;

    /**
     * 岗位职责描述
     */
    private String responsibility;

    /**
     * 编制人数
     */
    private Integer quotaCount;

    /**
     * 在岗人数
     */
    private Integer currentCount;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态（0-禁用 1-启用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}