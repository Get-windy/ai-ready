package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrintClientRegisterRequest {

    @NotBlank(message = "客户端名称不能为空")
    @Size(max = 200)
    private String clientName;

    private String clientVersion;
}
