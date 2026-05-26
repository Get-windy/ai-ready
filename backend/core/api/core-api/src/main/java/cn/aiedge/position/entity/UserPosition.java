package cn.aiedge.position.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户岗位关联实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_user_position")
public class UserPosition {

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 岗位ID
     */
    private Long positionId;

    /**
     * 是否主岗位：0-否，1-是
     */
    private Integer isPrimary;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
