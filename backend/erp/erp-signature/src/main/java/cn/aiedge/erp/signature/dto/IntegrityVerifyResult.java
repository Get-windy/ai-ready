package cn.aiedge.erp.signature.dto;

import lombok.Data;

public class IntegrityVerifyResult {

    private Long signatureId;

    private Boolean verified;

    private String verifyMessage;

    private LocalDateTime verifyTime;

    private String originalHash;

    private String currentHash;
}