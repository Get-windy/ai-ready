package cn.aiedge.erp.sale;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import cn.aiedge.erp.sale.mapper.SaleOrderItemMapper;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    public SaleOrderItemMapper saleOrderItemMapper() {
        return org.mockito.Mockito.mock(SaleOrderItemMapper.class);
    }
}