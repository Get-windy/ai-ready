package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 行政区划（省 / 市 / 区县，国家统计局口径）
 * <p>
 * 系统级公共数据：无 tenant_id，已在 MyBatisPlusConfig.IGNORE_TENANT_TABLES 中登记。
 * 供往来单位（客户 / 供应商 / 物流 / 其他）表单的「所在地区」三级联动使用。
 * </p>
 */
@Getter
@Setter
@TableName("sys_region")
public class SysRegion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 行政区划代码：省 2 位 / 市 4 位 / 区县 6 位 */
    private String code;

    /** 名称 */
    private String name;

    /** 上级代码（省级为 null） */
    private String parentCode;

    /** 层级：1 省 2 市 3 区县 */
    private Integer regionLevel;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    /** 子节点（树形返回时装配，非表字段） */
    @TableField(exist = false)
    private List<SysRegion> children;
}
