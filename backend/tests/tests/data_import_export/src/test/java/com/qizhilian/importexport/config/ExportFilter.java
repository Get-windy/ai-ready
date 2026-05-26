package com.qizhilian.importexport.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 导出筛选条件
 */
public class ExportFilter {
    private List<FilterCondition> conditions = new ArrayList<>();
    
    public void addCondition(String field, String operator, Object value) {
        conditions.add(new FilterCondition(field, operator, value));
    }
    
    public List<FilterCondition> getConditions() {
        return conditions;
    }
    
    /**
     * 筛选条件
     */
    public static class FilterCondition {
        private String field;
        private String operator;
        private Object value;
        
        public FilterCondition(String field, String operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
        
        public String getField() { return field; }
        public String getOperator() { return operator; }
        public Object getValue() { return value; }
    }
}
