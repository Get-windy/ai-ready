package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 打印客户端注册请求 DTO
 */
@Data
public class PrintClientRegisterDTO {

    @NotBlank(message = "客户端名称不能为空")
    private String clientName;

    /** 客户端版本号 */
    private String clientVersion;
}
