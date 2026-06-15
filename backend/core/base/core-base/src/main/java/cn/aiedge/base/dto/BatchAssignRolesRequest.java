package cn.aiedge.base.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量分配角色请求
 */
@Data
public class BatchAssignRolesRequest {
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;

    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
