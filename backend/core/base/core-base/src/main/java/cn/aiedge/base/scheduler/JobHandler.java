package cn.aiedge.base.scheduler;

/**
 * 定时任务处理器（白名单 SPI）
 *
 * <p>定时任务的执行目标由**已注册的处理器**决定，任务行只存 {@code job_key}：</p>
 * <ul>
 *   <li>实现类必须是 Spring Bean —— 可正常注入 Mapper/Service 等依赖（旧实现用
 *       {@code Class.forName(executeClass).getDeclaredConstructor().newInstance()} 反射实例化普通类，
 *       <b>无法注入任何依赖</b>，导致定时任务只能跑「无参构造 + 单 String 参数」的空壳方法）；</li>
 *   <li>{@link #key()} 全局唯一；重复注册启动即失败（由调度侧注册表校验）；</li>
 *   <li>SPI 放在 core-base：业务模块（dms / erp-* 等）只依赖 core-base，
 *       实现类写在各自模块内，由 core-api 的调度器统一收集。</li>
 * </ul>
 *
 * <p><b>安全口径</b>：旧实现把「类名 + 方法名」当参数执行（来自请求体）→ 任意登录用户可让后台线程
 * 反射调用类路径上任意「无参构造 + 单 String 参数」方法，属越权 + 危险设计；改为白名单后
 * <b>只有本接口的实现类可被调度</b>，任务行里的类名/方法名不再被读取。</p>
 *
 * @author AI-Ready Team
 */
public interface JobHandler {

    /**
     * 处理器唯一键（存 {@code scheduled_task.job_key}，建议「域.模块.动作」形式，如 {@code dms.dispatch.escalateOverdue}）
     */
    String key();

    /**
     * 处理器名称（中文，前端下拉展示）
     */
    String name();

    /**
     * 执行（正常返回即成功；抛异常即失败，由调度器记录失败原因与堆栈）
     *
     * @param params 任务行上的 {@code execute_params} 原文（可空，约定为 JSON）
     * @return 执行摘要（写入执行日志的「执行结果」列，如「命中 3 单，重派 1 单」/「跳过：另一实例正在执行」）；
     *         返回 {@code null}/空串时日志记为「执行成功」
     */
    String execute(String params) throws Exception;
}
