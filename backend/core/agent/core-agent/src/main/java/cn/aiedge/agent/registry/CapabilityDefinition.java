package cn.aiedge.agent.registry;

import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;

@Data
@Builder
public class CapabilityDefinition {

    private String code;

    private String name;

    private String description;

    private List<String> tags;

    private int timeout;

    private boolean requireAuth;

    private String version;

    private Object targetBean;

    private Method targetMethod;

    private boolean enabled;

    private long registeredAt;
}
