package cn.aiedge.scheduler.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志
 *
 * <p>本表同时是「设置 → 账套操作 → 系统任务」（菜单 70561）的**异步任务执行台账**数据源
 * （2026-09-18 裁定，见迁移 {@code V11.401.0}）：一行 = 一次执行；补齐
 * {@code tenant_id / task_type / created_by_name / result_url / result_expire_time}
 * 后即可承载对标 ql361「系统任务」的 9 列契约。</p>
 *
 * <p>⚠️ 本表登记在 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}（平台级调度数据，
 * 不自动注入租户条件）→ 台账端点必须**自己**判定租户可见性，
 * 见 {@code SystemTaskController#visibleToCurrentTenant}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("scheduled_task_log")
public class ScheduledTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID（所属定时任务定义；非调度来源的异步任务为 NULL —— 迁移已放开非空约束）
     */
    private Long taskId;

    /**
     * 租户归属：0 = 平台级（定时任务自动执行，全租户可见）；非 0 = 发起方租户（仅本租户可见）
     *
     * <p>⚠️ 故意**不加** {@code FieldFill.INSERT}：全局填充只对「有登录会话」的写入生效，
     * 而定时任务由调度线程触发（无会话）→ 新行保持 DB 默认值 0（平台级），
     * 于是「开发工具 → 定时任务 → 执行日志」的既有可见性一点不变；
     * 租户发起的任务由写入方显式落自己的 tenant_id。</p>
     */
    private Long tenantId;

    /**
     * 任务类型（对标取值如「导出」；既有定时任务执行记录为「定时任务」）
     */
    private String taskType;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 发起人用户 id（{@code sys_user.id}）。
     *
     * <p>2026-09-18 补：有会话的写入（开发工具 → 定时任务 → 立即执行 / 重试）记当前登录用户 id；
     * 无会话的调度线程触发为 NULL（此时 {@link #createdByName} 记系统标识「定时调度」）。</p>
     *
     * <p>⚠️ 本列迁移 {@code V11.409.0} 才建立 → <b>既有历史行无来源可回填</b>，一律保持 NULL。</p>
     */
    private Long createBy;

    /**
     * 创建人姓名（对标显示真实姓名；发起方未上报时为 NULL，页面如实显示「-」）
     *
     * <p>写入规则（迁移 {@code V11.409.0} 起的写入侧口径）：有会话 → 当前登录用户
     * 姓名（real_name → nickname → username 依次取值）；无会话的调度触发 → 系统标识
     * 「定时调度」（**绝不留空**）。历史行（本列此前从未被写入）保持 NULL。</p>
     */
    private String createdByName;

    /**
     * 执行状态: SUCCESS/FAILURE/RUNNING
     */
    private String executeStatus;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时长(毫秒)
     */
    private Long executeTime;

    /**
     * 执行结果
     */
    private String executeResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 异常堆栈
     */
    private String exceptionStack;

    /**
     * 执行参数
     */
    private String executeParams;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 执行节点IP
     */
    private String executeNode;

    /**
     * 处理结果地址（相对 {@code storage.local.base-path} 的存储相对路径；当前无写入方，
     * 页面的「下载处理结果」优先取 {@link #executeResult} 文本）
     */
    private String resultUrl;

    /**
     * 处理结果过期时刻（过期后下载端点返回 410「处理结果已过期」）
     */
    private LocalDateTime resultExpireTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
