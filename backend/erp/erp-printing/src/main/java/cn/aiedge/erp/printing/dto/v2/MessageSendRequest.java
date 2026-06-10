package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class MessageSendRequest {

    @NotNull
    private Long screenshotId;

    @NotBlank
    private String targetType;   // wechat / email / sms

    @NotBlank
    private String targetId;     // openid / email / phone

    private String messageType;  // print_confirm / print_notify

    private Map<String, Object> extraParams;
}
