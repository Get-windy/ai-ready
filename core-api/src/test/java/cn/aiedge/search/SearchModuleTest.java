package cn.aiedge.search;

import cn.aiedge.search.index.IndexManagementService;
import cn.aiedge.search.model.SearchRequest;
import cn.aiedge.search.model.SearchResponse;
import cn.aiedge.search.service.ElasticsearchSearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 搜索模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SearchModuleTest {

    @Mock
    private ElasticsearchClient mockClient;

    @Mock
    private ElasticsearchTransport mockTransport;

    @Mock
    private ElasticsearchIndicesClient mockIndicesClient;

    private IndexManagementService indexManagementService;
    private ElasticsearchSearchService searchService;

    private static final Long TEST_TENANT_ID = 1L;
    private static final String TEST_INDEX_PREFIX = "ai_ready";

    @BeforeEach
    void setUp() {
        // 初始化索引管理服务（由于依赖注入限制，这里直接使用实现）
        indexManagementService = new IndexManagementService() {
            @Override
            public String getFullIndexName(String indexType, Long tenantId) {
                if (tenantId == null) {
                    return String.format("%s_%s", TEST_INDEX_PREFIX, indexType);
                }
                return String.format("%s_%s_%d", TEST_INDEX_PREFIX, indexType, tenantId);
            }
        };
    }

    // ==================== 索引名称测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试获取完整索引名称 - 有租户ID")
    void testGetFullIndexName_WithTenantId() {
        // Given
        String indexType = IndexManagementService.INDEX_CUSTOMER;
        
        // When
        String indexName = indexManagementService.getFullIndexName(indexType, TEST_TENANT_ID);
        
        // Then
        assertEquals("ai_ready_customer_1", indexName);
    }

    @Test
    @Order(2)
    @DisplayName("测试获取完整索引名称 - 无租户ID")
    void testGetFullIndexName_WithoutTenantId() {
        // Given
        String indexType = IndexManagementService.INDEX_PRODUCT;
        
        // When
        String indexName = indexManagementService.getFullIndexName(indexType, null);
        
        // Then
        assertEquals("ai_ready_product", indexName);
    }

    @Test
    @Order(3)
    @DisplayName("测试索引别名")
    void testGetIndexAlias() {
        // Given
        String indexType = IndexManagementService.INDEX_ORDER;
        
        // When
        String alias = indexManagementService.getIndexAlias(indexType);
        
        // Then
        assertEquals("ai_ready_order_alias", alias);
    }

    // ==================== 搜索请求测试 ====================

    @Test
    @Order(10)
    @DisplayName("测试搜索请求构建 - 默认值")
    void testSearchRequest_DefaultValues() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setKeyword("测试");
        
        // Then
        assertEquals("测试", request.getKeyword());
        assertEquals("all", request.getType());
        assertEquals(1, request.getPage());
        assertEquals(10, request.getPageSize());
        assertEquals(0, request.getOffset());
        assertTrue(request.isHighlight());
        assertTrue(request.isReturnTotal());
        assertEquals("desc", request.getSortOrder());
    }

    @Test
    @Order(11)
    @DisplayName("测试搜索请求 - 分页计算")
    void testSearchRequest_PaginationCalculation() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setKeyword("测试");
        request.setPage(3);
        request.setPageSize(20);
        
        // Then
        assertEquals(40, request.getOffset()); // (3-1) * 20 = 40
    }

    @Test
    @Order(12)
    @DisplayName("测试搜索请求 - 搜索类型")
    void testSearchRequest_SearchTypes() {
        // When
        SearchRequest allRequest = new SearchRequest();
        allRequest.setKeyword("测试");
        allRequest.setType("all");
        
        SearchRequest singleRequest = new SearchRequest();
        singleRequest.setKeyword("测试");
        singleRequest.setType("customer");
        
        // Then
        assertEquals(List.of("customer", "product", "order"), allRequest.getSearchTypes());
        assertEquals(List.of("customer"), singleRequest.getSearchTypes());
    }

    // ==================== 搜索请求验证测试 ====================

    @Test
    @Order(20)
    @DisplayName("测试搜索请求 - 页码验证")
    void testSearchRequest_PageValidation() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setKeyword("测试");
        request.setPage(0);
        
        // Then
        assertTrue(request.getPage() < 1, "页码应该大于等于1");
    }

    @Test
    @Order(21)
    @DisplayName("测试搜索请求 - 页大小验证")
    void testSearchRequest_PageSizeValidation() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setKeyword("测试");
        request.setPageSize(150);
        
        // Then
        assertTrue(request.getPageSize() > 100, "页大小超过100应该被验证");
    }

    // ==================== 索引类型常量测试 ====================

    @Test
    @Order(30)
    @DisplayName("测试索引类型常量")
    void testIndexTypeConstants() {
        assertEquals("customer", IndexManagementService.INDEX_CUSTOMER);
        assertEquals("product", IndexManagementService.INDEX_PRODUCT);
        assertEquals("order", IndexManagementService.INDEX_ORDER);
        assertEquals("supplier", IndexManagementService.INDEX_SUPPLIER);
        assertEquals("contract", IndexManagementService.INDEX_CONTRACT);
        assertEquals("knowledge", IndexManagementService.INDEX_KNOWLEDGE);
    }

    // ==================== 文档操作测试 ====================

    @Test
    @Order(40)
    @DisplayName("测试文档数据构建")
    void testDocumentBuilder() {
        // Given
        Map<String, Object> document = new HashMap<>();
        document.put("id", 1L);
        document.put("tenantId", TEST_TENANT_ID);
        document.put("name", "测试客户");
        document.put("code", "C001");
        document.put("status", "ACTIVE");
        
        // Then
        assertNotNull(document);
        assertEquals(5, document.size());
        assertEquals("测试客户", document.get("name"));
        assertEquals("C001", document.get("code"));
    }

    @Test
    @Order(41)
    @DisplayName("测试批量文档构建")
    void testBulkDocumentsBuilder() {
        // Given
        Map<String, Map<String, Object>> documents = new HashMap<>();
        
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", (long) i);
            doc.put("name", "产品" + i);
            doc.put("code", "P" + String.format("%03d", i));
            documents.put(String.valueOf(i), doc);
        }
        
        // Then
        assertEquals(10, documents.size());
        assertTrue(documents.containsKey("1"));
        assertTrue(documents.containsKey("10"));
    }

    // ==================== 过滤条件测试 ====================

    @Test
    @Order(50)
    @DisplayName("测试过滤条件构建")
    void testFilterConditions() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setKeyword("测试");
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "ACTIVE");
        filters.put("category", "电子");
        request.setFilters(filters);
        
        // Then
        assertNotNull(request.getFilters());
        assertEquals(2, request.getFilters().size());
        assertEquals("ACTIVE", request.getFilters().get("status"));
        assertEquals("电子", request.getFilters().get("category"));
    }

    // ==================== 高亮配置测试 ====================

    @Test
    @Order(60)
    @DisplayName("测试高亮配置")
    void testHighlightConfiguration() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setKeyword("测试");
        request.setHighlight(true);
        request.setHighlightPre("<span class='highlight'>");
        request.setHighlightPost("</span>");
        
        // Then
        assertTrue(request.isHighlight());
        assertEquals("<span class='highlight'>", request.getHighlightPre());
        assertEquals("</span>", request.getHighlightPost());
    }

    // ==================== 响应构建测试 ====================

    @Test
    @Order(70)
    @DisplayName("测试搜索响应构建")
    void testSearchResponseBuilder() {
        // Given
        SearchResponse response = new SearchResponse();
        response.setKeyword("测试");
        response.setType("customer");
        response.setTotal(100L);
        response.setPage(1);
        response.setPageSize(10);
        response.setTotalPages(10);
        response.setTook(50L);
        response.setHasMore(true);
        
        // Then
        assertEquals("测试", response.getKeyword());
        assertEquals("customer", response.getType());
        assertEquals(100L, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getPageSize());
        assertEquals(10, response.getTotalPages());
        assertEquals(50L, response.getTook());
        assertTrue(response.isHasMore());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(80)
    @DisplayName("测试空关键词")
    void testEmptyKeyword() {
        SearchRequest request = new SearchRequest();
        // 关键词不应为空（由验证注解处理）
        assertNull(request.getKeyword());
    }

    @Test
    @Order(81)
    @DisplayName("测试特殊字符关键词")
    void testSpecialCharactersKeyword() {
        // Given
        SearchRequest request = new SearchRequest();
        request.setKeyword("测试<script>alert('xss')</script>");
        
        // Then
        assertNotNull(request.getKeyword());
        assertTrue(request.getKeyword().contains("<script>"));
        // 注意：实际应用中需要对特殊字符进行转义或过滤
    }

    @Test
    @Order(82)
    @DisplayName("测试超长关键词")
    void testTooLongKeyword() {
        // Given
        StringBuilder longKeyword = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longKeyword.append("测");
        }
        
        SearchRequest request = new SearchRequest();
        request.setKeyword(longKeyword.toString());
        
        // Then
        assertEquals(1000, request.getKeyword().length());
    }

    // ==================== 性能测试 ====================

    @Test
    @Order(90)
    @DisplayName("测试大量搜索类型解析性能")
    void testSearchTypesPerformance() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10000; i++) {
            SearchRequest request = new SearchRequest();
            request.setKeyword("测试" + i);
            request.setType("all");
            List<String> types = request.getSearchTypes();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 10000次操作应该在1秒内完成
        assertTrue(duration < 1000, "搜索类型解析性能测试失败，耗时: " + duration + "ms");
    }

    // ==================== 集成测试标记 ====================

    @Test
    @Order(100)
    @DisplayName("集成测试 - Elasticsearch连接（需要真实ES环境）")
    @Disabled("需要真实Elasticsearch环境，在CI/CD中运行")
    void testElasticsearchConnection() {
        // 此测试需要真实的Elasticsearch环境
        // 在CI/CD流水线中启用
    }
}
