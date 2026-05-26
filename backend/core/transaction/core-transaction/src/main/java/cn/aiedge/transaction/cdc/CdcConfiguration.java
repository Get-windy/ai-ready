package cn.aiedge.transaction.cdc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Change Data Capture module
 */
@Configuration
public class CdcConfiguration {
    
    /**
     * Creates a default CDC event publisher bean
     * @return DefaultCdcEventPublisher instance
     */
    @Bean
    public CdcEventPublisher cdcEventPublisher() {
        return new DefaultCdcEventPublisher();
    }
    
    /**
     * Creates a default event serializer bean
     * @return EventSerializer instance
     */
    @Bean
    public EventSerializer eventSerializer() {
        return new EventSerializer();
    }
    
    /**
     * Creates a default CDC processor bean
     * @return DefaultCdcProcessor instance
     */
    @Bean
    public CdcProcessor cdcProcessor() {
        return new DefaultCdcProcessor(cdcEventPublisher(), eventSerializer());
    }
}