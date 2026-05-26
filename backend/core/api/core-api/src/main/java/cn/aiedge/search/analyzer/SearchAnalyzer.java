package cn.aiedge.search.analyzer;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 搜索分析器
 * 
 * 提供：
 * - 中文分词
 * - 拼音转换
 * - 同义词扩展
 * - 停用词过滤
 * - 词干提取
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class SearchAnalyzer {

    /**
     * 停用词表
     */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            // 中文停用词
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "那", "他", "她", "它", "们", "这个", "那个", "什么", "怎么",
            // 英文停用词
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "and", "or", "but", "if", "then", "else", "when", "at", "from", "to", "of",
            "in", "on", "for", "with", "by", "about", "as", "into", "through", "during",
            "before", "after", "above", "below", "between", "under", "again", "further",
            "once", "here", "there", "all", "each", "few", "more", "most", "other", "some",
            "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very"
    ));

    /**
     * 同义词词典
     */
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>();

    static {
        // 初始化同义词
        addSynonyms("客户", "顾客", "买方", "甲方");
        addSynonyms("产品", "商品", "货物", "货品");
        addSynonyms("订单", "单子", "订单单", "采购单");
        addSynonyms("公司", "企业", "单位", "机构");
        addSynonyms("电话", "手机", "联系电话", "联系方式");
        addSynonyms("地址", "位置", "地点", "所在位置");
        addSynonyms("价格", "价钱", "金额", "费用");
        addSynonyms("客户经理", "业务员", "销售", "业务经理");
        addSynonyms("仓库", "库房", "存储", "库区");
    }

    private static void addSynonyms(String word, String... synonyms) {
        List<String> all = new ArrayList<>();
        all.add(word);
        all.addAll(Arrays.asList(synonyms));
        for (String w : all) {
            SYNONYMS.put(w, all.stream().filter(s -> !s.equals(w)).toList());
        }
    }

    /**
     * 分析文本
     *
     * @param text 文本
     * @param options 分析选项
     * @return 分析结果
     */
    public AnalysisResult analyze(String text, AnalyzeOptions options) {
        AnalysisResult result = new AnalysisResult();
        result.setOriginalText(text);

        if (text == null || text.isEmpty()) {
            return result;
        }

        // 1. 文本预处理
        String normalized = normalize(text);
        result.setNormalizedText(normalized);

        // 2. 分词
        List<Token> tokens = tokenize(normalized, options);
        result.setTokens(tokens);

        // 3. 停用词过滤
        if (options.isRemoveStopWords()) {
            tokens = filterStopWords(tokens);
            result.setFilteredTokens(tokens);
        }

        // 4. 同义词扩展
        if (options.isExpandSynonyms()) {
            Map<String, List<String>> expanded = expandSynonyms(tokens);
            result.setSynonyms(expanded);
        }

        // 5. 拼音转换
        if (options.isConvertPinyin()) {
            Map<String, String> pinyin = convertToPinyin(tokens);
            result.setPinyin(pinyin);
        }

        // 6. 提取关键词
        List<String> keywords = extractKeywords(tokens);
        result.setKeywords(keywords);

        return result;
    }

    /**
     * 快速分词
     *
     * @param text 文本
     * @return 词条列表
     */
    public List<String> quickTokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        String lower = text.toLowerCase();

        // 英文单词
        Pattern wordPattern = Pattern.compile("[a-zA-Z]+");
        Matcher matcher = wordPattern.matcher(lower);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        // 数字
        Pattern numberPattern = Pattern.compile("\\d+(\\.\\d+)?");
        matcher = numberPattern.matcher(lower);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        // 中文字符（单字分词）
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                tokens.add(String.valueOf(c));
            }
        }

        return tokens;
    }

    /**
     * 智能分词（支持中英文混合）
     *
     * @param text 文本
     * @return 词条列表
     */
    public List<String> smartTokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        Character.UnicodeScript lastScript = null;

        for (char c : text.toCharArray()) {
            Character.UnicodeScript currentScript = Character.UnicodeScript.of(c);

            // 跳过标点和空白
            if (Character.isWhitespace(c) || isPunctuation(c)) {
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString().toLowerCase());
                    buffer.setLength(0);
                }
                lastScript = null;
                continue;
            }

            // 汉字单独成词
            if (currentScript == Character.UnicodeScript.HAN) {
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString().toLowerCase());
                    buffer.setLength(0);
                }
                tokens.add(String.valueOf(c));
                lastScript = null;
            }
            // 相同脚本连续
            else if (lastScript == null || lastScript == currentScript) {
                buffer.append(c);
                lastScript = currentScript;
            }
            // 不同脚本切换
            else {
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString().toLowerCase());
                }
                buffer.setLength(1);
                buffer.setCharAt(0, c);
                lastScript = currentScript;
            }
        }

        if (buffer.length() > 0) {
            tokens.add(buffer.toString().toLowerCase());
        }

        return tokens;
    }

    /**
     * 中文分词（最大匹配算法）
     *
     * @param text 中文文本
     * @param dictionary 词典
     * @return 词条列表
     */
    public List<String> chineseTokenize(String text, Set<String> dictionary) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        int pos = 0;
        int maxLen = dictionary.stream().mapToInt(String::length).max().orElse(4);

        while (pos < text.length()) {
            // 跳过非汉字
            if (Character.UnicodeScript.of(text.charAt(pos)) != Character.UnicodeScript.HAN) {
                pos++;
                continue;
            }

            // 最大匹配
            int matched = 0;
            for (int len = Math.min(maxLen, text.length() - pos); len >= 1; len--) {
                String word = text.substring(pos, pos + len);
                if (dictionary.contains(word)) {
                    tokens.add(word);
                    matched = len;
                    break;
                }
            }

            if (matched == 0) {
                // 未匹配，单字切分
                tokens.add(String.valueOf(text.charAt(pos)));
                pos++;
            } else {
                pos += matched;
            }
        }

        return tokens;
    }

    /**
     * 文本规范化
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }

        // 转小写
        String result = text.toLowerCase();

        // 全角转半角
        result = fullWidthToHalfWidth(result);

        // 繁体转简体（简单映射）
        result = traditionalToSimplified(result);

        // 去除多余空格
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    /**
     * 全角转半角
     */
    private String fullWidthToHalfWidth(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\u3000') {
                sb.append(' ');
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 繁体转简体（简单映射）
     */
    private String traditionalToSimplified(String text) {
        Map<Character, Character> map = new HashMap<>();
        // 常见繁简对照
        map.put('國', '国');
        map.put('產', '产');
        map.put('會', '会');
        map.put('員', '员');
        map.put('務', '务');
        map.put('類', '类');
        map.put('項', '项');
        map.put('訂', '订');
        map.put('購', '购');
        map.put('貨', '货');
        map.put('單', '单');
        map.put('經', '经');
        map.put('營', '营');
        map.put('業', '业');
        map.put('務', '务');
        map.put('資', '资');
        map.put('訊', '讯');
        map.put('檔', '档');
        map.put('案', '案');
        map.put('系', '系');
        map.put('統', '统');

        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(map.getOrDefault(c, c));
        }
        return sb.toString();
    }

    /**
     * 分词（带位置信息）
     */
    private List<Token> tokenize(String text, AnalyzeOptions options) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;

        // 使用智能分词
        List<String> words = smartTokenize(text);

        for (String word : words) {
            Token token = new Token();
            token.setTerm(word);
            token.setPosition(position++);
            token.setStartOffset(text.indexOf(word));
            token.setEndOffset(token.getStartOffset() + word.length());
            token.setType(getTokenType(word));
            tokens.add(token);
        }

        return tokens;
    }

    /**
     * 过滤停用词
     */
    private List<Token> filterStopWords(List<Token> tokens) {
        return tokens.stream()
                .filter(t -> !STOP_WORDS.contains(t.getTerm().toLowerCase()))
                .toList();
    }

    /**
     * 同义词扩展
     */
    private Map<String, List<String>> expandSynonyms(List<Token> tokens) {
        Map<String, List<String>> expanded = new HashMap<>();
        for (Token token : tokens) {
            List<String> synonyms = SYNONYMS.get(token.getTerm());
            if (synonyms != null && !synonyms.isEmpty()) {
                expanded.put(token.getTerm(), synonyms);
            }
        }
        return expanded;
    }

    /**
     * 转换为拼音
     */
    private Map<String, String> convertToPinyin(List<Token> tokens) {
        Map<String, String> pinyinMap = new HashMap<>();
        PinyinConverter converter = new PinyinConverter();

        for (Token token : tokens) {
            String pinyin = converter.toPinyin(token.getTerm());
            if (pinyin != null && !pinyin.isEmpty()) {
                pinyinMap.put(token.getTerm(), pinyin);
            }
        }

        return pinyinMap;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(List<Token> tokens) {
        // 简单实现：返回非停用词的词条
        return tokens.stream()
                .filter(t -> !STOP_WORDS.contains(t.getTerm().toLowerCase()))
                .filter(t -> t.getTerm().length() >= 2 || isChinese(t.getTerm().charAt(0)))
                .map(Token::getTerm)
                .distinct()
                .toList();
    }

    /**
     * 判断是否为标点
     */
    private boolean isPunctuation(char c) {
        return Character.getType(c) == Character.OTHER_PUNCTUATION
                || Character.getType(c) == Character.CONNECTOR_PUNCTUATION
                || Character.getType(c) == Character.DASH_PUNCTUATION
                || Character.getType(c) == Character.START_PUNCTUATION
                || Character.getType(c) == Character.END_PUNCTUATION
                || Character.getType(c) == Character.INITIAL_QUOTE_PUNCTUATION
                || Character.getType(c) == Character.FINAL_QUOTE_PUNCTUATION;
    }

    /**
     * 判断是否为中文
     */
    private boolean isChinese(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }

    /**
     * 获取词条类型
     */
    private String getTokenType(String term) {
        if (term.matches("\\d+")) {
            return "NUMBER";
        } else if (term.matches("[a-zA-Z]+")) {
            return "WORD";
        } else if (term.matches("[\\u4e00-\\u9fa5]+")) {
            return "CHINESE";
        } else {
            return "MIXED";
        }
    }

    // ==================== 内部类 ====================

    /**
     * 词条
     */
    @lombok.Data
    public static class Token {
        private String term;
        private int position;
        private int startOffset;
        private int endOffset;
        private String type;
    }

    /**
     * 分析选项
     */
    @lombok.Data
    public static class AnalyzeOptions {
        private boolean removeStopWords = true;
        private boolean expandSynonyms = false;
        private boolean convertPinyin = false;
        private int maxTokenLength = 100;

        public static AnalyzeOptions DEFAULT = new AnalyzeOptions();
        public static AnalyzeOptions FULL = new AnalyzeOptions() {{
            setRemoveStopWords(true);
            setExpandSynonyms(true);
            setConvertPinyin(true);
        }};
    }

    /**
     * 分析结果
     */
    @lombok.Data
    public static class AnalysisResult {
        private String originalText;
        private String normalizedText;
        private List<Token> tokens;
        private List<Token> filteredTokens;
        private Map<String, List<String>> synonyms;
        private Map<String, String> pinyin;
        private List<String> keywords;
    }

    /**
     * 拼音转换器
     */
    @lombok.extern.slf4j.Slf4j
    public static class PinyinConverter {
        // 简化实现：只包含常用汉字的拼音
        private static final Map<Character, String> PINYIN_MAP = new HashMap<>();

        static {
            // 常用汉字拼音映射
            PINYIN_MAP.put('客', "ke");
            PINYIN_MAP.put('户', "hu");
            PINYIN_MAP.put('产', "chan");
            PINYIN_MAP.put('品', "pin");
            PINYIN_MAP.put('订', "ding");
            PINYIN_MAP.put('单', "dan");
            PINYIN_MAP.put('公', "gong");
            PINYIN_MAP.put('司', "si");
            PINYIN_MAP.put('名', "ming");
            PINYIN_MAP.put('称', "cheng");
            PINYIN_MAP.put('电', "dian");
            PINYIN_MAP.put('话', "hua");
            PINYIN_MAP.put('地', "di");
            PINYIN_MAP.put('址', "zhi");
            PINYIN_MAP.put('价', "jia");
            PINYIN_MAP.put('格', "ge");
            PINYIN_MAP.put('金', "jin");
            PINYIN_MAP.put('额', "e");
            PINYIN_MAP.put('仓', "cang");
            PINYIN_MAP.put('库', "ku");
            PINYIN_MAP.put('销', "xiao");
            PINYIN_MAP.put('售', "shou");
            PINYIN_MAP.put('采', "cai");
            PINYIN_MAP.put('购', "gou");
            PINYIN_MAP.put('库', "ku");
            PINYIN_MAP.put('存', "cun");
            PINYIN_MAP.put('数', "shu");
            PINYIN_MAP.put('量', "liang");
            PINYIN_MAP.put('备', "bei");
            PINYIN_MAP.put('注', "zhu");
            PINYIN_MAP.put('状', "zhuang");
            PINYIN_MAP.put('态', "tai");
            PINYIN_MAP.put('时', "shi");
            PINYIN_MAP.put('间', "jian");
            PINYIN_MAP.put('日', "ri");
            PINYIN_MAP.put('期', "qi");
        }

        public String toPinyin(String text) {
            if (text == null || text.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                String pinyin = PINYIN_MAP.get(c);
                if (pinyin != null) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(pinyin);
                } else if (Character.isLetter(c) && c < 128) {
                    sb.append(Character.toLowerCase(c));
                }
            }

            return sb.toString();
        }

        public String toPinyinFirstLetter(String text) {
            String pinyin = toPinyin(text);
            if (pinyin == null || pinyin.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (String part : pinyin.split(" ")) {
                if (!part.isEmpty()) {
                    sb.append(part.charAt(0));
                }
            }
            return sb.toString();
        }
    }
}
