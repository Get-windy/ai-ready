package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class HeartbeatRequest {

    @NotNull
    private Long clientId;

    @NotBlank
    private String authKey;

    private String clientVersion;

    private String defaultPrinter;

    private List<String> availablePrinters;
}
