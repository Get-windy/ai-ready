package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrintTemplateDTO {

    private Long id;

    private String templateCode;

    private String templateName;

    private String templateType;

    private String fileType;

    private String filePath;

    private String content;

    private String variables;

    private Integer width;

    private Integer height;

    private Integer orientation;

    private Integer copies;

    private Integer paperSize;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}