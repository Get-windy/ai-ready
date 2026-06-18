package cn.aiedge.erp.signature.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class IntegrityVerifyResult {

    private Long signatureId;

    private Boolean verified;

    private String verifyMessage;

    private LocalDateTime verifyTime;

    private String originalHash;

    private String currentHash;
}