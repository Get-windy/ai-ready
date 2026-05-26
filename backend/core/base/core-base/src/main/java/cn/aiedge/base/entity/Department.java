package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_department")
public class Department extends BaseEntity {

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 祖级路径（如：0,1,2）
     */
    private String ancestors;

    /**
     * 部门负责人ID
     */
    private Long leaderId;

    /**
     * 部门负责人姓名
     */
    private String leaderName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
