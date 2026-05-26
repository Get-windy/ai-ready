package cn.aiedge.agent.config;

import cn.aiedge.agent.security.AgentAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AgentWebConfig implements WebMvcConfigurer {

    private final AgentAuthInterceptor agentAuthInterceptor;

    public AgentWebConfig(AgentAuthInterceptor agentAuthInterceptor) {
        this.agentAuthInterceptor = agentAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agentAuthInterceptor)
                .addPathPatterns("/api/agent/invoke/**");
    }
}
