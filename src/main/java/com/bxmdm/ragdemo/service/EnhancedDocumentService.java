package com.bxmdm.ragdemo.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EnhancedDocumentService {

    // 分块配置
    private static final int MAX_CHUNK_SIZE = 1000;  // 最大字符数
    private static final int MIN_CHUNK_SIZE = 100;   // 最小字符数
    private static final int OVERLAP_SIZE = 50;      // 重叠字符数
    private static final int MAX_CHUNK_TOKENS = 300; // 最大token数（估算）
    
    private final Tika tika;
    
    public EnhancedDocumentService(Tika tika) {
        this.tika = tika;
    }

    // 基础文档解析
    public List<Document> parseDocument(InputStream inputStream, String filename) {
        try {
            // 使用TikaDocumentReader解析
            Resource resource = new InputStreamResource(inputStream);
            List<Document> documents = new TikaDocumentReader(resource).get();
            
            // 为每个文档添加元数据
            for (Document doc : documents) {
                Map<String, Object> metadata = doc.getMetadata();
                metadata.put("source_filename", filename);
                metadata.put("parsed_at", System.currentTimeMillis());
                metadata.put("parser", "TikaDocumentReader");
            }
            
            return documents;
            
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    // 高级文档解析，包含更多元数据提取
    public List<Document> parseDocumentWithMetadata(InputStream inputStream, String filename) {
        try {
            // 1. 先提取元数据
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
            
            // 2. 解析文档内容和元数据
            String content = tika.parseToString(inputStream, metadata);
            
            // 3. 构建增强的元数据
            Map<String, Object> docMetadata = extractEnhancedMetadata(metadata, filename);
            
            // 4. 创建文档对象
            Document document = new Document(content, docMetadata);
            
            return Arrays.asList(document);
            
        } catch (Exception e) {
            throw new RuntimeException("增强文档解析失败: " + e.getMessage(), e);
        }
    }

    // 批量解析多个文档
    public List<Document> parseMultipleDocuments(List<MultipartFile> files) {
        List<Document> allDocuments = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                List<Document> docs = parseDocument(file.getInputStream(), 
                                                  file.getOriginalFilename());
                allDocuments.addAll(docs);
            } catch (IOException e) {
                System.err.println("解析文件失败: " + file.getOriginalFilename() + 
                                 ", 错误: " + e.getMessage());
            }
        }
        
        return allDocuments;
    }

    // 提取增强的元数据
    private Map<String, Object> extractEnhancedMetadata(Metadata tikaMetadata, String filename) {
        Map<String, Object> metadata = new HashMap<>();
        
        // 基础信息
        metadata.put("filename", filename);
        metadata.put("parsed_at", String.valueOf(System.currentTimeMillis()));
        
        // 文档属性
        String title = tikaMetadata.get(TikaCoreProperties.TITLE);
        metadata.put("title", title != null ? title : "未提供标题");
        String author = tikaMetadata.get(TikaCoreProperties.CREATOR);
        metadata.put("author", author != null ? author : "未提供作者");
        String subject = tikaMetadata.get(TikaCoreProperties.SUBJECT);
        metadata.put("subject", subject != null ? subject : "未提供主题");
//        metadata.put("keywords", tikaMetadata.get(TikaCoreProperties.KEYWORDS));
        
        // 技术属性
        String contentType = tikaMetadata.get(HttpHeaders.CONTENT_TYPE);
        metadata.put("content_type", contentType != null ? contentType : "未提供内容类型");
        String encoding = tikaMetadata.get(HttpHeaders.CONTENT_ENCODING);
        metadata.put("content_encoding", encoding != null ? encoding : "未提供编码");
        String contentLength = tikaMetadata.get(HttpHeaders.CONTENT_LENGTH);
        metadata.put("content_length", contentLength != null ? contentLength : "未提供内容长度");
        
        // 创建和修改时间
        String created = tikaMetadata.get(TikaCoreProperties.CREATED);
        metadata.put("created_date", created != null ? created : "未提供创建时间");
        String modified = tikaMetadata.get(TikaCoreProperties.MODIFIED);
        metadata.put("modified_date", modified != null ? modified : "未提供修改时间");
        
        // Office文档特有属性
        String application = tikaMetadata.get(OfficeOpenXMLExtended.APPLICATION);
        metadata.put("application_name", application != null ? application : "未提供应用程序");
        String appVersion = tikaMetadata.get(OfficeOpenXMLExtended.APP_VERSION);
        metadata.put("application_version", appVersion != null ? appVersion : "未提供应用程序版本");
        String company = tikaMetadata.get(OfficeOpenXMLExtended.COMPANY);
        metadata.put("company", company != null ? company : "未提供公司信息");
        
        // 页数和字数（如果可用）
        String pages = tikaMetadata.get(PagedText.N_PAGES);
        metadata.put("page_count", pages != null ? pages : "未提供页数");
        String totalTime = tikaMetadata.get(OfficeOpenXMLExtended.TOTAL_TIME);
        metadata.put("word_count", totalTime != null ? totalTime : "未提供总时间");
        
        return metadata;
    }

    // 检测文档类型
    public String detectDocumentType(InputStream inputStream) {
        try {
            return tika.detect(inputStream);
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    // 验证文档是否可解析
    public boolean isDocumentSupported(String filename) {
        String contentType = tika.detect(filename);
        return contentType.startsWith("application/msword") || 
               contentType.startsWith("application/vnd.openxmlformats") ||
               contentType.startsWith("application/pdf") ||
               contentType.startsWith("text/");
    }

    // 多策略分块方法
    public List<Document> chunkDocumentSmart(String content, String documentId,
                                             Map<String, Object> baseMetadata) {

        // 1. 预处理文本
        String cleanContent = preprocessText(content);

        // 2. 尝试按语义边界分块
        List<Document> chunks = chunkBySemanticBoundaries(cleanContent, documentId, baseMetadata);

        // 3. 如果语义分块失败或块太大，使用滑动窗口分块
        if (chunks.isEmpty() || hasOversizedChunks(chunks)) {
            chunks = chunkBySlidingWindow(cleanContent, documentId, baseMetadata);
        }

        // 4. 后处理和验证
        return postProcessChunks(chunks);
    }

    // 文本预处理
    private String preprocessText(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        return content
                // 规范化换行符
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                // 去除多余的空格
                .replaceAll("[ \\t]+", " ")
                // 去除多余的换行
                .replaceAll("\\n{3,}", "\n\n")
                // 去除首尾空白
                .trim();
    }

    // 按语义边界分块（推荐方式）
    public List<Document> chunkBySemanticBoundaries(String content, String documentId,
                                                    Map<String, Object> baseMetadata) {
        List<Document> chunks = new ArrayList<>();

        // 1. 按大标题分割（通常是章节）
        List<Section> sections = splitByHeaders(content);

        int globalChunkIndex = 0;

        for (Section section : sections) {
            // 2. 对每个章节进行进一步分块
            List<Document> sectionChunks = chunkSection(section, documentId,
                    baseMetadata, globalChunkIndex);
            chunks.addAll(sectionChunks);
            globalChunkIndex += sectionChunks.size();
        }

        return chunks;
    }

    // 按标题分割文档
    private List<Section> splitByHeaders(String content) {
        List<Section> sections = new ArrayList<>();

        // 定义标题模式（可根据实际文档调整）
        Pattern headerPattern = Pattern.compile(
                "(?m)^((?:第[一二三四五六七八九十\\d]+[章节]|[\\d]+\\.|[一二三四五六七八九十]、|[1-9]\\d*\\s*[、.])" +
                        "|(?:^[^\\n]{1,50}[:：]\\s*$)|(?:^#{1,6}\\s+.+$))"
        );

        Matcher matcher = headerPattern.matcher(content);
        List<Integer> headerPositions = new ArrayList<>();
        List<String> headerTexts = new ArrayList<>();

        // 找到所有标题位置
        while (matcher.find()) {
            headerPositions.add(matcher.start());
            headerTexts.add(matcher.group().trim());
        }

        // 如果没有找到标题，整个文档作为一个section
        if (headerPositions.isEmpty()) {
            sections.add(new Section("", content, 0));
            return sections;
        }

        // 按标题分割内容
        for (int i = 0; i < headerPositions.size(); i++) {
            int start = headerPositions.get(i);
            int end = (i + 1 < headerPositions.size()) ?
                    headerPositions.get(i + 1) : content.length();

            String sectionContent = content.substring(start, end).trim();
            sections.add(new Section(headerTexts.get(i), sectionContent, i));
        }

        return sections;
    }

    // 对单个章节进行分块
    private List<Document> chunkSection(Section section, String documentId,
                                        Map<String, Object> baseMetadata, int startIndex) {
        List<Document> chunks = new ArrayList<>();
        String content = section.getContent();

        // 如果章节内容较短，直接作为一个chunk
        if (content.length() <= MAX_CHUNK_SIZE) {
            Map<String, Object> metadata = createChunkMetadata(baseMetadata, documentId,
                    startIndex, section);
            chunks.add(new Document(content, metadata));
            return chunks;
        }

        // 按段落分块
        String[] paragraphs = content.split("\\n\\s*\\n");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = startIndex;

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 检查是否需要创建新的chunk
            if (shouldCreateNewChunk(currentChunk.toString(), paragraph)) {
                if (currentChunk.length() > 0) {
                    Map<String, Object> metadata = createChunkMetadata(baseMetadata, documentId,
                            chunkIndex++, section);
                    chunks.add(new Document(currentChunk.toString(), metadata));

                    // 添加重叠内容
                    currentChunk = new StringBuilder(getOverlapContent(currentChunk.toString()));
                }
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        // 处理最后一个chunk
        if (currentChunk.length() > 0) {
            Map<String, Object> metadata = createChunkMetadata(baseMetadata, documentId,
                    chunkIndex, section);
            chunks.add(new Document(currentChunk.toString(), metadata));
        }

        return chunks;
    }

    // 滑动窗口分块（备用方案）
    public List<Document> chunkBySlidingWindow(String content, String documentId,
                                               Map<String, Object> baseMetadata) {
        List<Document> chunks = new ArrayList<>();

        // 按句子分割
        String[] sentences = splitIntoSentences(content);

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            // 检查添加当前句子是否会超出限制
            if (currentChunk.length() + sentence.length() > MAX_CHUNK_SIZE &&
                    currentChunk.length() > MIN_CHUNK_SIZE) {

                // 创建chunk
                Map<String, Object> metadata = createChunkMetadata(baseMetadata, documentId,
                        chunkIndex++, null);
                chunks.add(new Document(currentChunk.toString(), metadata));

                // 保留重叠内容
                currentChunk = new StringBuilder(getOverlapContent(currentChunk.toString()));
            }

            currentChunk.append(sentence).append(" ");
        }

        // 处理最后一个chunk
        if (currentChunk.length() > MIN_CHUNK_SIZE) {
            Map<String, Object> metadata = createChunkMetadata(baseMetadata, documentId,
                    chunkIndex, null);
            chunks.add(new Document(currentChunk.toString(), metadata));
        }

        return chunks;
    }

    // 句子分割
    private String[] splitIntoSentences(String text) {
        // 中英文句子分割模式
        return text.split("(?<=[。！？.!?])\\s*(?=[A-Z\\u4e00-\\u9fa5])");
    }

    // 判断是否需要创建新的chunk
    private boolean shouldCreateNewChunk(String currentChunk, String newParagraph) {
        int newLength = currentChunk.length() + newParagraph.length();
        return newLength > MAX_CHUNK_SIZE && currentChunk.length() > MIN_CHUNK_SIZE;
    }

    // 获取重叠内容
    private String getOverlapContent(String content) {
        if (content.length() <= OVERLAP_SIZE) {
            return content;
        }

        // 从末尾开始找到合适的分割点（句子边界）
        String overlap = content.substring(content.length() - OVERLAP_SIZE);
        int lastSentenceEnd = Math.max(
                overlap.lastIndexOf("。"),
                Math.max(overlap.lastIndexOf("！"), overlap.lastIndexOf("？"))
        );

        if (lastSentenceEnd > 0) {
            return content.substring(content.length() - OVERLAP_SIZE + lastSentenceEnd + 1);
        }

        return overlap;
    }

    // 创建chunk元数据
    private Map<String, Object> createChunkMetadata(Map<String, Object> baseMetadata,
                                                    String documentId, int chunkIndex,
                                                    Section section) {
        Map<String, Object> metadata = new HashMap<>(baseMetadata);
        metadata.put("document_id", documentId);
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_type", section != null ? "semantic" : "sliding_window");

        if (section != null) {
            metadata.put("section_title", section.getTitle());
            metadata.put("section_index", section.getIndex());
        }

        return metadata;
    }

    // 检查是否有过大的chunks
    private boolean hasOversizedChunks(List<Document> chunks) {
        return chunks.stream().anyMatch(doc -> doc.getText().length() > MAX_CHUNK_SIZE * 1.5);
    }

    // 后处理chunks
    private List<Document> postProcessChunks(List<Document> chunks) {
        return chunks.stream()
                .filter(doc -> doc.getText().trim().length() >= MIN_CHUNK_SIZE)
                .map(this::enhanceChunkMetadata)
                .collect(Collectors.toList());
    }

    // 增强chunk元数据
    private Document enhanceChunkMetadata(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        String content = doc.getText();

        // 添加内容统计
        metadata.put("chunk_length", content.length());
        metadata.put("word_count", countWords(content));
        metadata.put("estimated_tokens", estimateTokens(content));
        metadata.put("created_at", String.valueOf(System.currentTimeMillis()));

        return new Document(content, metadata);
    }

    // 词数统计
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;

        // 中文字符数 + 英文单词数
        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int englishWords = text.replaceAll("[\\u4e00-\\u9fa5]", " ")
                .trim()
                .split("\\s+").length;

        return chineseChars + englishWords;
    }

    // 估算token数
    private int estimateTokens(String text) {
        // 粗略估算：中文1字符≈1token，英文1单词≈1.3token
        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int englishWords = countWords(text) - chineseChars;

        return chineseChars + (int)(englishWords * 1.3);
    }

    // 内部类：文档节
    private static class Section {
        private final String title;
        private final String content;
        private final int index;

        public Section(String title, String content, int index) {
            this.title = title;
            this.content = content;
            this.index = index;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public int getIndex() { return index; }
    }
}