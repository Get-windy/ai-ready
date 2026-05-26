package cn.aiedge.erp.price.engine.config;

import cn.aiedge.erp.price.engine.config.dto.PriceStrategyConfig;

import java.util.List;

/**
 * 价格策略配置解析器接口
 * 使用I-prefix表示Service接口，符合项目规范
 */
public interface IPriceStrategyConfigParser {
    
    /**
     * 解析JSON配置
     * @param jsonConfig JSON配置字符串
     * @return 解析后的价格策略配置
     */
    PriceStrategyConfig parseJson(String jsonConfig);
    
    /**
     * 解析XML配置
     * @param xmlConfig XML配置字符串
     * @return 解析后的价格策略配置
     */
    PriceStrategyConfig parseXml(String xmlConfig);
    
    /**
     * 解析YAML配置
     * @param yamlConfig YAML配置字符串
     * @return 解析后的价格策略配置
     */
    PriceStrategyConfig parseYaml(String yamlConfig);
    
    /**
     * 批量解析配置
     * @param configs 配置字符串列表
     * @return 解析后的价格策略配置列表
     */
    List<PriceStrategyConfig> parseBatch(List<String> configs);
    
    /**
     * 验证配置格式
     * @param config 配置字符串
     * @return 验证结果
     */
    boolean validateFormat(String config);
    
    /**
     * 获取支持的配置格式
     * @return 支持的格式列表
     */
    List<String> getSupportedFormats();
}