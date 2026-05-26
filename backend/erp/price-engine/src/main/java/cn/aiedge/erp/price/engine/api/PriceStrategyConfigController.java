package cn.aiedge.erp.price.engine.api;

import cn.aiedge.erp.price.engine.config.IPriceStrategyConfigParser;
import cn.aiedge.erp.price.engine.config.dto.PriceStrategyConfig;
import cn.aiedge.erp.price.engine.config.IPriceConfigValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 价格策略配置API控制器
 */
@RestController
@RequestMapping("/api/v1/price-strategy/config")
@Tag(name = "价格策略配置管理", description = "价格策略的配置解析、验证和管理接口")
public class PriceStrategyConfigController {
    
    private final IPriceStrategyConfigParser configParser;
    private final IPriceConfigValidator configValidator;
    
    public PriceStrategyConfigController(
            IPriceStrategyConfigParser configParser,
            IPriceConfigValidator configValidator
    ) {
        this.configParser = configParser;
        this.configValidator = configValidator;
    }
    
    @PostMapping("/parse/json")
    @Operation(summary = "解析JSON配置", description = "解析JSON格式的价格策略配置")
    public ResponseEntity<ApiResponse<PriceStrategyConfig>> parseJsonConfig(
            @Parameter(description = "JSON配置字符串", required = true)
            @RequestBody String jsonConfig
    ) {
        try {
            PriceStrategyConfig config = configParser.parseJson(jsonConfig);
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("解析失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/parse/xml")
    @Operation(summary = "解析XML配置", description = "解析XML格式的价格策略配置")
    public ResponseEntity<ApiResponse<PriceStrategyConfig>> parseXmlConfig(
            @Parameter(description = "XML配置字符串", required = true)
            @RequestBody String xmlConfig
    ) {
        try {
            PriceStrategyConfig config = configParser.parseXml(xmlConfig);
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("解析失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/parse/yaml")
    @Operation(summary = "解析YAML配置", description = "解析YAML格式的价格策略配置")
    public ResponseEntity<ApiResponse<PriceStrategyConfig>> parseYamlConfig(
            @Parameter(description = "YAML配置字符串", required = true)
            @RequestBody String yamlConfig
    ) {
        try {
            PriceStrategyConfig config = configParser.parseYaml(yamlConfig);
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("解析失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/parse/batch")
    @Operation(summary = "批量解析配置", description = "批量解析多种格式的价格策略配置")
    public ResponseEntity<ApiResponse<List<PriceStrategyConfig>>> parseBatchConfigs(
            @Parameter(description = "配置字符串列表", required = true)
            @RequestBody List<String> configs
    ) {
        try {
            List<PriceStrategyConfig> parsedConfigs = configParser.parseBatch(configs);
            return ResponseEntity.ok(ApiResponse.success(parsedConfigs));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("批量解析失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/validate")
    @Operation(summary = "验证配置", description = "验证价格策略配置的有效性和完整性")
    public ResponseEntity<ApiResponse<IPriceConfigValidator.ValidationResult>> validateConfig(
            @Parameter(description = "价格策略配置", required = true)
            @RequestBody PriceStrategyConfig config
    ) {
        try {
            IPriceConfigValidator.ValidationResult result = configValidator.validate(config);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("验证失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/validate/batch")
    @Operation(summary = "批量验证配置", description = "批量验证多个价格策略配置")
    public ResponseEntity<ApiResponse<List<IPriceConfigValidator.ValidationResult>>> validateBatchConfigs(
            @Parameter(description = "价格策略配置列表", required = true)
            @RequestBody List<PriceStrategyConfig> configs
    ) {
        try {
            List<IPriceConfigValidator.ValidationResult> results = configValidator.validateBatch(configs);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("批量验证失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/check/completeness")
    @Operation(summary = "检查配置完整性", description = "检查价格策略配置的完整性")
    public ResponseEntity<ApiResponse<IPriceConfigValidator.CompletenessCheckResult>> checkCompleteness(
            @Parameter(description = "价格策略配置", required = true)
            @RequestBody PriceStrategyConfig config
    ) {
        try {
            IPriceConfigValidator.CompletenessCheckResult result = configValidator.checkCompleteness(config);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("完整性检查失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/check/consistency")
    @Operation(summary = "检查配置一致性", description = "检查多个配置之间的一致性")
    public ResponseEntity<ApiResponse<IPriceConfigValidator.ConsistencyCheckResult>> checkConsistency(
            @Parameter(description = "价格策略配置列表", required = true)
            @RequestBody List<PriceStrategyConfig> configs
    ) {
        try {
            IPriceConfigValidator.ConsistencyCheckResult result = configValidator.checkConsistency(configs);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("一致性检查失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/validate/business-rules")
    @Operation(summary = "验证业务规则", description = "验证配置是否符合业务规则")
    public ResponseEntity<ApiResponse<IPriceConfigValidator.BusinessRuleValidationResult>> validateBusinessRules(
            @Parameter(description = "价格策略配置", required = true)
            @RequestBody PriceStrategyConfig config
    ) {
        try {
            IPriceConfigValidator.BusinessRuleValidationResult result = configValidator.validateBusinessRules(config);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("业务规则验证失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/supported-formats")
    @Operation(summary = "获取支持的格式", description = "获取支持的配置格式列表")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedFormats() {
        try {
            List<String> formats = configParser.getSupportedFormats();
            return ResponseEntity.ok(ApiResponse.success(formats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取格式失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/validation-rules")
    @Operation(summary = "获取验证规则", description = "获取所有验证规则")
    public ResponseEntity<ApiResponse<List<IPriceConfigValidator.ValidationRule>>> getValidationRules() {
        try {
            List<IPriceConfigValidator.ValidationRule> rules = configValidator.getValidationRules();
            return ResponseEntity.ok(ApiResponse.success(rules));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取验证规则失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/format")
    @Operation(summary = "格式化配置", description = "将配置格式化为指定格式")
    public ResponseEntity<ApiResponse<String>> formatConfig(
            @Parameter(description = "格式化请求", required = true)
            @RequestBody FormatRequest request
    ) {
        try {
            // 假设configParser有formatConfig方法
            if (configParser instanceof cn.aiedge.erp.price.engine.config.impl.PriceStrategyConfigParserImpl) {
                cn.aiedge.erp.price.engine.config.impl.PriceStrategyConfigParserImpl parserImpl = 
                        (cn.aiedge.erp.price.engine.config.impl.PriceStrategyConfigParserImpl) configParser;
                String formatted = parserImpl.formatConfig(request.config(), request.format());
                return ResponseEntity.ok(ApiResponse.success(formatted));
            }
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(ApiResponse.error("格式化功能未实现"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("格式化失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查配置服务健康状态")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "PriceStrategyConfigService",
                "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * API响应包装类
     */
    public record ApiResponse<T>(
            boolean success,
            String message,
            T data,
            long timestamp
    ) {
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "成功", data, System.currentTimeMillis());
        }
        
        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null, System.currentTimeMillis());
        }
    }
    
    /**
     * 格式化请求
     */
    public record FormatRequest(
            PriceStrategyConfig config,
            String format
    ) {}
}