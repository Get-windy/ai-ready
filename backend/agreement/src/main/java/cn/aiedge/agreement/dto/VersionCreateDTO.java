package cn.aiedge.agreement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起变更：从当前生效版本复制出新的 DRAFT 版本。
 *
 * <p>{@code changeReason} 必填 —— 变更是要留痕并被双方看到的，"为什么改"
 * 不能留空，否则日后无法解释"这一版是谁为了什么改的"。</p>
 */
@Data
public class VersionCreateDTO {

    @NotBlank(message = "请填写变更原因：这一版改了什么、为什么要改")
    private String changeReason;
}
