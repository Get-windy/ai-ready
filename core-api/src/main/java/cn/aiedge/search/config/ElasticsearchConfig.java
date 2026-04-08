package cn.aiedge.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Elasticsearch 配置类
 * 
 * 配置 Elasticsearch 客户端连接
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
@ConditionalOnProperty(prefix = "elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchConfig {

    /**
     * 是否启用 Elasticsearch
     */
    private boolean enabled = false;

    /**
     * 集群节点列表（格式：host:port）
     */
    private List<String> hosts = List.of("localhost:9200");

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 5000;

    /**
     * Socket 超时时间（毫秒）
     */
    private int socketTimeout = 30000;

    /**
     * 最大连接数
     */
    private int maxConnections = 100;

    /**
     * 最大连接数（每路由）
     */
    private int maxConnectionsPerRoute = 20;

    /**
     * 索引前缀
     */
    private String indexPrefix = "ai_ready";

    /**
     * 默认分片数
     */
    private int defaultShards = 3;

    /**
     * 默认副本数
     */
    private int defaultReplicas = 1;

    /**
     * 创建 Elasticsearch 低级别客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "elasticsearch", name = "enabled", havingValue = "true")
    public RestClient elasticsearchRestClient() {
        HttpHost[] httpHosts = hosts.stream()
                .map(this::parseHost)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(httpHosts)
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(connectionTimeout)
                                .setSocketTimeout(socketTimeout))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setMaxConnTotal(maxConnections);
                    httpClientBuilder.setMaxConnPerRoute(maxConnectionsPerRoute);
                    
                    // 添加认证
                    if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                        // 使用 Basic Auth
                        httpClientBuilder.setDefaultCredentialsProvider(
                                new org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback() {
                                    @Override
                                    public org.apache.http.impl.nio.client.HttpAsyncClientBuilder customizeHttpClient(
                                            org.apache.http.impl.nio.client.HttpAsyncClientBuilder httpClientBuilder) {
                                        return httpClientBuilder.setDefaultCredentialsProvider(
                                                new org.apache.http.impl.client.BasicCredentialsProvider() {{
                                                    setCredentials(
                                                            new org.apache.http.auth.AuthScope(HttpHost.ANY_HOST, HttpHost.ANY_PORT),
                                                            new org.apache.http.auth.UsernamePasswordCredentials(username, password)
                                                    );
                                                }}
                                        );
                                    }
                                }.customizeHttpClient(httpClientBuilder));
                    }
                    
                    return httpClientBuilder;
                });

        return builder.build();
    }

    /**
     * 创建 Elasticsearch Transport
     */
    @Bean
    @ConditionalOnProperty(prefix = "elasticsearch", name = "enabled", havingValue = "true")
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    /**
     * 创建 Elasticsearch 高级别客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "elasticsearch", name = "enabled", havingValue = "true")
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    /**
     * 解析 Host 字符串
     */
    private HttpHost parseHost(String hostStr) {
        String[] parts = hostStr.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
        return new HttpHost(host, port, "http");
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<String> getHosts() { return hosts; }
    public void setHosts(List<String> hosts) { this.hosts = hosts; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public int getSocketTimeout() { return socketTimeout; }
    public void setSocketTimeout(int socketTimeout) { this.socketTimeout = socketTimeout; }
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    public int getMaxConnectionsPerRoute() { return maxConnectionsPerRoute; }
    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) { this.maxConnectionsPerRoute = maxConnectionsPerRoute; }
    public String getIndexPrefix() { return indexPrefix; }
    public void setIndexPrefix(String indexPrefix) { this.indexPrefix = indexPrefix; }
    public int getDefaultShards() { return defaultShards; }
    public void setDefaultShards(int defaultShards) { this.defaultShards = defaultShards; }
    public int getDefaultReplicas() { return defaultReplicas; }
    public void setDefaultReplicas(int defaultReplicas) { this.defaultReplicas = defaultReplicas; }
}
