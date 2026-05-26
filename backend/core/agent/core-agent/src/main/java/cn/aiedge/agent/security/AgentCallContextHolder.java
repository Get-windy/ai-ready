package cn.aiedge.agent.security;

import cn.aiedge.agent.protocol.AgentCallContext;

public final class AgentCallContextHolder {

    private AgentCallContextHolder() {}

    private static final ThreadLocal<AgentCallContext> CONTEXT = new ThreadLocal<>();

    public static void set(AgentCallContext context) {
        CONTEXT.set(context);
    }

    public static AgentCallContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
