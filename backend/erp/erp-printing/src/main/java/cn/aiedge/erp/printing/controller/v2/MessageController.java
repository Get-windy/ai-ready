package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.MessageSendRequest;
import cn.aiedge.erp.printing.message.MessageSender;
import cn.hutool.extra.spring.SpringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "V2-消息发送（预留）", description = "将截图发送至微信等通讯工具，当前为预留接口")
@RestController
@RequestMapping("/api/v2/print/messages")
@RequiredArgsConstructor
public class MessageController {

    @Operation(summary = "发送消息（功能即将开放）")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Object>> send(@Valid @RequestBody MessageSendRequest request) {
        // 使用占位实现，后续对接具体渠道
        MessageSender placeholder = SpringUtil.getBean("placeholderMessageSender", MessageSender.class);

        MessageSender.SendRequest sendReq = new MessageSender.SendRequest();
        sendReq.setScreenshotId(request.getScreenshotId());
        sendReq.setTargetType(request.getTargetType());
        sendReq.setTargetId(request.getTargetId());
        sendReq.setMessageType(request.getMessageType());
        sendReq.setExtraParams(request.getExtraParams());

        MessageSender.SendResult result = placeholder.send(sendReq);

        Map<String, Object> data = new HashMap<>();
        data.put("success", result.isSuccess());
        data.put("messageId", result.getMessageId());
        data.put("errorMessage", result.getErrorMessage());

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
