package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import java.util.List;
import java.util.Map;

/**
 * 报表数据源服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ReportDataSourceService {

    /**
     * 根据数据源类型获取报表数据
     *
     * @param definition 报表定义
     * @param parameters 查询参数
     * @param tenantId 租户ID
     * @return 报表数据
     */
    List<Map<String, Object>> fetchData(ReportDefinition definition, 
                                         Map<String, Object> parameters, Long tenantId);

    /**
     * 测试数据源连接
     *
     * @param dataSourceConfig 数据源配置
     * @return 是否连接成功
     */
    boolean testConnection(Map<String, Object> dataSourceConfig);

    /**
     * 获取数据源类型
     *
     * @return 支持的数据源类型列表
     */
    List<String> getSupportedDataSourceTypes();

    /**
     * 执行SQL查询
     *
     * @param sql SQL语句
     * @param parameters 参数
     * @param tenantId 租户ID
     * @return 查询结果
     */
    List<Map<String, Object>> executeSql(String sql, Map<String, Object> parameters, Long tenantId);

    /**
     * 调用API数据源
     *
     * @param apiUrl API地址
     * @param method 请求方法
     * @param headers 请求头
     * @param parameters 请求参数
     * @return API响应
     */
    Map<String, Object> callApi(String apiUrl, String method, 
                                Map<String, String> headers, 
                                Map<String, Object> parameters);

    /**
     * 混合数据源查询 - 先执行SQL再调用API
     *
     * @param sql SQL语句
     * @param apiUrl API地址
     * @param parameters 混合参数
     * @param tenantId 租户ID
     * @return 合并后的数据
     */
    List<Map<String, Object>> fetchMixedData(String sql, String apiUrl,
                                              Map<String, Object> parameters, Long tenantId);
}
