package cn.aiedge.knowledge.service;

import cn.aiedge.knowledge.entity.*;
import cn.aiedge.knowledge.model.*;
import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {
    /** 创建知识库 */
    KnowledgeBase createKnowledgeBase(KnowledgeBaseRequest request);
    
    /** 获取知识库 */
    KnowledgeBase getKnowledgeBase(Long id);
    
    /** 列出知识库 */
    List<KnowledgeBase> listKnowledgeBases(Long tenantId);
    
    /** 删除知识库 */
    void deleteKnowledgeBase(Long id);
}
