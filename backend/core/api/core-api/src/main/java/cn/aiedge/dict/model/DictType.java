package cn.aiedge.dict.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_dict_type")
public class DictType {

    /**
     * 字典类型ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 字典类型编码（唯一）
     */
    private String dictCode;

    /**
     * 字典类型名称
     */
    private String dictName;

    /**
     * 字典类型描述
     */
    private String description;

    /**
     * 父字典ID（用于树形结构，0表示顶级）
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态：ENABLED, DISABLED
     */
    private String status;

    /**
     * 是否内置：Y-是（系统内置不可删除）, N-否
     */
    private String isBuiltIn;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者ID
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 状态枚举
     */
    public enum Status {
        ENABLED("enabled", "启用"),
        DISABLED("disabled", "禁用");

        private final String code;
        private final String desc;

        Status(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() { return code; }
        public String getDesc() { return desc; }
    }
}
