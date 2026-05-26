package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.ReportTemplate;
import cn.aiedge.finance.entity.DataSource;
import cn.aiedge.finance.entity.ChartConfig;
import cn.aiedge.finance.entity.ReportInstance;
import cn.aiedge.finance.mapper.ReportTemplateMapper;
import cn.aiedge.finance.mapper.DataSourceMapper;
import cn.aiedge.finance.mapper.ChartConfigMapper;
import cn.aiedge.finance.mapper.ReportInstanceMapper;
import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.service.IIntelligentReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能报表服务实现
 */
@Service
@Transactional
public class IntelligentReportServiceImpl implements IIntelligentReportService {

    private final ReportTemplateMapper reportTemplateMapper;
    private final DataSourceMapper dataSourceMapper;
    private final ChartConfigMapper chartConfigMapper;
    private final ReportInstanceMapper reportInstanceMapper;

    public IntelligentReportServiceImpl(
            ReportTemplateMapper reportTemplateMapper,
            DataSourceMapper dataSourceMapper,
            ChartConfigMapper chartConfigMapper,
            ReportInstanceMapper reportInstanceMapper) {
        this.reportTemplateMapper = reportTemplateMapper;
        this.dataSourceMapper = dataSourceMapper;
        this.chartConfigMapper = chartConfigMapper;
        this.reportInstanceMapper = reportInstanceMapper;
    }

