package cn.aiedge.hr.ref;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 部门只读引用
 *
 * <p>部门主数据归**系统管理域**（`sys_department` + `DepartmentController`），HR 只读取不写入。
 * 此实体仅为在 hr-base 模块内解析 `dept_id → dept_name`、以及给部门下拉/左树提供数据源，
 * **不得**在本模块对它做增删改。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_department")
public class HrDepartmentRef {

    @TableId
    private Long id;

    private Long tenantId;

    private Long parentId;

    private String deptCode;

    private String deptName;

    private String ancestors;

    private Long leaderId;

    private String leaderName;

    private String phone;

    private String email;

    private Integer sort;

    /** 状态（0-禁用 1-启用） */
    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;
}
