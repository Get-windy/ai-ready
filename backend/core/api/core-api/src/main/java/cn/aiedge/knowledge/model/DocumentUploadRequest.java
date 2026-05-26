package cn.aiedge.knowledge.model;

import lombok.Data;

/**
 * 文档上传请求
 */
@Data
public class DocumentUploadRequest {
    private Long knowledgeBaseId;
    private String fileName;
    private String fileType;
    private Long fileSize;
}
