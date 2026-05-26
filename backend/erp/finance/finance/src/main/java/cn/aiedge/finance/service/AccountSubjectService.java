package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.AccountSubject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AccountSubjectService extends IService<AccountSubject> {
    
    List<AccountSubject> listAllEnabled(Long tenantId);
    
    List<AccountSubject> listByParentId(Long tenantId, Long parentId);
    
    List<AccountSubject> listByType(Long tenantId, Integer subjectType);
    
    List<AccountSubject> listLeafSubjects(Long tenantId);
    
    AccountSubject getByCode(Long tenantId, String subjectCode);
    
    Page<AccountSubject> pageList(Long tenantId, String subjectCode, String subjectName, Integer subjectType, Integer level, Integer enabled, Page<AccountSubject> page);
    
    boolean createSubject(AccountSubject subject);
    
    boolean updateSubject(AccountSubject subject);
    
    boolean deleteSubject(Long tenantId, Long subjectId);
    
    boolean enableSubject(Long tenantId, Long subjectId);
    
    boolean disableSubject(Long tenantId, Long subjectId);
    
    List<AccountSubject> buildTree(Long tenantId);
    
    boolean initStandardSubjects(Long tenantId);
}