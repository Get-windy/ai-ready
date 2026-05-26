package cn.aiedge.erp.price.engine.config.impl;

import cn.aiedge.erp.price.engine.config.IPriceStrategyConfigParser;
import cn.aiedge.erp.price.engine.config.dto.PriceStrategyConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 价格策略配置解析器实现
 */
@Service
public class PriceStrategyConfigParserImpl implements IPriceStrategyConfigParser {
    
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;
    private final ObjectMapper yamlMapper;
    
    public PriceStrategyConfigParserImpl() {
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.findAndRegisterModules(); // 支持Java 8日期时间
        
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.findAndRegisterModules();
        
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.findAndRegisterModules();
    }
    
    @Override
    public PriceStrategyConfig parseJson(String jsonConfig) {
        try {
            return jsonMapper.readValue(jsonConfig, PriceStrategyConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON config: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PriceStrategyConfig parseXml(String xmlConfig) {
        try {
            return xmlMapper.readValue(xmlConfig, PriceStrategyConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse XML config: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PriceStrategyConfig parseYaml(String yamlConfig) {
        try {
            return yamlMapper.readValue(yamlConfig, PriceStrategyConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse YAML config: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<PriceStrategyConfig> parseBatch(List<String> configs) {
        List<PriceStrategyConfig> result = new ArrayList<>();
        for (String config : configs) {
            try {
                // 尝试自动检测格式
                if (config.trim().startsWith("{") && config.trim().endsWith("}")) {
                    result.add(parseJson(config));
                } else if (config.trim().startsWith("<")) {
                    result.add(parseXml(config));
                } else {
                    result.add(parseYaml(config));
                }
            } catch (Exception e) {
                // 跳过无法解析的配置，记录错误
                System.err.println("Failed to parse config: " + e.getMessage());
            }
        }
        return result;
    }
    
    @Override
    public boolean validateFormat(String config) {
        if (config == null || config.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = config.trim();
        try {
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                jsonMapper.readTree(config);
                return true;
            } else if (trimmed.startsWith("<")) {
                xmlMapper.readTree(config);
                return true;
            } else {
                yamlMapper.readTree(config);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return Arrays.asList("JSON", "XML", "YAML");
    }
    
    /**
     * 格式化配置为指定格式
     * @param config 价格策略配置
     * @param format 目标格式
     * @return 格式化后的字符串
     */
    public String formatConfig(PriceStrategyConfig config, String format) {
        try {
            switch (format.toUpperCase()) {
                case "JSON":
                    return jsonMapper.writeValueAsString(config);
                case "XML":
                    return xmlMapper.writeValueAsString(config);
                case "YAML":
                    return yamlMapper.writeValueAsString(config);
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to format config: " + e.getMessage(), e);
        }
    }
}