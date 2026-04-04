package cn.aiedge.search.index;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 倒排索引
 * 
 * 核心数据结构：
 * - term -> docId列表（倒排列表）
 * - 支持快速关键词查找，O(1)复杂度
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Data
public class InvertedIndex {

    /**
     * 索引名称
     */
    private String name;

    /**
     * 倒排索引：term -> docId -> positions
     */
    private final Map<String, Map<Long, List<Integer>>> invertedIndex = new ConcurrentHashMap<>();

    /**
     * 正排索引：docId -> document
     */
    private final Map<Long, Map<String, Object>> forwardIndex = new ConcurrentHashMap<>();

    /**
     * 文档频率：term -> docCount（用于TF-IDF计算）
     */
    private final Map<String, AtomicLong> documentFrequency = new ConcurrentHashMap<>();

    /**
     * 总文档数
     */
    private final AtomicLong totalDocuments = new AtomicLong(0);

    /**
     * 字段权重配置
     */
    private Map<String, Double> fieldWeights = new HashMap<>();

    public InvertedIndex(String name) {
        this.name = name;
    }

    /**
     * 添加文档到索引
     *
     * @param docId  文档ID
     * @param document 文档内容
     * @param fieldsToIndex 需要索引的字段列表
     */
    public void addDocument(Long docId, Map<String, Object> document, List<String> fieldsToIndex) {
        if (docId == null || document == null) {
            return;
        }

        // 存储到正排索引
        forwardIndex.put(docId, document);

        // 处理每个字段
        for (String field : fieldsToIndex) {
            Object value = document.get(field);
            if (value == null) continue;

            String text = value.toString();
            List<String> terms = tokenize(text);

            for (int i = 0; i < terms.size(); i++) {
                String term = terms.get(i);
                
                // 更新倒排索引
                invertedIndex.computeIfAbsent(term, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent(docId, k -> new ArrayList<>())
                        .add(i);

                // 更新文档频率
                documentFrequency.computeIfAbsent(term, k -> new AtomicLong(0)).incrementAndGet();
            }
        }

        totalDocuments.incrementAndGet();
        log.debug("索引文档: index={}, docId={}, totalDocs={}", name, docId, totalDocuments.get());
    }

    /**
     * 删除文档
     *
     * @param docId 文档ID
     */
    public void removeDocument(Long docId) {
        Map<String, Object> document = forwardIndex.remove(docId);
        if (document == null) {
            return;
        }

        // 从倒排索引中移除
        for (String term : new HashSet<>(invertedIndex.keySet())) {
            Map<Long, List<Integer>> postings = invertedIndex.get(term);
            if (postings != null) {
                postings.remove(docId);
                if (postings.isEmpty()) {
                    invertedIndex.remove(term);
                    documentFrequency.remove(term);
                }
            }
        }

        totalDocuments.decrementAndGet();
        log.debug("删除文档: index={}, docId={}", name, docId);
    }

    /**
     * 更新文档
     *
     * @param docId 文档ID
     * @param document 新文档内容
     * @param fieldsToIndex 需要索引的字段列表
     */
    public void updateDocument(Long docId, Map<String, Object> document, List<String> fieldsToIndex) {
        removeDocument(docId);
        addDocument(docId, document, fieldsToIndex);
    }

    /**
     * 搜索词条
     *
     * @param term 搜索词条
     * @return 匹配的文档ID和分数
     */
    public Map<Long, Double> search(String term) {
        Map<Long, Double> results = new HashMap<>();
        
        Map<Long, List<Integer>> postings = invertedIndex.get(term.toLowerCase());
        if (postings == null || postings.isEmpty()) {
            return results;
        }

        double idf = calculateIDF(term);

        for (Map.Entry<Long, List<Integer>> entry : postings.entrySet()) {
            Long docId = entry.getKey();
            int tf = entry.getValue().size(); // 词频
            double tfIdf = tf * idf;
            results.put(docId, tfIdf);
        }

        return results;
    }

    /**
     * 多词条搜索（AND）
     *
     * @param terms 词条列表
     * @return 同时包含所有词条的文档
     */
    public Map<Long, Double> searchAnd(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> results = null;

        for (String term : terms) {
            Map<Long, Double> termResults = search(term.toLowerCase());
            
            if (results == null) {
                results = new HashMap<>(termResults);
            } else {
                // 交集
                results.keySet().retainAll(termResults.keySet());
                // 合并分数
                for (Long docId : results.keySet()) {
                    results.put(docId, results.get(docId) + termResults.getOrDefault(docId, 0.0));
                }
            }

            if (results.isEmpty()) {
                break;
            }
        }

        return results != null ? results : Collections.emptyMap();
    }

    /**
     * 多词条搜索（OR）
     *
     * @param terms 词条列表
     * @return 包含任一词条的文档
     */
    public Map<Long, Double> searchOr(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> results = new HashMap<>();

        for (String term : terms) {
            Map<Long, Double> termResults = search(term.toLowerCase());
            for (Map.Entry<Long, Double> entry : termResults.entrySet()) {
                results.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        return results;
    }

    /**
     * 前缀搜索
     *
     * @param prefix 前缀
     * @return 匹配的文档
     */
    public Map<Long, Double> searchPrefix(String prefix) {
        Map<Long, Double> results = new HashMap<>();
        String lowerPrefix = prefix.toLowerCase();

        for (String term : invertedIndex.keySet()) {
            if (term.startsWith(lowerPrefix)) {
                Map<Long, Double> termResults = search(term);
                for (Map.Entry<Long, Double> entry : termResults.entrySet()) {
                    results.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }

        return results;
    }

    /**
     * 通配符搜索（支持*和?）
     *
     * @param pattern 通配符模式
     * @return 匹配的文档
     */
    public Map<Long, Double> searchWildcard(String pattern) {
        Map<Long, Double> results = new HashMap<>();
        String regex = wildcardToRegex(pattern.toLowerCase());

        for (String term : invertedIndex.keySet()) {
            if (term.matches(regex)) {
                Map<Long, Double> termResults = search(term);
                for (Map.Entry<Long, Double> entry : termResults.entrySet()) {
                    results.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }

        return results;
    }

    /**
     * 模糊搜索（编辑距离）
     *
     * @param term 词条
     * @param maxDistance 最大编辑距离
     * @return 匹配的文档
     */
    public Map<Long, Double> searchFuzzy(String term, int maxDistance) {
        Map<Long, Double> results = new HashMap<>();
        String lowerTerm = term.toLowerCase();

        for (String indexTerm : invertedIndex.keySet()) {
            int distance = levenshteinDistance(lowerTerm, indexTerm);
            if (distance <= maxDistance) {
                Map<Long, Double> termResults = search(indexTerm);
                // 距离越近，权重越高
                double similarityFactor = 1.0 - (double) distance / (maxDistance + 1);
                
                for (Map.Entry<Long, Double> entry : termResults.entrySet()) {
                    results.merge(entry.getKey(), entry.getValue() * similarityFactor, Double::sum);
                }
            }
        }

        return results;
    }

    /**
     * 短语搜索（要求词序连续）
     *
     * @param phrase 短语
     * @return 匹配的文档
     */
    public Map<Long, Double> searchPhrase(String phrase) {
        List<String> terms = tokenize(phrase);
        if (terms.isEmpty()) {
            return Collections.emptyMap();
        }

        // 先找包含所有词的文档
        Map<Long, Double> candidates = searchAnd(terms);
        Map<Long, Double> results = new HashMap<>();

        for (Long docId : candidates.keySet()) {
            if (isPhraseMatch(docId, terms)) {
                results.put(docId, candidates.get(docId) * 2.0); // 短语匹配加权
            }
        }

        return results;
    }

    /**
     * 获取文档
     *
     * @param docId 文档ID
     * @return 文档内容
     */
    public Map<String, Object> getDocument(Long docId) {
        return forwardIndex.get(docId);
    }

    /**
     * 获取所有文档ID
     *
     * @return 文档ID集合
     */
    public Set<Long> getAllDocIds() {
        return new HashSet<>(forwardIndex.keySet());
    }

    /**
     * 获取索引统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("name", name);
        stats.put("totalDocuments", totalDocuments.get());
        stats.put("totalTerms", invertedIndex.size());
        stats.put("avgDocLength", forwardIndex.values().stream()
                .mapToInt(doc -> doc.values().stream()
                        .mapToInt(v -> v.toString().length())
                        .sum())
                .average().orElse(0));
        return stats;
    }

    /**
     * 清空索引
     */
    public void clear() {
        invertedIndex.clear();
        forwardIndex.clear();
        documentFrequency.clear();
        totalDocuments.set(0);
        log.info("索引已清空: index={}", name);
    }

    // ==================== 私有方法 ====================

    /**
     * 分词
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        
        // 简单分词：按空格、标点分割，转小写
        String[] words = text.toLowerCase().split("[\\s\\p{Punct}]+");
        for (String word : words) {
            if (!word.isEmpty() && word.length() >= 1) {
                tokens.add(word);
            }
        }

        // 中文分词：按字符分割（简单实现，生产环境应使用IK等分词器）
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                tokens.add(String.valueOf(c));
            }
        }

        return tokens;
    }

    /**
     * 计算IDF（逆文档频率）
     */
    private double calculateIDF(String term) {
        AtomicLong df = documentFrequency.get(term.toLowerCase());
        if (df == null || df.get() == 0) {
            return 0;
        }
        return Math.log((double) totalDocuments.get() / df.get());
    }

    /**
     * 检查短语是否匹配
     */
    private boolean isPhraseMatch(Long docId, List<String> terms) {
        if (terms.size() == 1) {
            return true;
        }

        List<List<Integer>> positions = new ArrayList<>();
        for (String term : terms) {
            Map<Long, List<Integer>> postings = invertedIndex.get(term.toLowerCase());
            if (postings == null) {
                return false;
            }
            List<Integer> pos = postings.get(docId);
            if (pos == null) {
                return false;
            }
            positions.add(pos);
        }

        // 检查位置是否连续
        for (int firstPos : positions.get(0)) {
            boolean consecutive = true;
            for (int i = 1; i < positions.size(); i++) {
                final int expectedPos = firstPos + i;
                if (positions.get(i).stream().noneMatch(p -> p == expectedPos)) {
                    consecutive = false;
                    break;
                }
            }
            if (consecutive) {
                return true;
            }
        }

        return false;
    }

    /**
     * 通配符转正则表达式
     */
    private String wildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");
        for (char c : wildcard.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                default:
                    if (isRegexSpecialChar(c)) {
                        regex.append("\\");
                    }
                    regex.append(c);
            }
        }
        regex.append("$");
        return regex.toString();
    }

    private boolean isRegexSpecialChar(char c) {
        return "\\^$.|?*+()[]{}".indexOf(c) >= 0;
    }

    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j], dp[i][j - 1]),
                            dp[i - 1][j - 1]
                    ) + 1;
                }
            }
        }

        return dp[m][n];
    }
}
