package cn.aiedge.erp.batchsn.ai.service;

import cn.aiedge.erp.batchsn.ai.dto.BatchSearchResultDto;
import cn.aiedge.erp.batchsn.ai.dto.IntelligentSearchRequestDto;

import java.util.List;
import java.util.Map;

/**
 * 批次追溯的智能搜索和关联分析服务接口
 */
public interface IntelligentSearchService {
    
    /**
     * 智能搜索批次
     * @param request 搜索请求
     * @return 搜索结果
     */
    BatchSearchResultDto intelligentSearch(IntelligentSearchRequestDto request);
    
    /**
     * 语义搜索批次
     * @param query 自然语言查询
     * @return 搜索结果
     */
    List<BatchSearchResultDto> semanticSearch(String query);
    
    /**
     * 相似批次搜索
     * @param batchId 批次ID
     * @param limit 返回数量
     * @return 相似批次列表
     */
    List<SimilarBatchResult> findSimilarBatches(Long batchId, int limit);
    
    /**
     * 关联分析
     * @param batchIds 批次ID列表
     * @return 关联分析结果
     */
    CorrelationAnalysisResult analyzeCorrelations(List<Long> batchIds);
    
    /**
     * 批次聚类分析
     * @param criteria 聚类标准
     * @return 聚类结果
     */
    ClusteringResult clusterBatches(ClusteringCriteria criteria);
    
    /**
     * 批次知识图谱查询
     * @param query 图谱查询
     * @return 知识图谱结果
     */
    KnowledgeGraphResult queryKnowledgeGraph(String query);
    
    /**
     * 构建批次特征向量
     * @param batchId 批次ID
     * @return 特征向量
     */
    FeatureVector buildFeatureVector(Long batchId);
    
    /**
     * 训练语义搜索模型
     * @param trainingData 训练数据
     * @return 训练结果
     */
    boolean trainSearchModel(List<SearchTrainingData> trainingData);
    
    /**
     * 更新搜索索引
     * @param batchIds 批次ID列表
     * @return 更新结果
     */
    boolean updateSearchIndex(List<Long> batchIds);
    
    /**
     * 获取搜索统计
     * @return 搜索统计
     */
    SearchStatistics getSearchStatistics();
    
    /**
     * 相似批次结果
     */
    class SimilarBatchResult {
        private Long batchId;
        private String batchNumber;
        private Double similarityScore; // 相似度分数 0-1
        private Map<String, Double> featureSimilarities; // 特征相似度
        private String similarityReason; // 相似原因
        
        // getters and setters
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public Double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
        public Map<String, Double> getFeatureSimilarities() { return featureSimilarities; }
        public void setFeatureSimilarities(Map<String, Double> featureSimilarities) { this.featureSimilarities = featureSimilarities; }
        public String getSimilarityReason() { return similarityReason; }
        public void setSimilarityReason(String similarityReason) { this.similarityReason = similarityReason; }
    }
    
    /**
     * 关联分析结果
     */
    class CorrelationAnalysisResult {
        private List<Long> batchIds;
        private Map<String, Double> correlationMatrix; // 相关性矩阵
        private List<StrongCorrelation> strongCorrelations; // 强相关性
        private List<ClusterGroup> clusters; // 聚类分组
        private String analysisSummary; // 分析摘要
        
        // getters and setters
        public List<Long> getBatchIds() { return batchIds; }
        public void setBatchIds(List<Long> batchIds) { this.batchIds = batchIds; }
        public Map<String, Double> getCorrelationMatrix() { return correlationMatrix; }
        public void setCorrelationMatrix(Map<String, Double> correlationMatrix) { this.correlationMatrix = correlationMatrix; }
        public List<StrongCorrelation> getStrongCorrelations() { return strongCorrelations; }
        public void setStrongCorrelations(List<StrongCorrelation> strongCorrelations) { this.strongCorrelations = strongCorrelations; }
        public List<ClusterGroup> getClusters() { return clusters; }
        public void setClusters(List<ClusterGroup> clusters) { this.clusters = clusters; }
        public String getAnalysisSummary() { return analysisSummary; }
        public void setAnalysisSummary(String analysisSummary) { this.analysisSummary = analysisSummary; }
    }
    
