package com.aiedge.codegen.config;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成器配置类
 *
 * @author AI-Ready
 */
@Configuration
public class CodeGeneratorConfig {

    /**
     * 代码生成器配置
     *
     * @return FastAutoGenerator
     */
    public FastAutoGenerator getCodeGenerator() {
        return FastAutoGenerator.create("jdbc:mysql://localhost:3306/test", "root", "password");
    }

    /**
     * 获取输出配置
     *
     * @param outputDir 输出目录
     * @return 输出配置Map
     */
    public Map<String, Object> getOutputConfig(String outputDir) {
        Map<String, Object> config = new HashMap<>();

        // 设置各个文件的输出路径
        config.put("controller", outputDir + "/controller");
        config.put("service", outputDir + "/service");
        config.put("serviceImpl", outputDir + "/service/impl");
        config.put("mapper", outputDir + "/mapper");
        config.put("xml", outputDir + "/mapper/xml");
        config.put("entity", outputDir + "/entity");

        return config;
    }

}