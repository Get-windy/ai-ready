package cn.aiedge.erp.signature.service;

import cn.aiedge.erp.signature.dto.*;
import cn.aiedge.erp.signature.entity.SignatureRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

public interface SignatureService {

    SignatureRecord photoSignature(PhotoSignatureRequest request, MultipartFile photo);

    SignatureRecord electronicSignature(ElectronicSignatureRequest request);

    SignatureRecord getSignatureById(Long id);

    SignatureRecord getLatestByOrderNo(String orderNo);

    Page<SignatureRecord> listSignatures(Integer page, Integer size, String orderNo, String signatureType);

    IntegrityVerifyResult verifyIntegrity(Long id);

    Page<SignatureRecord> listByDeliveryPerson(String deliveryPersonId, Integer page, Integer size);
}