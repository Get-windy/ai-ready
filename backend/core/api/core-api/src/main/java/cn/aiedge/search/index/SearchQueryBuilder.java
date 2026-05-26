package cn.aiedge.search.index;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 搜索查询构建器
 * 
 * 支持多种查询类型：
 * - 词条查询（Term Query）
 * - 匹配查询（Match Query）
 * - 布尔查询（Bool Query）
 * - 范围查询（Range Query）
 * - 前缀查询（Prefix Query）
 * - 通配符查询（Wildcard Query）
 * - 模糊查询（Fuzzy Query）
 * - 短语查询（Phrase Query）
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class SearchQueryBuilder {

    /**
     * 创建词条查询
     */
    public static TermQuery term(String field, String value) {
        return new TermQuery(field, value);
    }

    /**
     * 创建匹配查询
     */
    public static MatchQuery match(String field, String text) {
        return new MatchQuery(field, text);
    }

    /**
     * 创建布尔查询
     */
    public static BoolQuery bool() {
        return new BoolQuery();
    }

    /**
     * 创建范围查询
     */
    public static RangeQuery range(String field) {
        return new RangeQuery(field);
    }

    /**
     * 创建前缀查询
     */
    public static PrefixQuery prefix(String field, String prefix) {
        return new PrefixQuery(field, prefix);
    }

    /**
     * 创建通配符查询
     */
    public static WildcardQuery wildcard(String field, String pattern) {
        return new WildcardQuery(field, pattern);
    }

    /**
     * 创建模糊查询
     */
    public static FuzzyQuery fuzzy(String field, String term) {
        return new FuzzyQuery(field, term);
    }

    /**
     * 创建短语查询
     */
    public static PhraseQuery phrase(String field, String phrase) {
        return new PhraseQuery(field, phrase);
    }

    /**
     * 创建多字段查询
     */
    public static MultiMatchQuery multiMatch(String text, String... fields) {
        return new MultiMatchQuery(text, fields);
    }

    // ==================== 查询类型定义 ====================

    /**
     * 查询基类
     */
    @Data
    public static abstract class Query {
        protected String field;
        protected double boost = 1.0;

        public Query boost(double boost) {
            this.boost = boost;
            return this;
        }

        public abstract Map<Long, Double> execute(InvertedIndex index);
    }

    /**
     * 词条查询
     */
    @Data
    public static class TermQuery extends Query {
        private final String value;

        public TermQuery(String field, String value) {
            this.field = field;
            this.value = value;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> results = index.search(value);
            // 应用boost
            results.replaceAll((k, v) -> v * boost);
            return results;
        }
    }

    /**
     * 匹配查询（对文本分词后搜索）
     */
    @Data
    public static class MatchQuery extends Query {
        private final String text;
        private MatchOperator operator = MatchOperator.OR;

        public MatchQuery(String field, String text) {
            this.field = field;
            this.text = text;
        }

        public MatchQuery operator(MatchOperator operator) {
            this.operator = operator;
            return this;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            List<String> terms = tokenize(text);
            if (terms.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<Long, Double> results;
            if (operator == MatchOperator.AND) {
                results = index.searchAnd(terms);
            } else {
                results = index.searchOr(terms);
            }

            results.replaceAll((k, v) -> v * boost);
            return results;
        }

        public enum MatchOperator {
            AND, OR
        }
    }

    /**
     * 布尔查询
     */
    @Data
    public static class BoolQuery extends Query {
        private List<Query> must = new ArrayList<>();
        private List<Query> should = new ArrayList<>();
        private List<Query> mustNot = new ArrayList<>();
        private int minimumShouldMatch = 1;

        public BoolQuery must(Query query) {
            must.add(query);
            return this;
        }

        public BoolQuery should(Query query) {
            should.add(query);
            return this;
        }

        public BoolQuery mustNot(Query query) {
            mustNot.add(query);
            return this;
        }

        public BoolQuery minimumShouldMatch(int count) {
            this.minimumShouldMatch = count;
            return this;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> results = new HashMap<>();

            // 处理must（必须匹配，取交集）
            if (!must.isEmpty()) {
                for (Query q : must) {
                    Map<Long, Double> queryResults = q.execute(index);
                    if (results.isEmpty()) {
                        results.putAll(queryResults);
                    } else {
                        results.keySet().retainAll(queryResults.keySet());
                        for (Long docId : results.keySet()) {
                            results.put(docId, results.get(docId) + queryResults.getOrDefault(docId, 0.0));
                        }
                    }
                }
            }

            // 处理mustNot（必须不匹配，从结果中排除）
            Set<Long> excludeIds = new HashSet<>();
            for (Query q : mustNot) {
                excludeIds.addAll(q.execute(index).keySet());
            }
            results.keySet().removeAll(excludeIds);

            // 处理should（可选匹配，至少匹配minimumShouldMatch个）
            if (!should.isEmpty()) {
                Map<Long, Double> shouldResults = new HashMap<>();
                Map<Long, Integer> shouldCount = new HashMap<>();

                for (Query q : should) {
                    Map<Long, Double> queryResults = q.execute(index);
                    for (Map.Entry<Long, Double> entry : queryResults.entrySet()) {
                        shouldResults.merge(entry.getKey(), entry.getValue(), Double::sum);
                        shouldCount.merge(entry.getKey(), 1, Integer::sum);
                    }
                }

                // 过滤掉匹配数不足的文档
                Set<Long> validIds = new HashSet<>();
                for (Map.Entry<Long, Integer> entry : shouldCount.entrySet()) {
                    if (entry.getValue() >= minimumShouldMatch) {
                        validIds.add(entry.getKey());
                    }
                }

                if (must.isEmpty() && mustNot.isEmpty()) {
                    // 只有should，返回满足条件的
                    results.clear();
                    for (Long docId : validIds) {
                        results.put(docId, shouldResults.get(docId));
                    }
                } else {
                    // 有must，should只影响分数
                    for (Long docId : validIds) {
                        if (results.containsKey(docId)) {
                            results.put(docId, results.get(docId) + shouldResults.get(docId) * 0.5);
                        }
                    }
                }
            }

            results.replaceAll((k, v) -> v * boost);
            return results;
        }
    }

    /**
     * 范围查询
     */
    @Data
    public static class RangeQuery extends Query {
        private Object from;
        private Object to;
        private boolean includeFrom = true;
        private boolean includeTo = true;

        public RangeQuery(String field) {
            this.field = field;
        }

        public RangeQuery from(Object from) {
            this.from = from;
            return this;
        }

        public RangeQuery to(Object to) {
            this.to = to;
            return this;
        }

        public RangeQuery includeFrom(boolean include) {
            this.includeFrom = include;
            return this;
        }

        public RangeQuery includeTo(boolean include) {
            this.includeTo = include;
            return this;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> results = new HashMap<>();

            for (Map.Entry<Long, Map<String, Object>> entry : index.getForwardIndex().entrySet()) {
                Long docId = entry.getKey();
                Map<String, Object> doc = entry.getValue();
                Object value = doc.get(field);

                if (value != null && isInRange(value)) {
                    results.put(docId, 1.0 * boost);
                }
            }

            return results;
        }

        private boolean isInRange(Object value) {
            if (from != null) {
                int cmp = compareValues(value, from);
                if (cmp < 0 || (cmp == 0 && !includeFrom)) {
                    return false;
                }
            }
            if (to != null) {
                int cmp = compareValues(value, to);
                if (cmp > 0 || (cmp == 0 && !includeTo)) {
                    return false;
                }
            }
            return true;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private int compareValues(Object v1, Object v2) {
            if (v1 instanceof Comparable && v2 instanceof Comparable) {
                return ((Comparable) v1).compareTo(v2);
            }
            return v1.toString().compareTo(v2.toString());
        }
    }

    /**
     * 前缀查询
     */
    @Data
    public static class PrefixQuery extends Query {
        private final String prefix;

        public PrefixQuery(String field, String prefix) {
            this.field = field;
            this.prefix = prefix;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> results = index.searchPrefix(prefix);
            results.replaceAll((k, v) -> v * boost);
            return results;
        }
    }

    /**
     * 通配符查询
     */
    @Data
    public static class WildcardQuery extends Query {
        private final String pattern;

        public WildcardQuery(String field, String pattern) {
            this.field = field;
            this.pattern = pattern;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> results = index.searchWildcard(pattern);
            results.replaceAll((k, v) -> v * boost);
            return results;
        }
    }

    /**
     * 模糊查询
     */
    @Data
    public static class FuzzyQuery extends Query {
        private final String term;
        private int maxDistance = 2;
        private int prefixLength = 0;

        public FuzzyQuery(String field, String term) {
            this.field = field;
            this.term = term;
        }

        public FuzzyQuery maxDistance(int distance) {
            this.maxDistance = distance;
            return this;
        }

        public FuzzyQuery prefixLength(int length) {
            this.prefixLength = length;
            return this;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> results = index.searchFuzzy(term, maxDistance);
            results.replaceAll((k, v) -> v * boost);
            return results;
        }
    }

    /**
     * 短语查询
     */
    @Data
    public static class PhraseQuery extends Query {
        private final String phrase;
        private int slop = 0; // 允许的词间距离

        public PhraseQuery(String field, String phrase) {
            this.field = field;
            this.phrase = phrase;
        }

        public PhraseQuery slop(int slop) {
            this.slop = slop;
            return this;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> results = index.searchPhrase(phrase);
            results.replaceAll((k, v) -> v * boost);
            return results;
        }
    }

    /**
     * 多字段查询
     */
    @Data
    public static class MultiMatchQuery extends Query {
        private final String text;
        private final String[] fields;
        private Map<String, Double> fieldBoosts = new HashMap<>();
        private MatchQuery.MatchOperator operator = MatchQuery.MatchOperator.OR;

        public MultiMatchQuery(String text, String... fields) {
            this.text = text;
            this.fields = fields;
        }

        public MultiMatchQuery fieldBoost(String field, double boost) {
            fieldBoosts.put(field, boost);
            return this;
        }

        public MultiMatchQuery operator(MatchQuery.MatchOperator operator) {
            this.operator = operator;
            return this;
        }

        @Override
        public Map<Long, Double> execute(InvertedIndex index) {
            Map<Long, Double> combinedResults = new HashMap<>();

            for (String field : fields) {
                double fieldBoost = fieldBoosts.getOrDefault(field, 1.0);
                MatchQuery matchQuery = new MatchQuery(field, text);
                matchQuery.operator(operator);
                matchQuery.boost(fieldBoost * boost);

                Map<Long, Double> fieldResults = matchQuery.execute(index);
                for (Map.Entry<Long, Double> entry : fieldResults.entrySet()) {
                    combinedResults.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }

            return combinedResults;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 分词
     */
    private static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        String[] words = text.toLowerCase().split("[\\s\\p{Punct}]+");
        for (String word : words) {
            if (!word.isEmpty() && word.length() >= 1) {
                tokens.add(word);
            }
        }

        // 中文分词
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                tokens.add(String.valueOf(c));
            }
        }

        return tokens;
    }
}
