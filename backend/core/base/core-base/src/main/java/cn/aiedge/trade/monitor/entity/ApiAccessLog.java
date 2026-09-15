package cn.aiedge.trade.monitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口调用日志（网关级监控底表）
 *
 * <p>表复用 V8.14.0 建成的 `api_access_log`（原注释「用于限流监控」，此前无任何读写），
 * V11.360.0 补齐 api_name / request_id / direction / status / error_code / error_msg 六列，
 * **不另建 api_call_log**（同义表双轨见《配送模块 README》§6 开发原则）。</p>
 *
 * <p>写入方（三类方向，见 {@link ApiCallDirection}）：
 * <ul>
 *   <li>IN —— `/api/open/**` 入站拦截器（{@code ApiCallLogInterceptor}）；</li>
 *   <li>OUT —— 我方调用外部渠道（`InventorySyncServiceImpl` 推送库存/改价）；</li>
 *   <li>SANDBOX —— 页面「快速联调」自检（同一个表即联调历史，不再单建历史表）。</li>
 * </ul>
 *
 * <p>安全口径：只落**脱敏后的查询串**与响应摘要，**不落请求/响应体**（避免客户隐私与凭据落库）。</p>
 */
@Data
@TableName("api_access_log")
public class ApiAccessLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 渠道编码（入站路径变量/查询参数，出站为适配器渠道编码；无渠道的接口为空） */
    private String channelCode;

    /** 接口路径（入站为 URI 模板，如 /api/open/inventory/query） */
    private String apiPath;

    /** 接口名称（可读名，如「库存查询」） */
    private String apiName;

    /** 请求方法 */
    private String requestMethod;

    /** 脱敏后的查询串（敏感键掩码，超长截断；不落请求体） */
    private String requestParams;

    /** 调用链请求号（入站回写响应头 X-Request-Id） */
    private String requestId;

    /** 方向: IN / OUT / SANDBOX */
    private String direction;

    /** 调用状态: SUCCESS / FAIL */
    private String status;

    /** HTTP/业务响应码 */
    private Integer responseCode;

    /** 耗时(ms) */
    private Integer responseTime;

    /** 错误码 */
    private String errorCode;

    /** 错误摘要（截断，不含凭据） */
    private String errorMsg;

    /** 调用方 IP（入站） */
    private String ipAddress;

    /** 调用方 UA（截断） */
    private String userAgent;

    /** 调用时间 */
    private LocalDateTime accessTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
