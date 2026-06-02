package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.mapper.FinanceTaxDeclarationMapper;
import cn.aiedge.erp.finance.model.entity.FinanceTaxDeclaration;
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
    private FinanceTaxDeclarationMapper financeTaxDeclarationMapper;

    @Override
    public boolean createDeclaration(FinanceTaxDeclaration declaration) {
        try {
            financeTaxDeclarationMapper.insert(declaration);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<FinanceTaxDeclaration> listDeclarations(Integer taxType,
            String declarationPeriod, Integer status) {
        if (taxType != null) {
            return financeTaxDeclarationMapper.findByTaxType(taxType);
        } else if (declarationPeriod != null) {
            return financeTaxDeclarationMapper.findByDeclarationPeriod(declarationPeriod);
        } else if (status != null) {
            return financeTaxDeclarationMapper.findByStatus(status);
        }
        return financeTaxDeclarationMapper.selectList(null);
    }

    @Override
    public FinanceTaxDeclaration getDeclarationDetail(String declarationNo) {
        return financeTaxDeclarationMapper.findByDeclarationNo(declarationNo);
    }

    @Override
    public boolean updateDeclarationStatus(String declarationNo, Integer status) {
        FinanceTaxDeclaration declaration = financeTaxDeclarationMapper.findByDeclarationNo(declarationNo);
        if (declaration == null) {
            return false;
        }
        declaration.setStatus(status);
        financeTaxDeclarationMapper.updateById(declaration);
        return true;
    }

    @Override
    public Object getTaxPayableStatistics(String period) {
        List<FinanceTaxDeclaration> declarations = financeTaxDeclarationMapper.selectList(null);

        double totalTax = declarations.stream()
                .mapToDouble(t -> t.getTaxAmount() != null ? t.getTaxAmount().doubleValue() : 0.0)
                .sum();

        double paidTax = declarations.stream()
                .mapToDouble(t -> t.getPaidAmount() != null ? t.getPaidAmount().doubleValue() : 0.0)
                .sum();

        return Map.of(
                "totalTax", totalTax,
                "paidTax", paidTax,
                "unpaidTax", totalTax - paidTax
        );
    }
}