    /**
     * 强相关性
     */
    class StrongCorrelation {
        private Long batchId1;
        private Long batchId2;
        private Double correlationScore; // 相关性分数
        private String correlationType; // 相关性类型
        private String explanation; // 解释
        
        // getters and setters
        public Long getBatchId1() { return batchId1; }
        public void setBatchId1(Long batchId1) { this.batchId1 = batchId1; }
        public Long getBatchId2() { return batchId2; }
        public void setBatchId2(Long batchId2) { this.batchId2 = batchId2; }
        public Double getCorrelationScore() { return correlationScore; }
        public void setCorrelationScore(Double correlationScore) { this.correlationScore = correlationScore; }
        public String getCorrelationType() { return correlationType; }
        public void setCorrelationType(String correlationType) { this.correlationType = correlationType; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
    
    /**
     * 聚类分组
     */
    class ClusterGroup {
        private String clusterId;
        private String clusterName;
        private List<Long> batchIds;
        private Map<String, Object> centroidFeatures; // 中心特征
        private String clusterDescription; // 聚类描述
        
        // getters and setters
        public String getClusterId() { return clusterId; }
        public void setClusterId(String clusterId) { this.clusterId = clusterId; }
        public String getClusterName() { return clusterName; }
        public void setClusterName(String clusterName) { this.clusterName = clusterName; }
        public List<Long> getBatchIds() { return batchIds; }
        public void setBatchIds(List<Long> batchIds) { this.batchIds = batchIds; }
        public Map<String, Object> getCentroidFeatures() { return centroidFeatures; }
        public void setCentroidFeatures(Map<String, Object> centroidFeatures) { this.centroidFeatures = centroidFeatures; }
        public String getClusterDescription() { return clusterDescription; }
        public void setClusterDescription(String clusterDescription) { this.clusterDescription = clusterDescription; }
    }
    
    /**
     * 聚类标准
     */
    class ClusteringCriteria {
        private List<String> features; // 特征列表
        private String algorithm; // 聚类算法
        private Integer numberOfClusters; // 聚类数量
        private Double similarityThreshold; // 相似度阈值
        
        // getters and setters
        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Integer getNumberOfClusters() { return numberOfClusters; }
        public void setNumberOfClusters(Integer numberOfClusters) { this.numberOfClusters = numberOfClusters; }
        public Double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    }
    
    /**
     * 知识图谱结果
     */
    class KnowledgeGraphResult {
        private List<KnowledgeNode> nodes; // 节点
        private List<KnowledgeEdge> edges; // 边
        private Map<String, Object> graphMetadata; // 图元数据
        
        // getters and setters
        public List<KnowledgeNode> getNodes() { return nodes; }
        public void setNodes(List<KnowledgeNode> nodes) { this.nodes = nodes; }
        public List<KnowledgeEdge> getEdges() { return edges; }
        public void setEdges(List<KnowledgeEdge> edges) { this.edges = edges; }
        public Map<String, Object> getGraphMetadata() { return graphMetadata; }
        public void setGraphMetadata(Map<String, Object> graphMetadata) { this.graphMetadata = graphMetadata; }
    }
    
    /**
     * 知识节点
     */
    class KnowledgeNode {
        private String nodeId;
        private String nodeType; // 节点类型: batch, product, supplier, etc.
        private String label;
        private Map<String, Object> properties;
        
        // getters and setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }
    
    /**
     * 知识边
     */
    class KnowledgeEdge {
        private String edgeId;
        private String sourceNodeId;
        private String targetNodeId;
        private String relationshipType;
        private Double weight;
        private Map<String, Object> properties;
        
        // getters and setters
        public String getEdgeId() { return edgeId; }
        public void setEdgeId(String edgeId) { this.edgeId = edgeId; }
        public String getSourceNodeId() { return sourceNodeId; }
        public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
        public String getTargetNodeId() { return targetNodeId; }
        public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
        public String getRelationshipType() { return relationshipType; }
        public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }
    
    /**
     * 特征向量
     */
    class FeatureVector {
        private Long batchId;
        private List<Double> vector; // 特征向量值
        private Map<String, Double> featureWeights; // 特征权重
        private Integer dimension; // 维度
        private String vectorType; // 向量类型
        
        // getters and setters
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public List<Double> getVector() { return vector; }
        public void setVector(List<Double> vector) { this.vector = vector; }
        public Map<String, Double> getFeatureWeights() { return featureWeights; }
        public void setFeatureWeights(Map<String, Double> featureWeights) { this.featureWeights = featureWeights; }
        public Integer getDimension() { return dimension; }
        public void setDimension(Integer dimension) { this.dimension = dimension; }
        public String getVectorType() { return vectorType; }
        public void setVectorType(String vectorType) { this.vectorType = vectorType; }
    }
    
    /**
     * 搜索训练数据
     */
    class SearchTrainingData {
        private String query;
        private List<Long> relevantBatchIds;
        private List<Long> irrelevantBatchIds;
        private String queryType;
        private Double relevanceScore;
        
        // getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public List<Long> getRelevantBatchIds() { return relevantBatchIds; }
        public void setRelevantBatchIds(List<Long> relevantBatchIds) { this.relevantBatchIds = relevantBatchIds; }
        public List<Long> getIrrelevantBatchIds() { return irrelevantBatchIds; }
        public void setIrrelevantBatchIds(List<Long> irrelevantBatchIds) { this.irrelevantBatchIds = irrelevantBatchIds; }
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        public Double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
    }
    
    /**
     * 搜索统计
     */
    class SearchStatistics {
        private Integer totalSearches;
        private Integer successfulSearches;
        private Double averageResponseTime;
        private Map<String, Integer> queryTypeCounts;
        private Map<String, Double> successRateByType;
        private List<PopularQuery> popularQueries;
        
        // getters and setters
        public Integer getTotalSearches() { return totalSearches; }
        public void setTotalSearches(Integer totalSearches) { this.totalSearches = totalSearches; }
        public Integer getSuccessfulSearches() { return successfulSearches; }
        public void setSuccessfulSearches(Integer successfulSearches) { this.successfulSearches = successfulSearches; }
        public Double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(Double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public Map<String, Integer> getQueryTypeCounts() { return queryTypeCounts; }
        public void setQueryTypeCounts(Map<String, Integer> queryTypeCounts) { this.queryTypeCounts = queryTypeCounts; }
        public Map<String, Double> getSuccessRateByType() { return successRateByType; }
        public void setSuccessRateByType(Map<String, Double> successRateByType) { this.successRateByType = successRateByType; }
        public List<PopularQuery> getPopularQueries() { return popularQueries; }
        public void setPopularQueries(List<PopularQuery> popularQueries) { this.popularQueries = popularQueries; }
    }
    
    /**
     * 热门查询
     */
    class PopularQuery {
        private String query;
        private Integer searchCount;
        private Double successRate;
        
        // getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getSearchCount() { return searchCount; }
        public void setSearchCount(Integer searchCount) { this.searchCount = searchCount; }
        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
    }
    
    /**
     * 聚类结果
     */
    class ClusteringResult {
        private List<ClusterGroup> clusters;
        private Map<String, Object> clusterMetrics;
        private String algorithmUsed;
        private Integer numberOfClusters;
        
        // getters and setters
        public List<ClusterGroup> getClusters() { return clusters; }
        public void setClusters(List<ClusterGroup> clusters) { this.clusters = clusters; }
        public Map<String, Object> getClusterMetrics() { return clusterMetrics; }
        public void setClusterMetrics(Map<String, Object> clusterMetrics) { this.clusterMetrics = clusterMetrics; }
        public String getAlgorithmUsed() { return algorithmUsed; }
        public void setAlgorithmUsed(String algorithmUsed) { this.algorithmUsed = algorithmUsed; }
        public Integer getNumberOfClusters() { return numberOfClusters; }
        public void setNumberOfClusters(Integer numberOfClusters) { this.numberOfClusters = numberOfClusters; }
    }
}