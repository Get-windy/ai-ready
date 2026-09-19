package cn.aiedge.audit.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 操作日志列表 / 详情视图对象（设置 → 账套操作 → 操作日志 → 系统日志 Tab）
 *
 * <p>为什么需要 VO（不直接返回 {@code SysOperLog} 实体）：
 * <ol>
 *   <li><b>裁剪大字段</b>：实体含 {@code request_params} / {@code response_result} 两个 text 列，
 *       列表接口返回整实体会把大字段一并拖出（性能 + 敏感面）。列表只回必要列，
 *       两个大字段仅在详情接口（{@code GET /api/log/{id}}）填充。</li>
 *   <li><b>补「姓名」列</b>：对标 ql361 的台账是「操作员（账号）+ 姓名（真实姓名）」两列，
 *       而 {@code sys_oper_log} 只落 `username`，真实姓名在 {@code sys_user.real_name}，
 *       故列表侧批量补齐 {@link #realName}（不新增数据库字段）。</li>
 * </ol>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class SysOperLogVO {

    /** 日志ID（雪花ID，前端按字符串处理） */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 操作员（登录账号，对应 sys_oper_log.username） */
    private String username;

    /** 姓名（来自 sys_user.real_name，空则回退 nickname；两侧都空时为 null） */
    private String realName;

    /** 模块（对标「内容」列的组成部分） */
    private String module;

    /** 操作类型（CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT/OTHER 及业务自定义值） */
    private String action;

    /** 操作时间 */
    private LocalDateTime operTime;

    /** 操作IP */
    private String operIp;

    /** 操作地点 */
    private String operLocation;

    /** 耗时(ms) */
    private Long costTime;

    /** 状态(0-成功 1-失败) */
    private Integer status;

    /** 错误信息（失败时） */
    private String errorMsg;

    /** 请求方式（GET/POST/...） */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 后端方法签名 */
    private String method;

    /** 变更对比数据（JSON，记录修改前后差异；对齐 SAP SCU3 的 before/after 语义） */
    private String diffData;

    /**
     * 请求参数（text 大字段）——【仅详情接口填充】，列表接口恒为 null
     * ⚠️ 可能含密码 / token 等敏感内容，展示前必须脱敏（见前端详情弹窗注释）
     */
    private String requestParams;

    /**
     * 响应结果（text 大字段）——【仅详情接口填充】，列表接口恒为 null
     * ⚠️ 同上
     */
    private String responseResult;
}
