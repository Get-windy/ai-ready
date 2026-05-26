package cn.aiedge.feedback.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.feedback.model.Feedback;
import cn.aiedge.feedback.model.FeedbackCategory;
import cn.aiedge.feedback.model.FeedbackReply;
import cn.aiedge.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户反馈服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final CacheService cacheService;

    // 缓存Key前缀
    private static final String FEEDBACK_KEY = "feedback:item:";
    private static final String FEEDBACK_LIST_KEY = "feedback:list:";
    private static final String FEEDBACK_REPLY_KEY = "feedback:reply:";
    private static final String CATEGORY_KEY = "feedback:category:";
    private static final String PENDING_KEY = "feedback:pending:";
    private static final String MY_FEEDBACK_KEY = "feedback:my:";

    // 内置分类
    private static final Map<String, FeedbackCategory> BUILTIN_CATEGORIES = new LinkedHashMap<>();

    static {
        initBuiltinCategories();
    }

    private static void initBuiltinCategories() {
        // UI问题
        FeedbackCategory ui = new FeedbackCategory();
        ui.setCategoryId("cat_ui");
        ui.setName("UI问题");
        ui.setCode("UI");
        ui.setDescription("界面显示、样式、交互问题");
        ui.setDefaultPriority("medium");
        ui.setSortOrder(1);
        ui.setEnabled(true);
        BUILTIN_CATEGORIES.put("cat_ui", ui);

        // 功能问题
        FeedbackCategory func = new FeedbackCategory();
        func.setCategoryId("cat_function");
        func.setName("功能问题");
        func.setCode("FUNCTION");
        func.setDescription("功能缺陷、逻辑错误");
        func.setDefaultPriority("high");
        func.setSortOrder(2);
        func.setEnabled(true);
        BUILTIN_CATEGORIES.put("cat_function", func);

        // 性能问题
        FeedbackCategory perf = new FeedbackCategory();
        perf.setCategoryId("cat_performance");
        perf.setName("性能问题");
        perf.setCode("PERFORMANCE");
        perf.setDescription("响应慢、卡顿、资源占用高");
        perf.setDefaultPriority("high");
        perf.setSortOrder(3);
        perf.setEnabled(true);
        BUILTIN_CATEGORIES.put("cat_performance", perf);

        // 安全问题
        FeedbackCategory security = new FeedbackCategory();
        security.setCategoryId("cat_security");
        security.setName("安全问题");
        security.setCode("SECURITY");
        security.setDescription("安全漏洞、权限问题");
        security.setDefaultPriority("urgent");
        security.setSortOrder(4);
        security.setEnabled(true);
        BUILTIN_CATEGORIES.put("cat_security", security);

        // 体验优化
        FeedbackCategory exp = new FeedbackCategory();
        exp.setCategoryId("cat_experience");
        exp.setName("体验优化");
        exp.setCode("EXPERIENCE");
        exp.setDescription("用户体验改进建议");
        exp.setDefaultPriority("low");
        exp.setSortOrder(5);
        exp.setEnabled(true);
        BUILTIN_CATEGORIES.put("cat_experience", exp);

        // 其他
        FeedbackCategory other = new FeedbackCategory();
        other.setCategoryId("cat_other");
        other.setName("其他");
        other.setCode("OTHER");
        other.setDescription("其他类型反馈");
        other.setDefaultPriority("medium");
        other.setSortOrder(6);
        other.setEnabled(true);
        BUILTIN_CATEGORIES.put("cat_other", other);
    }

    // ==================== 反馈管理 ====================

    @Override
    public Feedback submitFeedback(Feedback feedback) {
        // 生成ID
        if (feedback.getFeedbackId() == null) {
            feedback.setFeedbackId(UUID.randomUUID().toString());
        }

        // 设置初始状态
        feedback.setStatus("pending");
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());

        // 自动分类
        if (feedback.getCategory() == null) {
            feedback.setCategory(autoCategorize(feedback.getTitle(), feedback.getContent()));
        }

        // 自动分配优先级
        if (feedback.getPriority() == null) {
            feedback.setPriority(autoAssignPriority(feedback));
        }

        // 保存反馈
        cacheService.set(FEEDBACK_KEY + feedback.getFeedbackId(), feedback);

        // 添加到列表
        cacheService.lPush(FEEDBACK_LIST_KEY + "all", feedback.getFeedbackId());
        if (feedback.getTenantId() != null) {
            cacheService.lPush(FEEDBACK_LIST_KEY + "tenant:" + feedback.getTenantId(), feedback.getFeedbackId());
        }

        // 添加到我的反馈
        if (feedback.getSubmitterId() != null) {
            cacheService.lPush(MY_FEEDBACK_KEY + feedback.getSubmitterId(), feedback.getFeedbackId());
        }

        log.info("提交反馈: feedbackId={}, type={}, category={}", 
                feedback.getFeedbackId(), feedback.getType(), feedback.getCategory());

        return feedback;
    }

    @Override
    public Feedback getFeedback(String feedbackId) {
        return cacheService.get(FEEDBACK_KEY + feedbackId, Feedback.class);
    }

    @Override
    public Feedback updateFeedback(String feedbackId, Feedback feedback) {
        Feedback existing = getFeedback(feedbackId);
        if (existing == null) {
            return null;
        }

        // 更新字段
        if (feedback.getTitle() != null) existing.setTitle(feedback.getTitle());
        if (feedback.getContent() != null) existing.setContent(feedback.getContent());
        if (feedback.getPriority() != null) existing.setPriority(feedback.getPriority());
        if (feedback.getCategory() != null) existing.setCategory(feedback.getCategory());
        if (feedback.getTags() != null) existing.setTags(feedback.getTags());

        existing.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, existing);

        return existing;
    }

    @Override
    public boolean deleteFeedback(String feedbackId) {
        cacheService.delete(FEEDBACK_KEY + feedbackId);
        return true;
    }

    @Override
    public List<Feedback> listFeedbacks(Map<String, Object> params, int page, int pageSize) {
        // 简化实现：返回所有反馈
        List<Object> ids = cacheService.lRange(FEEDBACK_LIST_KEY + "all", 0, -1);
        if (ids == null) return new ArrayList<>();

        return ids.stream()
                .map(id -> getFeedback(id.toString()))
                .filter(Objects::nonNull)
                .filter(f -> filterFeedback(f, params))
                .skip((page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    @Override
    public long countFeedbacks(Map<String, Object> params) {
        List<Object> ids = cacheService.lRange(FEEDBACK_LIST_KEY + "all", 0, -1);
        if (ids == null) return 0;

        return ids.stream()
                .map(id -> getFeedback(id.toString()))
                .filter(Objects::nonNull)
                .filter(f -> filterFeedback(f, params))
                .count();
    }

    private boolean filterFeedback(Feedback feedback, Map<String, Object> params) {
        if (params == null) return true;

        // 按状态过滤
        if (params.containsKey("status")) {
            String status = (String) params.get("status");
            if (!status.equals(feedback.getStatus())) return false;
        }

        // 按类型过滤
        if (params.containsKey("type")) {
            String type = (String) params.get("type");
            if (!type.equals(feedback.getType())) return false;
        }

        // 按分类过滤
        if (params.containsKey("category")) {
            String category = (String) params.get("category");
            if (!category.equals(feedback.getCategory())) return false;
        }

        // 按优先级过滤
        if (params.containsKey("priority")) {
            String priority = (String) params.get("priority");
            if (!priority.equals(feedback.getPriority())) return false;
        }

        // 按租户过滤
        if (params.containsKey("tenantId")) {
            Long tenantId = (Long) params.get("tenantId");
            if (!tenantId.equals(feedback.getTenantId())) return false;
        }

        return true;
    }

    // ==================== 反馈处理 ====================

    @Override
    public boolean reviewFeedback(String feedbackId, Long reviewerId, boolean approved, String comment) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null || !"pending".equals(feedback.getStatus())) {
            return false;
        }

        if (approved) {
            feedback.setStatus("review");
            // 自动分配处理人
            Long handlerId = autoAssignHandler(feedback.getCategory());
            if (handlerId != null) {
                feedback.setHandlerId(handlerId);
                cacheService.lPush(PENDING_KEY + handlerId, feedbackId);
            }
        } else {
            feedback.setStatus("rejected");
        }

        feedback.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, feedback);

        // 添加审核回复
        FeedbackReply reply = new FeedbackReply();
        reply.setReplyId(UUID.randomUUID().toString());
        reply.setFeedbackId(feedbackId);
        reply.setContent(approved ? "审核通过" + (comment != null ? ": " + comment : "") : "审核拒绝: " + comment);
        reply.setReplierType("system");
        reply.setReplierId(reviewerId);
        reply.setInternal(true);
        reply.setCreateTime(LocalDateTime.now());
        addReply(reply);

        log.info("审核反馈: feedbackId={}, approved={}", feedbackId, approved);
        return true;
    }

    @Override
    public boolean assignFeedback(String feedbackId, Long handlerId, Long operatorId) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null) return false;

        feedback.setHandlerId(handlerId);
        feedback.setStatus("assigned");
        feedback.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, feedback);

        cacheService.lPush(PENDING_KEY + handlerId, feedbackId);

        log.info("分配反馈: feedbackId={}, handlerId={}", feedbackId, handlerId);
        return true;
    }

    @Override
    public boolean processFeedback(String feedbackId, Long handlerId, String solution) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null || !feedback.getHandlerId().equals(handlerId)) {
            return false;
        }

        feedback.setStatus("resolved");
        feedback.setSolution(solution);
        feedback.setHandleTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, feedback);

        log.info("处理反馈: feedbackId={}, handlerId={}", feedbackId, handlerId);
        return true;
    }

    @Override
    public FeedbackReply replyFeedback(String feedbackId, FeedbackReply reply) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null) return null;

        if (reply.getReplyId() == null) {
            reply.setReplyId(UUID.randomUUID().toString());
        }
        reply.setFeedbackId(feedbackId);
        reply.setCreateTime(LocalDateTime.now());

        addReply(reply);

        // 更新反馈状态
        if (!"closed".equals(feedback.getStatus())) {
            feedback.setStatus("processing");
            feedback.setUpdateTime(LocalDateTime.now());
            cacheService.set(FEEDBACK_KEY + feedbackId, feedback);
        }

        log.info("回复反馈: feedbackId={}, replierId={}", feedbackId, reply.getReplierId());
        return reply;
    }

    @Override
    public boolean closeFeedback(String feedbackId, Long operatorId, String reason) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null) return false;

        feedback.setStatus("closed");
        feedback.setCloseTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, feedback);

        log.info("关闭反馈: feedbackId={}, reason={}", feedbackId, reason);
        return true;
    }

    @Override
    public boolean rejectFeedback(String feedbackId, Long operatorId, String reason) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null) return false;

        feedback.setStatus("rejected");
        feedback.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, feedback);

        log.info("拒绝反馈: feedbackId={}, reason={}", feedbackId, reason);
        return true;
    }

    @Override
    public boolean reopenFeedback(String feedbackId, Long operatorId, String reason) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null) return false;

        feedback.setStatus("processing");
        feedback.setCloseTime(null);
        feedback.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, feedback);

        log.info("重新打开反馈: feedbackId={}, reason={}", feedbackId, reason);
        return true;
    }

    // ==================== 回复管理 ====================

    @Override
    public List<FeedbackReply> getReplies(String feedbackId) {
        String key = FEEDBACK_REPLY_KEY + feedbackId;
        List<Object> replies = cacheService.lRange(key, 0, -1);
        if (replies == null) return new ArrayList<>();

        return replies.stream()
                .map(r -> (FeedbackReply) r)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteReply(String replyId) {
        // 简化实现
        return true;
    }

    private void addReply(FeedbackReply reply) {
        String key = FEEDBACK_REPLY_KEY + reply.getFeedbackId();
        cacheService.lPush(key, reply);
        cacheService.expire(key, 365, TimeUnit.DAYS);
    }

    // ==================== 分类管理 ====================

    @Override
    public FeedbackCategory createCategory(FeedbackCategory category) {
        if (category.getCategoryId() == null) {
            category.setCategoryId(UUID.randomUUID().toString());
        }
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());

        cacheService.set(CATEGORY_KEY + category.getCategoryId(), category);
        return category;
    }

    @Override
    public FeedbackCategory updateCategory(String categoryId, FeedbackCategory category) {
        FeedbackCategory existing = getCategory(categoryId);
        if (existing == null) return null;

        if (category.getName() != null) existing.setName(category.getName());
        if (category.getDescription() != null) existing.setDescription(category.getDescription());
        if (category.getDefaultPriority() != null) existing.setDefaultPriority(category.getDefaultPriority());
        if (category.getDefaultHandlerId() != null) existing.setDefaultHandlerId(category.getDefaultHandlerId());
        if (category.getSortOrder() != null) existing.setSortOrder(category.getSortOrder());
        if (category.getEnabled() != null) existing.setEnabled(category.getEnabled());

        existing.setUpdateTime(LocalDateTime.now());
        cacheService.set(CATEGORY_KEY + categoryId, existing);

        return existing;
    }

    @Override
    public boolean deleteCategory(String categoryId) {
        if (BUILTIN_CATEGORIES.containsKey(categoryId)) {
            log.warn("不能删除内置分类: {}", categoryId);
            return false;
        }
        cacheService.delete(CATEGORY_KEY + categoryId);
        return true;
    }

    @Override
    public List<FeedbackCategory> listCategories() {
        List<FeedbackCategory> result = new ArrayList<>(BUILTIN_CATEGORIES.values());
        // 可以添加从缓存读取自定义分类的逻辑
        return result.stream()
                .sorted(Comparator.comparing(FeedbackCategory::getSortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public FeedbackCategory getCategory(String categoryId) {
        FeedbackCategory builtin = BUILTIN_CATEGORIES.get(categoryId);
        if (builtin != null) return builtin;
        return cacheService.get(CATEGORY_KEY + categoryId, FeedbackCategory.class);
    }

    // ==================== 自动分配 ====================

    @Override
    public String autoCategorize(String title, String content) {
        String text = (title + " " + content).toLowerCase();

        // 关键词匹配
        if (containsAny(text, "bug", "错误", "异常", "崩溃", "报错", "失败")) {
            return "cat_function";
        }
        if (containsAny(text, "慢", "卡顿", "性能", "加载", "响应")) {
            return "cat_performance";
        }
        if (containsAny(text, "安全", "漏洞", "权限", "越权", "注入")) {
            return "cat_security";
        }
        if (containsAny(text, "ui", "界面", "样式", "显示", "布局", "颜色")) {
            return "cat_ui";
        }
        if (containsAny(text, "建议", "优化", "改进", "希望", "能否")) {
            return "cat_experience";
        }

        return "cat_other";
    }

    @Override
    public String autoAssignPriority(Feedback feedback) {
        String text = (feedback.getTitle() + " " + feedback.getContent()).toLowerCase();

        // 紧急关键词
        if (containsAny(text, "紧急", "urgent", "严重", "critical", "崩溃", "无法使用")) {
            return "urgent";
        }

        // 高优先级
        if (containsAny(text, "重要", "high", "安全", "漏洞", "数据丢失")) {
            return "high";
        }

        // 根据分类设置默认优先级
        FeedbackCategory category = getCategory(feedback.getCategory());
        if (category != null && category.getDefaultPriority() != null) {
            return category.getDefaultPriority();
        }

        return "medium";
    }

    @Override
    public Long autoAssignHandler(String category) {
        FeedbackCategory cat = getCategory(category);
        if (cat != null && cat.getDefaultHandlerId() != null) {
            return cat.getDefaultHandlerId();
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    // ==================== 统计查询 ====================

    @Override
    public Map<String, Object> getStatistics(Long tenantId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", countFeedbacks(Map.of("tenantId", tenantId)));
        stats.put("byStatus", countByStatus(tenantId));
        stats.put("byType", countByType(tenantId));
        stats.put("byCategory", countByCategory(tenantId));

        return stats;
    }

    @Override
    public Map<String, Long> countByStatus(Long tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        Map<String, Long> result = new HashMap<>();
        String[] statuses = {"pending", "review", "assigned", "processing", "resolved", "closed", "rejected"};

        for (String status : statuses) {
            params.put("status", status);
            result.put(status, countFeedbacks(params));
        }

        return result;
    }

    @Override
    public Map<String, Long> countByType(Long tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        Map<String, Long> result = new HashMap<>();
        String[] types = {"bug", "feature", "improvement", "complaint", "other"};

        for (String type : types) {
            params.put("type", type);
            result.put(type, countFeedbacks(params));
        }

        return result;
    }

    @Override
    public Map<String, Long> countByCategory(Long tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        Map<String, Long> result = new HashMap<>();
        for (FeedbackCategory cat : listCategories()) {
            params.put("category", cat.getCategoryId());
            result.put(cat.getName(), countFeedbacks(params));
        }


        return result;
    }

    @Override
    public List<Feedback> getMyPendingFeedbacks(Long handlerId, int page, int pageSize) {
        List<Object> feedbackIds = cacheService.lRange(PENDING_KEY + handlerId, (page - 1) * pageSize, page * pageSize - 1);
        if (feedbackIds == null) return new ArrayList<>();

        return feedbackIds.stream()
                .map(id -> getFeedback(id.toString()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Feedback> getMySubmittedFeedbacks(Long submitterId, int page, int pageSize) {
        List<Object> feedbackIds = cacheService.lRange(MY_FEEDBACK_KEY + submitterId, (page - 1) * pageSize, page * pageSize - 1);
        if (feedbackIds == null) return new ArrayList<>();

        return feedbackIds.stream()
                .map(id -> getFeedback(id.toString()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean submitSatisfaction(String feedbackId, Integer score, String comment) {
        Feedback feedback = getFeedback(feedbackId);
        if (feedback == null) return false;

        feedback.setSatisfaction(score);
        feedback.setSatisfactionComment(comment);
        feedback.setUpdateTime(LocalDateTime.now());
        cacheService.set(FEEDBACK_KEY + feedbackId, feedback);

        log.info("提交满意度: feedbackId={}, score={}", feedbackId, score);
        return true;
    }
}
