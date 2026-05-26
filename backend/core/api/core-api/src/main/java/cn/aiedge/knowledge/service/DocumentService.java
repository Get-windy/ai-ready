package cn.aiedge.knowledge.service;

import cn.aiedge.knowledge.entity.KnowledgeDocument;
import cn.aiedge.knowledge.model.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 文档服务接口
 */
public interface DocumentService {
    /** 上传文档 */
    KnowledgeDocument uploadDocument(Long knowledgeBaseId, MultipartFile file);
    
    /** 解析文档 */
    void parseDocument(Long documentId);
    
    /** 分块文档 */
    List<String> chunkDocument(String content, int chunkSize, int overlap);
    
    /** 删除文档 */
    void deleteDocument(Long documentId);
    
    /** 获取文档列表 */
    List<KnowledgeDocument> listDocuments(Long knowledgeBaseId);
}
