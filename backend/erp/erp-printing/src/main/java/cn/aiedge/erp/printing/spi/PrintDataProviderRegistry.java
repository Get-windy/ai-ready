package cn.aiedge.erp.printing.spi;

import cn.aiedge.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 打印数据装配器注册表：pageCode → 装配器。
 *
 * 由 Spring 注入全部 {@link PrintDataProvider} 实现后按 pageCode 建索引。
 * 同一个 pageCode 注册两次会**直接启动失败**——早先打印这套东西就是前后端各修各的、
 * 漂移了没人发现，宁可起不来也不要静默用错装配器。
 */
@Slf4j
@Component
public class PrintDataProviderRegistry {

    private final Map<String, PrintDataProvider> providers;

    public PrintDataProviderRegistry(List<PrintDataProvider> discovered) {
        Map<String, PrintDataProvider> map = new LinkedHashMap<>();
        for (PrintDataProvider provider : discovered) {
            String pageCode = provider.pageCode();
            if (pageCode == null || pageCode.isBlank()) {
                throw new IllegalStateException(
                        "打印数据装配器 " + provider.getClass().getName() + " 没有声明 pageCode");
            }
            PrintDataProvider previous = map.put(pageCode, provider);
            if (previous != null) {
                throw new IllegalStateException("打印数据装配器 pageCode 重复：「" + pageCode
                        + "」同时被 " + previous.getClass().getName()
                        + " 和 " + provider.getClass().getName() + " 注册");
            }
        }
        this.providers = Collections.unmodifiableMap(map);
        log.info("打印数据装配器已注册 {} 个：{}", providers.size(), providers.keySet());
    }

    /** 该页面是否后端可装配数据（否则只能走「页面自己给数据」的兼容模式） */
    public boolean supports(String pageCode) {
        return providers.containsKey(pageCode);
    }

    public PrintDataProvider require(String pageCode) {
        PrintDataProvider provider = providers.get(pageCode);
        if (provider == null) {
            throw BusinessException.badRequest("页面「" + pageCode + "」没有注册打印数据装配器，"
                    + "无法按单据编号打印。请在该业务模块实现 PrintDataProvider。"
                    + "已注册：" + providers.keySet());
        }
        return provider;
    }

    /** 已注册的页面编码，供接入检查用 */
    public Set<String> pageCodes() {
        return providers.keySet();
    }
}
