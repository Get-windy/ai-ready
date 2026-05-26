package cn.aiedge.knowledge.service;

import cn.aiedge.knowledge.model.*;
import java.util.List;

/**
 * 向量检索服务接口
 */
public interface VectorSearchService {
    /** 向量检索 */
    List<VectorSearchResult> search(VectorSearchRequest request);
    
    /** 生成向量嵌入 */
    float[] generateEmbedding(String text);
    
    /** 存储向量 */
    void storeVector(Long chunkId, float[] vector);
    
    /** 删除向量 */
    void deleteVector(String vectorId);
}