    @Override
    public Long createReportTemplate(ReportTemplateCreateRequest request) {
        ReportTemplate template = new ReportTemplate();
        BeanUtils.copyProperties(request, template);
        
        // 设置编码和基础信息
        template.setTemplateCode("RT-" + System.currentTimeMillis());
        template.setTenantId(getCurrentTenantId());
        template.setCreatedBy(getCurrentUser());
        template.setUpdatedBy(getCurrentUser());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        // 设置默认值
        if (template.getIsActive() == null) {
            template.setIsActive(true);
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(false);
        }
        if (template.getSortOrder() == null) {
            template.setSortOrder(0);
        }
        
        reportTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateReportTemplate(ReportTemplate template) {
        template.setUpdatedBy(getCurrentUser());
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateMapper.updateById(template);
    }

    @Override
    public Page<ReportTemplateVO> pageReportTemplates(ReportTemplateQueryRequest request) {
        LambdaQueryWrapper<ReportTemplate> wrapper = Wrappers.lambdaQuery(ReportTemplate.class)
                .eq(request.getTemplateType() != null, ReportTemplate::getTemplateType, request.getTemplateType())
                .eq(request.getCategory() != null, ReportTemplate::getCategory, request.getCategory())
                .like(request.getTemplateName() != null, ReportTemplate::getTemplateName, request.getTemplateName())
                .eq(request.getIsActive() != null, ReportTemplate::getIsActive, request.getIsActive())
                .eq(request.getIsDefault() != null, ReportTemplate::getIsDefault, request.getIsDefault())
                .eq(request.getDataSourceId() != null, ReportTemplate::getDataSourceId, request.getDataSourceId())
                .orderByDesc(ReportTemplate::getCreateTime);

        Page<ReportTemplate> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<ReportTemplate> resultPage = reportTemplateMapper.selectPage(page, wrapper);

        Page<ReportTemplateVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<ReportTemplateVO> voList = resultPage.getRecords().stream()
                .map(this::convertReportTemplateToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public ReportTemplateVO getReportTemplateById(Long id) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template != null) {
            return convertReportTemplateToVO(template);
        }
        return null;
    }

    @Override
    public void deleteReportTemplate(Long id) {
        reportTemplateMapper.deleteById(id);
    }

    @Override
    public void toggleReportTemplateActive(Long id, boolean active) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template != null) {
            template.setIsActive(active);
            template.setUpdatedBy(getCurrentUser());
            template.setUpdateTime(LocalDateTime.now());
            reportTemplateMapper.updateById(template);
        }
    }

    @Override
    public Long createDataSource(DataSourceCreateRequest request) {
        DataSource dataSource = new DataSource();
        BeanUtils.copyProperties(request, dataSource);
        
        // 设置编码和基础信息
        dataSource.setDataSourceCode("DS-" + System.currentTimeMillis());
        dataSource.setTenantId(getCurrentTenantId());
        dataSource.setCreatedBy(getCurrentUser());
        dataSource.setUpdatedBy(getCurrentUser());
        dataSource.setCreateTime(LocalDateTime.now());
        dataSource.setUpdateTime(LocalDateTime.now());
        
        // 设置默认值
        if (dataSource.getIsActive() == null) {
            dataSource.setIsActive(true);
        }
        if (dataSource.getIsShared() == null) {
            dataSource.setIsShared(false);
        }
        if (dataSource.getIsEncrypted() == null) {
            dataSource.setIsEncrypted(false);
        }
        if (dataSource.getMaxConnections() == null) {
            dataSource.setMaxConnections(10);
        }
        if (dataSource.getMinConnections() == null) {
            dataSource.setMinConnections(1);
        }
        if (dataSource.getTimeout() == null) {
            dataSource.setTimeout(30000); // 30秒
        }
        
        // 加密密码
        if (request.getPassword() != null) {
            dataSource.setPassword(encryptPassword(request.getPassword()));
        }
        
        dataSourceMapper.insert(dataSource);
        return dataSource.getId();
    }

    @Override
    public void updateDataSource(DataSource dataSource) {
        dataSource.setUpdatedBy(getCurrentUser());
        dataSource.setUpdateTime(LocalDateTime.now());
        dataSourceMapper.updateById(dataSource);
    }

    @Override
    public Page<DataSourceVO> pageDataSources(DataSourceQueryRequest request) {
        LambdaQueryWrapper<DataSource> wrapper = Wrappers.lambdaQuery(DataSource.class)
                .eq(request.getDataSourceType() != null, DataSource::getDataSourceType, request.getDataSourceType())
                .like(request.getDataSourceName() != null, DataSource::getDataSourceName, request.getDataSourceName())
                .eq(request.getDatabaseName() != null, DataSource::getDatabaseName, request.getDatabaseName())
                .eq(request.getIsActive() != null, DataSource::getIsActive, request.getIsActive())
                .eq(request.getIsShared() != null, DataSource::getIsShared, request.getIsShared())
                .orderByDesc(DataSource::getCreateTime);

        Page<DataSource> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<DataSource> resultPage = dataSourceMapper.selectPage(page, wrapper);

        Page<DataSourceVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<DataSourceVO> voList = resultPage.getRecords().stream()
                .map(this::convertDataSourceToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public DataSourceVO getDataSourceById(Long id) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource != null) {
            return convertDataSourceToVO(dataSource);
        }
        return null;
    }

    @Override
    public void deleteDataSource(Long id) {
        dataSourceMapper.deleteById(id);
    }

    @Override
    public boolean testDataSourceConnection(Long id) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource == null) {
            return false;
        }

        Connection conn = null;
        try {
            // 解密密码
            String decryptedPassword = decryptPassword(dataSource.getPassword());
            
            // 根据数据源类型创建连接
            if ("MYSQL".equalsIgnoreCase(dataSource.getDataSourceType())) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else if ("POSTGRESQL".equalsIgnoreCase(dataSource.getDataSourceType())) {
                Class.forName("org.postgresql.Driver");
            } else if ("ORACLE".equalsIgnoreCase(dataSource.getDataSourceType())) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
            } else if ("SQLSERVER".equalsIgnoreCase(dataSource.getDataSourceType())) {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            }
            
            conn = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                decryptedPassword
            );
            
            // 执行测试查询
            if (dataSource.getTestQuery() != null && !dataSource.getTestQuery().isEmpty()) {
                java.sql.Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(dataSource.getTestQuery());
                rs.close();
                stmt.close();
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("数据源连接测试失败: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("关闭连接时出错: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Object previewDataSourceData(Long id, String query, int limit) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在");
        }

        if (query == null || query.isEmpty()) {
            throw new RuntimeException("查询语句不能为空");
        }

        // 添加限制条件以防止返回过多数据
        String limitedQuery = query;
        if (!limitedQuery.toLowerCase().contains("limit")) {
            limitedQuery += " LIMIT " + Math.min(limit, 1000);
        }

        Connection conn = null;
        try {
            // 解密密码
            String decryptedPassword = decryptPassword(dataSource.getPassword());
            
            conn = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                decryptedPassword
            );
            
            PreparedStatement stmt = conn.prepareStatement(limitedQuery);
            ResultSet rs = stmt.executeQuery();
            
            // 将结果集转换为List<Map<String, Object>>
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                result.add(row);
            }
            
            rs.close();
            stmt.close();
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("预览数据时出错: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("关闭连接时出错: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Long createChartConfig(ChartConfigCreateRequest request) {
        ChartConfig chartConfig = new ChartConfig();
        BeanUtils.copyProperties(request, chartConfig);
        
        // 设置编码和基础信息
        chartConfig.setChartCode("CC-" + System.currentTimeMillis());
        chartConfig.setTenantId(getCurrentTenantId());
        chartConfig.setCreatedBy(getCurrentUser());
        chartConfig.setUpdatedBy(getCurrentUser());
        chartConfig.setCreateTime(LocalDateTime.now());
        chartConfig.setUpdateTime(LocalDateTime.now());
        
        // 设置默认值
        if (chartConfig.getShowGrid() == null) {
            chartConfig.setShowGrid(true);
        }
        if (chartConfig.getShowLegend() == null) {
            chartConfig.setShowLegend(true);
        }
        if (chartConfig.getShowLabels() == null) {
            chartConfig.setShowLabels(false);
        }
        if (chartConfig.getShowAnimation() == null) {
            chartConfig.setShowAnimation(true);
        }
        if (chartConfig.getIsStacked() == null) {
            chartConfig.setIsStacked(false);
        }
        if (chartConfig.getIsPercentage() == null) {
            chartConfig.setIsPercentage(false);
        }
        if (chartConfig.getWidth() == null) {
            chartConfig.setWidth(800);
        }
        if (chartConfig.getHeight() == null) {
            chartConfig.setHeight(600);
        }
        if (chartConfig.getSortOrder() == null) {
            chartConfig.setSortOrder(0);
        }
        
        chartConfigMapper.insert(chartConfig);
        return chartConfig.getId();
    }

    @Override
    public void updateChartConfig(ChartConfig chartConfig) {
        chartConfig.setUpdatedBy(getCurrentUser());
        chartConfig.setUpdateTime(LocalDateTime.now());
        chartConfigMapper.updateById(chartConfig);
    }

    @Override
    public Page<ChartConfigVO> pageChartConfigs(ChartConfigQueryRequest request) {
        LambdaQueryWrapper<ChartConfig> wrapper = Wrappers.lambdaQuery(ChartConfig.class)
                .eq(request.getChartType() != null, ChartConfig::getChartType, request.getChartType())
                .like(request.getChartName() != null, ChartConfig::getChartName, request.getChartName())
                .eq(request.getReportTemplateId() != null, ChartConfig::getReportTemplateId, request.getReportTemplateId())
                .eq(request.getDataSourceId() != null, ChartConfig::getDataSourceId, request.getDataSourceId())
                .eq(request.getShowGrid() != null, ChartConfig::getShowGrid, request.getShowGrid())
                .eq(request.getShowLegend() != null, ChartConfig::getShowLegend, request.getShowLegend())
                .orderByDesc(ChartConfig::getCreateTime);

        Page<ChartConfig> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<ChartConfig> resultPage = chartConfigMapper.selectPage(page, wrapper);

        Page<ChartConfigVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<ChartConfigVO> voList = resultPage.getRecords().stream()
                .map(this::convertChartConfigToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public ChartConfigVO getChartConfigById(Long id) {
        ChartConfig chartConfig = chartConfigMapper.selectById(id);
        if (chartConfig != null) {
            return convertChartConfigToVO(chartConfig);
        }
        return null;
    }

    @Override
    public void deleteChartConfig(Long id) {
        chartConfigMapper.deleteById(id);
    }

    @Override
    public Object getChartData(Long id) {
        ChartConfig chartConfig = chartConfigMapper.selectById(id);
        if (chartConfig == null) {
            throw new RuntimeException("图表配置不存在");
        }

        String sqlQuery = chartConfig.getSqlQuery();
        if (sqlQuery == null || sqlQuery.isEmpty()) {
            throw new RuntimeException("图表配置中未设置SQL查询语句");
        }

        DataSource dataSource = null;
        if (chartConfig.getDataSourceId() != null) {
            // 查找关联的数据源
            LambdaQueryWrapper<DataSource> dsWrapper = Wrappers.lambdaQuery(DataSource.class)
                    .eq(DataSource::getDataSourceCode, chartConfig.getDataSourceId());
            List<DataSource> dataSources = dataSourceMapper.selectList(dsWrapper);
            if (!dataSources.isEmpty()) {
                dataSource = dataSources.get(0);
            }
        }

        if (dataSource == null) {
            // 如果没有指定数据源，则尝试使用模板中的数据源
            ReportTemplate template = reportTemplateMapper.selectById(chartConfig.getReportTemplateId());
            if (template != null && template.getDataSourceId() != null) {
                LambdaQueryWrapper<DataSource> dsWrapper = Wrappers.lambdaQuery(DataSource.class)
                        .eq(DataSource::getDataSourceCode, template.getDataSourceId());
                List<DataSource> dataSources = dataSourceMapper.selectList(dsWrapper);
                if (!dataSources.isEmpty()) {
                    dataSource = dataSources.get(0);
                }
            }
        }

        if (dataSource == null) {
            throw new RuntimeException("找不到可用的数据源");
        }

        Connection conn = null;
        try {
            String decryptedPassword = decryptPassword(dataSource.getPassword());
            
            conn = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                decryptedPassword
            );
            
            PreparedStatement stmt = conn.prepareStatement(sqlQuery);
            ResultSet rs = stmt.executeQuery();
            
            // 将结果集转换为图表数据格式
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<String> columns = new java.util.ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            
            List<Map<String, Object>> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
            
            rs.close();
            stmt.close();
            
            Map<String, Object> result = new HashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("chartType", chartConfig.getChartType());
            result.put("chartName", chartConfig.getChartName());
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("获取图表数据时出错: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("关闭连接时出错: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Long generateReportInstance(ReportInstanceCreateRequest request) {
        ReportInstance instance = new ReportInstance();
        BeanUtils.copyProperties(request, instance);
        
        // 设置编码和基础信息
        instance.setReportCode("RI-" + System.currentTimeMillis());
        instance.setTenantId(getCurrentTenantId());
        instance.setGeneratedBy(getCurrentUser());
        instance.setStatus("GENERATING");
        instance.setCreateTime(LocalDateTime.now());
        instance.setUpdateTime(LocalDateTime.now());
        
        // 设置默认值
        if (instance.getReportDate() == null) {
            instance.setReportDate(LocalDate.now());
        }
        if (instance.getIsFavorite() == null) {
            instance.setIsFavorite(false);
        }
        if (instance.getIsShared() == null) {
            instance.setIsShared(false);
        }
        if (instance.getViewCount() == null) {
            instance.setViewCount(0);
        }
        if (instance.getDownloadCount() == null) {
            instance.setDownloadCount(0);
        }
        
        reportInstanceMapper.insert(instance);
        
        // 生成报表数据
        try {
            generateReportData(instance);
            
            // 更新状态为完成
            instance.setStatus("COMPLETED");
            instance.setUpdateTime(LocalDateTime.now());
            reportInstanceMapper.updateById(instance);
        } catch (Exception e) {
            // 更新状态为失败
            instance.setStatus("FAILED");
            instance.setExecutionLog("生成失败: " + e.getMessage());
            instance.setUpdateTime(LocalDateTime.now());
            reportInstanceMapper.updateById(instance);
            throw new RuntimeException("报表生成失败: " + e.getMessage());
        }
        
        return instance.getId();
    }

    @Override
    public void updateReportInstance(ReportInstance instance) {
        instance.setUpdateTime(LocalDateTime.now());
        reportInstanceMapper.updateById(instance);
    }

    @Override
    public Page<ReportInstanceVO> pageReportInstances(ReportInstanceQueryRequest request) {
        LambdaQueryWrapper<ReportInstance> wrapper = Wrappers.lambdaQuery(ReportInstance.class)
                .eq(request.getReportType() != null, ReportInstance::getReportType, request.getReportType())
                .like(request.getReportName() != null, ReportInstance::getReportName, request.getReportName())
                .eq(request.getTemplateId() != null, ReportInstance::getTemplateId, request.getTemplateId())
                .eq(request.getReportDate() != null, ReportInstance::getReportDate, request.getReportDate())
                .ge(request.getStartDate() != null, ReportInstance::getReportDate, request.getStartDate())
                .le(request.getEndDate() != null, ReportInstance::getReportDate, request.getEndDate())
                .eq(request.getStatus() != null, ReportInstance::getStatus, request.getStatus())
                .eq(request.getGeneratedBy() != null, ReportInstance::getGeneratedBy, request.getGeneratedBy())
                .orderByDesc(ReportInstance::getCreateTime);

        Page<ReportInstance> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<ReportInstance> resultPage = reportInstanceMapper.selectPage(page, wrapper);

        Page<ReportInstanceVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<ReportInstanceVO> voList = resultPage.getRecords().stream()
                .map(this::convertReportInstanceToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public ReportInstanceVO getReportInstanceById(Long id) {
        ReportInstance instance = reportInstanceMapper.selectById(id);
        if (instance != null) {
            return convertReportInstanceToVO(instance);
        }
        return null;
    }

    @Override
    public void deleteReportInstance(Long id) {
        reportInstanceMapper.deleteById(id);
    }

    @Override
    public byte[] exportReport(Long id, String format) {
        ReportInstance instance = reportInstanceMapper.selectById(id);
        if (instance == null) {
            throw new RuntimeException("报表实例不存在");
        }

        // 根据格式生成相应的内容
        String content = generateReportExportContent(instance, format);
        return content.getBytes();
    }

    @Override
    public void publishReport(Long id) {
        ReportInstance instance = reportInstanceMapper.selectById(id);
        if (instance == null) {
            throw new RuntimeException("报表实例不存在");
        }

        // 更新状态为已发布
        instance.setStatus("PUBLISHED");
        instance.setPublishedBy(getCurrentUser());
        instance.setUpdateTime(LocalDateTime.now());
        reportInstanceMapper.updateById(instance);
    }

    @Override
    public Object getReportData(Long id) {
        ReportInstance instance = reportInstanceMapper.selectById(id);
        if (instance == null) {
            throw new RuntimeException("报表实例不存在");
        }

        // 返回报表数据
        if (instance.getReportData() != null) {
            // 这里应该解析JSON字符串为对象
            return instance.getReportData();
        }
        return null;
    }

    @Override
    public Object executeCustomQuery(String dataSourceId, String sql, Object[] params) {
        if (sql == null || sql.isEmpty()) {
            throw new RuntimeException("SQL查询语句不能为空");
        }

        // 查找数据源
        LambdaQueryWrapper<DataSource> dsWrapper = Wrappers.lambdaQuery(DataSource.class)
                .eq(DataSource::getDataSourceCode, dataSourceId);
        List<DataSource> dataSources = dataSourceMapper.selectList(dsWrapper);
        if (dataSources.isEmpty()) {
            throw new RuntimeException("找不到指定的数据源: " + dataSourceId);
        }

        DataSource dataSource = dataSources.get(0);
        Connection conn = null;
        try {
            String decryptedPassword = decryptPassword(dataSource.getPassword());
            
            conn = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                decryptedPassword
            );
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            ResultSet rs = stmt.executeQuery();
            
            // 将结果集转换为List<Map<String, Object>>
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                result.add(row);
            }
            
            rs.close();
            stmt.close();
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("执行自定义查询时出错: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("关闭连接时出错: " + e.getMessage());
                }
            }
        }
    }

    private ReportTemplateVO convertReportTemplateToVO(ReportTemplate template) {
        ReportTemplateVO vo = new ReportTemplateVO();
        BeanUtils.copyProperties(template, vo);
        return vo;
    }

    private DataSourceVO convertDataSourceToVO(DataSource dataSource) {
        DataSourceVO vo = new DataSourceVO();
        BeanUtils.copyProperties(dataSource, vo);
        // 不复制密码字段（DataSourceVO中没有password字段）
        return vo;
    }

    private ChartConfigVO convertChartConfigToVO(ChartConfig chartConfig) {
        ChartConfigVO vo = new ChartConfigVO();
        BeanUtils.copyProperties(chartConfig, vo);
        return vo;
    }

    private ReportInstanceVO convertReportInstanceToVO(ReportInstance instance) {
        ReportInstanceVO vo = new ReportInstanceVO();
        BeanUtils.copyProperties(instance, vo);
        return vo;
    }

    private void generateReportData(ReportInstance instance) {
        // 获取报表模板
        ReportTemplate template = reportTemplateMapper.selectById(instance.getTemplateId());
        if (template == null) {
            throw new RuntimeException("报表模板不存在");
        }

        // 获取关联的数据源
        DataSource dataSource = null;
        if (template.getDataSourceId() != null) {
            LambdaQueryWrapper<DataSource> dsWrapper = Wrappers.lambdaQuery(DataSource.class)
                    .eq(DataSource::getDataSourceCode, template.getDataSourceId());
            List<DataSource> dataSources = dataSourceMapper.selectList(dsWrapper);
            if (!dataSources.isEmpty()) {
                dataSource = dataSources.get(0);
            }
        }

        if (dataSource == null) {
            throw new RuntimeException("报表模板未配置数据源");
        }

        // 执行SQL查询获取报表数据
        String sqlQuery = template.getSqlQuery();
        if (sqlQuery == null || sqlQuery.isEmpty()) {
            throw new RuntimeException("报表模板未配置SQL查询语句");
        }

        Connection conn = null;
        try {
            String decryptedPassword = decryptPassword(dataSource.getPassword());
            
            conn = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                decryptedPassword
            );
            
            PreparedStatement stmt = conn.prepareStatement(sqlQuery);
            ResultSet rs = stmt.executeQuery();
            
            // 将结果集转换为JSON格式的数据
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<Map<String, Object>> reportData = new java.util.ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                reportData.add(row);
            }
            
            rs.close();
            stmt.close();
            
            // 保存报表数据到实例
            instance.setReportData(convertToJson(reportData));
            
            // 获取关联的图表配置并生成图表数据
            LambdaQueryWrapper<ChartConfig> chartWrapper = Wrappers.lambdaQuery(ChartConfig.class)
                    .eq(ChartConfig::getReportTemplateId, template.getId());
            List<ChartConfig> chartConfigs = chartConfigMapper.selectList(chartWrapper);
            
            if (!chartConfigs.isEmpty()) {
                List<Map<String, Object>> chartData = new java.util.ArrayList<>();
                for (ChartConfig chartConfig : chartConfigs) {
                    try {
                        Object chartResult = getChartData(chartConfig.getId());
                        Map<String, Object> chartMap = new HashMap<>();
                        chartMap.put("chartId", chartConfig.getId());
                        chartMap.put("chartName", chartConfig.getChartName());
                        chartMap.put("data", chartResult);
                        chartData.add(chartMap);
                    } catch (Exception e) {
                        System.err.println("生成图表数据失败: " + e.getMessage());
                    }
                }
                
                if (!chartData.isEmpty()) {
                    instance.setChartData(convertToJson(chartData));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("生成报表数据时出错: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("关闭连接时出错: " + e.getMessage());
                }
            }
        }
    }

    private String generateReportExportContent(ReportInstance instance, String format) {
        // 根据不同格式生成报表导出内容
        switch (format.toUpperCase()) {
            case "PDF":
                return "PDF Export of Report: " + instance.getReportName();
            case "EXCEL":
                return "Excel Export of Report: " + instance.getReportName();
            case "WORD":
                return "Word Export of Report: " + instance.getReportName();
            case "CSV":
                return "CSV Export of Report: " + instance.getReportName();
            default:
                return "Default Export of Report: " + instance.getReportName();
        }
    }

    private String convertToJson(Object obj) {
        // 简单的JSON转换实现
        // 在实际应用中，应该使用Jackson或Gson等库
        return obj.toString(); // 临时实现
    }

    private String encryptPassword(String password) {
        // 简单的密码加密实现
        // 在实际应用中，应该使用更强的加密算法
        if (password == null) {
            return null;
        }
        // 这里只是简单示例，实际应用中应该使用安全的加密方法
        return java.util.Base64.getEncoder().encodeToString(password.getBytes());
    }

    private String decryptPassword(String encryptedPassword) {
        // 简单的密码解密实现
        // 在实际应用中，应该使用对应的解密算法
        if (encryptedPassword == null) {
            return null;
        }
        // 这里只是简单示例，实际应用中应该使用安全的解密方法
        return new String(java.util.Base64.getDecoder().decode(encryptedPassword));
    }

    private Long getCurrentTenantId() {
        // 获取当前租户ID，这里需要根据实际的租户管理实现来获取
        return 1L; // 临时实现，实际项目中需要正确获取租户ID
    }

    private String getCurrentUser() {
        // 获取当前用户名
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsString();
        }
        return "system";
    }
}
