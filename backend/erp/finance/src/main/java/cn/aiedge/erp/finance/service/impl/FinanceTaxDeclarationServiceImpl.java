package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.model.entity.FinanceTaxDeclaration;
import cn.aiedge.erp.finance.repository.FinanceTaxDeclarationRepository;
import cn.aiedge.erp.finance.service.FinanceTaxDeclarationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 税务申报Service实现类
 */
@Service
public class FinanceTaxDeclarationServiceImpl implements FinanceTaxDeclarationService {
    
    @Autowired
    private FinanceTaxDeclarationRepository financeTaxDeclarationRepository;
    
    @Override
    public boolean createDeclaration(FinanceTaxDeclaration declaration) {
        try {
            financeTaxDeclarationRepository.save(declaration);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public List<FinanceTaxDeclaration> listDeclarations(Integer taxType, 
            String declarationPeriod, Integer status) {
        if (taxType != null) {
            return financeTaxDeclarationRepository.findByTaxType(taxType);
        } else if (declarationPeriod != null) {
            return financeTaxDeclarationRepository.findByDeclarationPeriod(declarationPeriod);
        } else if (status != null) {
            return financeTaxDeclarationRepository.findByStatus(status);
        }
        return financeTaxDeclarationRepository.findAll();
    }
    
    @Override
    public FinanceTaxDeclaration getDeclarationDetail(String declarationNo) {
        return financeTaxDeclarationRepository.findByDeclarationNo(declarationNo);
    }
    
    @Override
    public boolean updateDeclarationStatus(String declarationNo, Integer status) {
        FinanceTaxDeclaration declaration = financeTaxDeclarationRepository.findByDeclarationNo(declarationNo);
        if (declaration == null) {
            return false;
        }
        declaration.setStatus(status);
        financeTaxDeclarationRepository.save(declaration);
        return true;
    }
    
    @Override
    public Object getTaxPayableStatistics(String period) {
        // 统计应缴税款
        List<FinanceTaxDeclaration> declarations = financeTaxDeclarationRepository.findAll();
        
        Double totalTax = declarations.stream()
            .mapToDouble(t -> t.getTaxAmount() != null ? t.getTaxAmount() : 0.0)
            .sum();
            
        Double paidTax = declarations.stream()
            .mapToDouble(t -> t.getPaidAmount() != null ? t.getPaidAmount() : 0.0)
            .sum();
        
        return Map.of(
            "totalTax", totalTax,
            "paidTax", paidTax,
            "unpaidTax", totalTax - paidTax
        );
    }
}
