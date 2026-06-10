package cn.aiedge.knowledge.controller;

import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckLogin;

import cn.aiedge.knowledge.entity.*;
import cn.aiedge.knowledge.model.*;
import cn.aiedge.knowledge.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/knowledge")
@SaCheckLogin
public class KnowledgeController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private VectorSearchService vectorSearchService;

    @PostMapping("/base")
    public KnowledgeBase createKnowledgeBase(@RequestBody KnowledgeBaseRequest request) {
        return knowledgeBaseService.createKnowledgeBase(request);
    }

    @GetMapping("/base/{id}")
    public KnowledgeBase getKnowledgeBase(@PathVariable Long id) {
        return knowledgeBaseService.getKnowledgeBase(id);
    }

    @GetMapping("/base/list")
    public List<KnowledgeBase> listKnowledgeBases(@RequestParam Long tenantId) {
        return knowledgeBaseService.listKnowledgeBases(tenantId);
    }

    @DeleteMapping("/base/{id}")
    public void deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
    }

    @PostMapping("/document/upload")
    public KnowledgeDocument uploadDocument(
            @RequestParam Long knowledgeBaseId,
            @RequestParam MultipartFile file) {
        return documentService.uploadDocument(knowledgeBaseId, file);
    }

    @PostMapping("/document/{id}/parse")
    public void parseDocument(@PathVariable Long id) {
        documentService.parseDocument(id);
    }

    @GetMapping("/document/list")
    public List<KnowledgeDocument> listDocuments(@RequestParam Long knowledgeBaseId) {
        return documentService.listDocuments(knowledgeBaseId);
    }

    @PostMapping("/search")
    public List<VectorSearchResult> search(@RequestBody VectorSearchRequest request) {
        return vectorSearchService.search(request);
    }

    @PostMapping("/embedding")
    public float[] generateEmbedding(@RequestBody Map<String, String> request) {
        return vectorSearchService.generateEmbedding(request.get("text"));
    }
}
