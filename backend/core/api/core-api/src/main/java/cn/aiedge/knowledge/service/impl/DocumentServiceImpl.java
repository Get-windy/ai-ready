package cn.aiedge.knowledge.service.impl;

import cn.aiedge.knowledge.entity.*;
import cn.aiedge.knowledge.mapper.*;
import cn.aiedge.knowledge.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档服务实现
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Override
    public KnowledgeDocument uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setName(file.getOriginalFilename());
        doc.setFileType(getFileType(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setStatus("pending");
        doc.setChunkCount(0);
        documentMapper.insert(doc);
        return doc;
    }

    @Override
    public void parseDocument(Long documentId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        doc.setStatus("processing");
        documentMapper.updateById(doc);
        // 实际解析逻辑需要根据文件类型调用对应解析器
        doc.setStatus("completed");
        documentMapper.updateById(doc);
    }

    @Override
    public List<String> chunkDocument(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            start = end - overlap;
            if (start < 0) break;
        }
        return chunks;
    }

    @Override
    public void deleteDocument(Long documentId) {
        documentMapper.deleteById(documentId);
    }

    @Override
    public List<KnowledgeDocument> listDocuments(Long knowledgeBaseId) {
        return documentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
        );
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "unknown";
    }
}
