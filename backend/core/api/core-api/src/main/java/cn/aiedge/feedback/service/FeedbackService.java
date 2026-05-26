package cn.aiedge.feedback.service;

import cn.aiedge.feedback.model.Feedback;
import cn.aiedge.feedback.model.FeedbackCategory;
import cn.aiedge.feedback.model.FeedbackReply;

import java.util.List;
import java.util.Map;

/**
 * 用户反馈服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface FeedbackService {

    // ==================== 反馈管理 ====================

    /**
     * 提交反馈
     */
    Feedback submitFeedback(Feedback feedback);

    /**
     * 获取反馈详情
     */
    Feedback getFeedback(String feedbackId);

    /**
     * 更新反馈
     */
    Feedback updateFeedback(String feedbackId, Feedback feedback);

    /**
     * 删除反馈
     */
    boolean deleteFeedback(String feedbackId);

    /**
     * 查询反馈列表
     */
    List<Feedback> listFeedbacks(Map<String, Object> params, int page, int pageSize);

    /**
     * 统计反馈数量
     */
    long countFeedbacks(Map<String, Object> params);

    // ==================== 反馈处理 ====================

    /**
     * 审核反馈
     */
    boolean reviewFeedback(String feedbackId, Long reviewerId, boolean approved, String comment);

    /**
     * 分配反馈
     */
    boolean assignFeedback(String feedbackId, Long handlerId, Long operatorId);

    /**
     * 处理反馈
     */
    boolean processFeedback(String feedbackId, Long handlerId, String solution);

    /**
     * 回复反馈
     */
    FeedbackReply replyFeedback(String feedbackId, FeedbackReply reply);

    /**
     * 关闭反馈
     */
    boolean closeFeedback(String feedbackId, Long operatorId, String reason);

    /**
     * 拒绝反馈
     */
    boolean rejectFeedback(String feedbackId, Long operatorId, String reason);

    /**
     * 重新打开反馈
     */
    boolean reopenFeedback(String feedbackId, Long operatorId, String reason);

    // ==================== 回复管理 ====================

    /**
     * 获取回复列表
     */
    List<FeedbackReply> getReplies(String feedbackId);

    /**
     * 删除回复
     */
    boolean deleteReply(String replyId);

    // ==================== 分类管理 ====================

    /**
     * 创建分类
     */
    FeedbackCategory createCategory(FeedbackCategory category);

    /**
     * 更新分类
     */
    FeedbackCategory updateCategory(String categoryId, FeedbackCategory category);

    /**
     * 删除分类
     */
    boolean deleteCategory(String categoryId);

    /**
     * 获取分类列表
     */
    List<FeedbackCategory> listCategories();

    /**
     * 获取分类详情
     */
    FeedbackCategory getCategory(String categoryId);

    // ==================== 自动分配 ====================

    /**
     * 自动分类反馈
     */
    String autoCategorize(String title, String content);

    /**
     * 自动分配优先级
     */
    String autoAssignPriority(Feedback feedback);

    /**
     * 自动分配处理人
     */
    Long autoAssignHandler(String category);

    // ==================== 统计查询 ====================

    /**
     * 获取反馈统计
     */
    Map<String, Object> getStatistics(Long tenantId);

    /**
     * 按状态统计
     */
    Map<String, Long> countByStatus(Long tenantId);

    /**
     * 按类型统计
     */
    Map<String, Long> countByType(Long tenantId);

    /**
     * 按分类统计
     */
    Map<String, Long> countByCategory(Long tenantId);

    /**
     * 获取我的待处理反馈
     */
    List<Feedback> getMyPendingFeedbacks(Long handlerId, int page, int pageSize);

    /**
     * 获取我提交的反馈
     */
    List<Feedback> getMySubmittedFeedbacks(Long submitterId, int page, int pageSize);

    /**
     * 提交满意度评价
     */
    boolean submitSatisfaction(String feedbackId, Integer score, String comment);
}
