package cn.aiedge.base.log;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志查询请求
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class LogQueryRequest {

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名（模糊匹配）
     */
    private String username;

    /**
     * 模块列表
     */
    private List<String> modules;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 状态(0-成功 1-失败)
     */
    private Integer status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * IP地址（模糊匹配）
     */
    private String operIp;

    /**
     * 操作地点（模糊匹配）
     */
    private String operLocation;

    /**
     * 最小耗时(ms)
     */
    private Long minCostTime;

    /**
     * 最大耗时(ms)
     */
    private Long maxCostTime;

    /**
     * 关键词搜索（搜索请求参数、响应结果、错误信息）
     */
    private String keyword;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL（模糊匹配）
     */
    private String requestUrl;

    /**
     * 排序字段
     */
    private String sortField = "operTime";

    /**
     * 排序方向
     */
    private String sortOrder = "desc";

    /**
     * 导出格式
     */
    private String exportFormat;

    /**
     * 是否导出
     */
    private Boolean export = false;
}
