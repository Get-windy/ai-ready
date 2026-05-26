package cn.aiedge.agent.registry;

import cn.aiedge.agent.annotation.AgentCapability;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CapabilityRegistry {

    private final ApplicationContext applicationContext;

    private final Map<String, CapabilityDefinition> capabilities = new ConcurrentHashMap<>();

    public CapabilityRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        scanAndRegister();
    }

    private void scanAndRegister() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(AgentCapability.class);
        for (Object bean : beans.values()) {
            Class<?> clazz = bean.getClass();
            AgentCapability classAnnotation = clazz.getAnnotation(AgentCapability.class);
            for (Method method : clazz.getDeclaredMethods()) {
                AgentCapability methodAnnotation = method.getAnnotation(AgentCapability.class);
                if (methodAnnotation != null) {
                    registerCapability(methodAnnotation, bean, method);
                }
            }
            if (classAnnotation != null && clazz.getDeclaredMethods().length == 0) {
                registerCapability(classAnnotation, bean, null);
            }
        }

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            for (Method method : bean.getClass().getDeclaredMethods()) {
                AgentCapability annotation = method.getAnnotation(AgentCapability.class);
                if (annotation != null && !beans.containsKey(bean)) {
                    registerCapability(annotation, bean, method);
                }
            }
        }
    }

    private void registerCapability(AgentCapability annotation, Object bean, Method method) {
        String name = annotation.name().isEmpty() ? annotation.code() : annotation.name();
        CapabilityDefinition definition = CapabilityDefinition.builder()
                .code(annotation.code())
                .name(name)
                .description(annotation.description())
                .tags(Arrays.asList(annotation.tags()))
                .timeout(annotation.timeout())
                .requireAuth(annotation.requireAuth())
                .version(annotation.version())
                .targetBean(bean)
                .targetMethod(method)
                .enabled(true)
                .registeredAt(System.currentTimeMillis())
                .build();
        capabilities.put(annotation.code(), definition);
    }

    public CapabilityDefinition getCapability(String code) {
        return capabilities.get(code);
    }

    public boolean hasCapability(String code) {
        CapabilityDefinition def = capabilities.get(code);
        return def != null && def.isEnabled();
    }

    public Collection<CapabilityDefinition> listCapabilities() {
        return capabilities.values();
    }

    public List<CapabilityDefinition> searchByTag(String tag) {
        return capabilities.values().stream()
                .filter(c -> c.getTags().contains(tag))
                .toList();
    }

    public int getCapabilityCount() {
        return capabilities.size();
    }
}
