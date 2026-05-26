package com.aiedge.codegen.generator;

import com.aiedge.codegen.entity.DataSourceInfo;
import com.aiedge.codegen.entity.GenerationConfig;
import com.aiedge.codegen.entity.Template;
import com.aiedge.codegen.service.DataSourceInfoService;
import com.aiedge.codegen.service.GenerationConfigService;
import com.aiedge.codegen.service.TemplateService;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 代码生成服务类
 * 
 * @author AI-Ready
 */
@Slf4j
@Service
public class CodeGeneratorService {
    
    @Autowired
    private DataSourceInfoService dataSourceInfoService;
    
    @Autowired
    private GenerationConfigService generationConfigService;
    
    @Autowired
    private TemplateService templateService;
    
    /**
     * 生成代码
     *
     * @param dataSourceId 数据源ID
     * @param configId 生成配置ID
     * @param tables 表名列表
     * @param outputDir 输出目录
     * @return 生成结果
     */
    public boolean generateCode(Long dataSourceId, Long configId, List<String> tables, String outputDir) {
        try {
            // 获取数据源信息
            DataSourceInfo dataSourceInfo = dataSourceInfoService.getById(dataSourceId);
            if (dataSourceInfo == null) {
                log.error("数据源不存在，ID: {}", dataSourceId);
                return false;
            }

            // 获取生成配置
            GenerationConfig genConfig = generationConfigService.getById(configId);
            if (genConfig == null) {
                log.error("生成配置不存在，ID: {}", configId);
                return false;
            }

            // 构建代码生成器
            FastAutoGenerator.create(
                dataSourceInfo.getJdbcUrl(),
                dataSourceInfo.getUsername(),
                dataSourceInfo.getPassword()
            )
            .globalConfig(builder -> {
                builder.author(genConfig.getAuthor())
                       .outputDir(outputDir)
                       .commentDate(genConfig.getCommentDate());
            })
            .packageConfig(builder -> {
                builder.parent(genConfig.getPackageName())
                       .moduleName(genConfig.getModuleName());
            })
            .strategyConfig(builder -> {
                builder.addInclude(tables.toArray(new String[0]))
                       .entityBuilder()
                       .naming(NamingStrategy.underline_to_camel)
                       .columnNaming(NamingStrategy.underline_to_camel)
                       .serviceBuilder()
                       .formatServiceFileName("%sService")
                       .formatServiceImplFileName("%sServiceImpl");
            })
            .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker作为模板引擎
            .execute();

            log.info("代码生成完成，数据源: {}, 表: {}", dataSourceInfo.getName(), tables);
            return true;
        } catch (Exception e) {
            log.error("代码生成失败", e);
            return false;
        }
    }
    
    /**
     * 生成代码（使用默认配置）
     * 
     * @param dataSourceId 数据源ID
     * @param tables 表名列表
     * @param outputDir 输出目录
     * @return 生成结果
     */
    public boolean generateCodeWithDefaultConfig(Long dataSourceId, List<String> tables, String outputDir) {
        GenerationConfig defaultConfig = generationConfigService.getDefaultConfig();
        if (defaultConfig == null) {
            log.error("未找到默认生成配置");
            return false;
        }
        return generateCode(dataSourceId, defaultConfig.getId(), tables, outputDir);
    }
    
    /**
     * 使用自定义模板生成代码
     *
     * @param dataSourceId 数据源ID
     * @param configId 生成配置ID
     * @param templateId 模板ID
     * @param tables 表名列表
     * @param outputDir 输出目录
     * @return 生成结果
     */
    public boolean generateCodeWithCustomTemplate(Long dataSourceId, Long configId, Long templateId,
                                                 List<String> tables, String outputDir) {
        try {
            // 获取数据源信息
            DataSourceInfo dataSourceInfo = dataSourceInfoService.getById(dataSourceId);
            if (dataSourceInfo == null) {
                log.error("数据源不存在，ID: {}", dataSourceId);
                return false;
            }

            // 获取生成配置
            GenerationConfig genConfig = generationConfigService.getById(configId);
            if (genConfig == null) {
                log.error("生成配置不存在，ID: {}", configId);
                return false;
            }

            // 获取自定义模板
            Template template = templateService.getById(templateId);
            if (template == null) {
                log.error("模板不存在，ID: {}", templateId);
                return false;
            }

            // 构建代码生成器
            FastAutoGenerator.create(
                dataSourceInfo.getJdbcUrl(),
                dataSourceInfo.getUsername(),
                dataSourceInfo.getPassword()
            )
            .globalConfig(builder -> {
                builder.author(genConfig.getAuthor())
                       .outputDir(outputDir)
                       .commentDate(genConfig.getCommentDate());
            })
            .packageConfig(builder -> {
                builder.parent(genConfig.getPackageName())
                       .moduleName(genConfig.getModuleName());
            })
            .strategyConfig(builder -> {
                builder.addInclude(tables.toArray(new String[0]))
                       .entityBuilder()
                       .naming(NamingStrategy.underline_to_camel)
                       .columnNaming(NamingStrategy.underline_to_camel)
                       .serviceBuilder()
                       .formatServiceFileName("%sService")
                       .formatServiceImplFileName("%sServiceImpl");
            })
            .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker作为模板引擎
            .execute();

            log.info("使用自定义模板生成代码完成，数据源: {}, 表: {}", dataSourceInfo.getName(), tables);
            return true;
        } catch (Exception e) {
            log.error("使用自定义模板生成代码失败", e);
            return false;
        }
    }

}