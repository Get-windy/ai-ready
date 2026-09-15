package cn.aiedge.scheduler.job;

import cn.aiedge.base.scheduler.JobHandler;
import cn.aiedge.scheduler.model.JobHandlerVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时任务处理器注册表（白名单）
 *
 * <p>Spring 启动时收集容器内全部 {@link JobHandler} 实现建索引：</p>
 * <ul>
 *   <li>{@link #get(String)}：按 {@code job_key} 取处理器 —— <b>调度器执行时唯一的解析入口</b>；</li>
 *   <li>{@link #list()}：供前端「任务处理器」下拉（避免手输类名/方法名）。</li>
 * </ul>
 *
 * <p>用 {@link ObjectProvider} 注入而非直接注入 {@code List<JobHandler>}：允许「一个处理器都没注册」的
 * 环境（如仅打包 core-api 的集成测试）正常启动，只会在执行时给出「未注册」的可读失败原因。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
public class JobHandlerRegistry {

    private final List<JobHandler> handlers;
    private Map<String, JobHandler> byKey = Map.of();

    public JobHandlerRegistry(ObjectProvider<JobHandler> handlerProvider) {
        this.handlers = handlerProvider.stream().collect(Collectors.toList());
    }

    @PostConstruct
    void init() {
        Map<String, JobHandler> map = new LinkedHashMap<>();
        List<JobHandler> sorted = new ArrayList<>(handlers);
        sorted.sort(Comparator.comparing(JobHandler::key));
        for (JobHandler handler : sorted) {
            JobHandler duplicated = map.putIfAbsent(handler.key(), handler);
            if (duplicated != null) {
                throw new IllegalStateException("定时任务处理器 key 重复: " + handler.key()
                        + "（" + duplicated.getClass().getName() + " / " + handler.getClass().getName() + "）");
            }
        }
        this.byKey = Map.copyOf(map);
        if (map.isEmpty()) {
            log.warn("未注册任何定时任务处理器（JobHandler）：定时任务执行时会因「未注册的任务处理器」失败");
        } else {
            log.info("定时任务处理器注册完成: {} 个 -> {}", map.size(), map.keySet());
        }
    }

    /**
     * 按 key 取处理器
     *
     * @return 未注册时返回 {@code null}（调用方给出可读失败原因，不抛 NPE）
     */
    public JobHandler get(String key) {
        return key == null ? null : byKey.get(key.trim());
    }

    /**
     * 已注册的处理器清单（按 key 排序）
     */
    public List<JobHandlerVO> list() {
        return byKey.values().stream()
                .map(h -> new JobHandlerVO(h.key(), h.name()))
                .collect(Collectors.toList());
    }
}
