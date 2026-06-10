package cn.aiedge.feedback.controller;

import cn.aiedge.feedback.model.Feedback;
import cn.aiedge.feedback.model.FeedbackCategory;
import cn.aiedge.feedback.model.FeedbackReply;
import cn.aiedge.feedback.service.FeedbackService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户反馈控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "用户反馈", description = "用户反馈收集和处理功能")
public class FeedbackController {

    private final FeedbackService feedbackService;

    // ==================== 反馈提交 ====================

    @PostMapping("/submit")
    @Operation(summary = "提交反馈")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @RequestBody Feedback feedback,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        log.info("提交反馈: title={}, type={}", feedback.getTitle(), feedback.getType());

        feedback.setSubmitterId(userId);
        feedback.setTenantId(tenantId);

        Feedback saved = feedbackService.submitFeedback(feedback);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("feedbackId", saved.getFeedbackId());
        result.put("status", saved.getStatus());
        result.put("priority", saved.getPriority());
        result.put("category", saved.getCategory());
        result.put("message", "反馈提交成功");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/submit/feishu")
    @Operation(summary = "从飞书提交反馈")
    public ResponseEntity<Map<String, Object>> submitFromFeishu(
            @RequestBody FeishuFeedbackRequest request) {

        log.info("飞书反馈: user={}, title={}", request.getUserName(), request.getTitle());

        Feedback feedback = new Feedback();
        feedback.setTitle(request.getTitle());
        feedback.setContent(request.getContent());
        feedback.setType(request.getType());
        feedback.setSource("feishu");
        feedback.setSubmitterName(request.getUserName());
        feedback.setContactInfo(request.getUserId());

        Feedback saved = feedbackService.submitFeedback(feedback);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("feedbackId", saved.getFeedbackId());
        result.put("message", "反馈已接收，感谢您的反馈！");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/submit/email")
    @Operation(summary = "从邮件提交反馈")
    public ResponseEntity<Map<String, Object>> submitFromEmail(
            @RequestBody EmailFeedbackRequest request) {

        log.info("邮件反馈: from={}, subject={}", request.getFrom(), request.getSubject());

        Feedback feedback = new Feedback();
        feedback.setTitle(request.getSubject());
        feedback.setContent(request.getBody());
        feedback.setType("other");
        feedback.setSource("email");
        feedback.setEmail(request.getFrom());
        feedback.setSubmitterName(request.getFromName());

        Feedback saved = feedbackService.submitFeedback(feedback);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("feedbackId", saved.getFeedbackId());
        result.put("message", "邮件反馈已接收");

        return ResponseEntity.ok(result);
    }

    // ==================== 反馈查询 ====================

    @GetMapping("/list")
    @Operation(summary = "获取反馈列表")
    public ResponseEntity<Map<String, Object>> listFeedbacks(
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "类型") @RequestParam(required = false) String type,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "优先级") @RequestParam(required = false) String priority,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        Map<String, Object> params = new HashMap<>();
        if (status != null) params.put("status", status);
        if (type != null) params.put("type", type);
        if (category != null) params.put("category", category);
        if (priority != null) params.put("priority", priority);
        params.put("tenantId", tenantId);

        List<Feedback> feedbacks = feedbackService.listFeedbacks(params, page, pageSize);
        long total = feedbackService.countFeedbacks(params);

        Map<String, Object> result = new HashMap<>();
        result.put("feedbacks", feedbacks);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{feedbackId}")
    @Operation(summary = "获取反馈详情")
    public ResponseEntity<Feedback> getFeedback(@PathVariable String feedbackId) {
        Feedback feedback = feedbackService.getFeedback(feedbackId);
        if (feedback == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/{feedbackId}/replies")
    @Operation(summary = "获取反馈回复")
    public ResponseEntity<List<FeedbackReply>> getReplies(@PathVariable String feedbackId) {
        List<FeedbackReply> replies = feedbackService.getReplies(feedbackId);
        return ResponseEntity.ok(replies);
    }

    // ==================== 我的反馈 ====================

    @GetMapping("/my/submitted")
    @Operation(summary = "我提交的反馈")
    public ResponseEntity<Map<String, Object>> getMySubmittedFeedbacks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        List<Feedback> feedbacks = feedbackService.getMySubmittedFeedbacks(userId, page, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("feedbacks", feedbacks);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/my/pending")
    @Operation(summary = "我的待处理反馈")
    public ResponseEntity<Map<String, Object>> getMyPendingFeedbacks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        List<Feedback> feedbacks = feedbackService.getMyPendingFeedbacks(userId, page, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("feedbacks", feedbacks);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/by-status")
    @Operation(summary = "按状态统计")
    public ResponseEntity<Map<String, Long>> countByStatus(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        Map<String, Long> result = feedbackService.countByStatus(tenantId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/by-type")
    @Operation(summary = "按类型统计")
    public ResponseEntity<Map<String, Long>> countByType(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        Map<String, Long> result = feedbackService.countByType(tenantId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/by-category")
    @Operation(summary = "按分类统计")
    public ResponseEntity<Map<String, Long>> countByCategory(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        Map<String, Long> result = feedbackService.countByCategory(tenantId);
        return ResponseEntity.ok(result);
    }

    // ==================== 请求DTO ====================

    public static class FeishuFeedbackRequest {
        private String userId;
        private String userName;
        private String title;
        private String content;
        private String type;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class EmailFeedbackRequest {
        private String from;
        private String fromName;
        private String subject;
        private String body;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    public static class ReviewRequest {
        private boolean approved;
        private String comment;

        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class AssignRequest {
        private Long handlerId;

        public Long getHandlerId() { return handlerId; }
        public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }
    }

    public static class ProcessRequest {
        private String solution;

        public String getSolution() { return solution; }
        public void setSolution(String solution) { this.solution = solution; }
    }

    public static class ReplyRequest {
        private String content;
        private String replierName;
        private Boolean internal;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getReplierName() { return replierName; }
        public void setReplierName(String replierName) { this.replierName = replierName; }
        public Boolean getInternal() { return internal; }
        public void setInternal(Boolean internal) { this.internal = internal; }
    }

    public static class CloseRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ReopenRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class SatisfactionRequest {
        private Integer score;
        private String comment;

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
