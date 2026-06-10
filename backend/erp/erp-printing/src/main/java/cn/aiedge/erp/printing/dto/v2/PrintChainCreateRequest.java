package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PrintChainCreateRequest {

    @NotBlank(message = "pageCode 不能为空")
    private String pageCode;

    @NotBlank(message = "链路名称不能为空")
    @Size(max = 200)
    private String chainName;

    @Size(max = 500)
    private String description;

    private Integer sortOrder;

    @Valid
    @Size(min = 1, max = 10, message = "链路步骤数量必须在 1~10 之间")
    private List<ChainItemRequest> items;
}
