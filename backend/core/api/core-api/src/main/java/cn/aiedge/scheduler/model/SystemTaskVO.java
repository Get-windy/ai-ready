package cn.aiedge.scheduler.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统任务台账行（设置 → 账套操作 → 系统任务，菜单 70561）。
 *
 * <p>数据源：{@code scheduled_task_log}（一行 = 一次执行，见迁移 {@code V11.401.0}）。
 * 字段口径**逐条对齐 ql361 对标页的 9 列**（实测 2026-09-18，
 * {@code tool-results/ql361/设置-live/系统任务.json} 的 {@code columnConfig.cols}）：
 * 任务ID · 任务类型 · 任务名称 · 任务状态 · 创建人 · 任务创建时间 · 任务开始时间 ·
 * 任务结束时间 · 结果查看。</p>
 *
 * <p>⚠️ 与开发文档 §10.1-17「返回字段恰含 9 项」的差异（**已登记，供复核**）：
 * 本 VO 多一个 {@link #errorMsg}（失败原因）。依据是同一文档 §4-7 / §12-P1
 * 「失败态必须有可读原因，否则用户无从排查」—— 它不是列，只供前端在状态上挂 tooltip。
 * 若复核要求严格 9 项，删掉该字段即可（列表与下载端点均不依赖它）。</p>
 *
 * <p>⚠️ 所有 Long 由 {@code JacksonConfig} 全局序列化为**字符串**（JS 大整数精度），
 * 前端一律按字符串处理，禁止 {@code Number(id)}。</p>
 */
@Data
public class SystemTaskVO {

    /** 任务ID（对标显示原始数字 ID，如 17248262；本系统为 {@code scheduled_task_log.id}） */
    private Long taskId;

    /** 任务类型（对标取值如「导出」；定时任务执行记录为「定时任务」） */
    private String taskType;

    /** 任务名称（对标取值如「报表导出」「商品资料导出」） */
    private String taskName;

    /**
     * 任务状态（**统一字符码**，闭集）：{@code pending} 待执行 · {@code running} 执行中 ·
     * {@code success} 成功 · {@code failed} 失败 · {@code partial} 部分成功 · {@code unknown} 未知。
     *
     * <p>库内原值为 {@code RUNNING/SUCCESS/FAILURE}（{@code TaskExecutor} 写入），
     * 由 {@code SystemTaskController#normalizeStatus} 归一 —— 前端只做中文映射，
     * **不再出现「筛选一套口径、展示另一套口径」**（开发文档 §5.1 / §9.2-P1）。</p>
     */
    private String status;

    /** 创建人**姓名**（对标为真实姓名；发起方未上报时为 null，页面显示「-」） */
    private String createdByName;

    /** 任务创建时间（对标第 6 列；必非空） */
    private LocalDateTime createTime;

    /** 任务开始时间（对标第 7 列；pending 时可空） */
    private LocalDateTime startTime;

    /** 任务结束时间（对标第 8 列；未结束时为空，耗时 = end − start） */
    private LocalDateTime endTime;

    /**
     * 结果查看（对标第 9 列，行内「下载处理结果」的目标）。
     *
     * <p>值为**本站鉴权下载地址**（相对路径，如 {@code /set/system-task/18/result}）——
     * 只有「执行成功/部分成功 + 确有产物 + 未过期」的行才有值，其余为 {@code null}
     * （前端据此置灰）。**绝不是永久公开直链**（开发文档 §7.3 / §10.1-28）。</p>
     */
    private String resultUrl;

    /**
     * 【非列字段】失败原因（库内 {@code error_message}），仅当状态为 failed 时有值，供前端 tooltip。
     */
    private String errorMsg;
}
