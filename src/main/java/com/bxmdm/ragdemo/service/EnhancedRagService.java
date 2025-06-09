package com.bxmdm.ragdemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnhancedRagService {

    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private EnhancedDocumentService documentService;
    @Autowired
    private ChatClient ollamaChatClient;

    public void indexDocumentWithSmartChunking() throws IOException {
        ClassPathResource resource = new ClassPathResource("Camera HD.docx");
        String filename = resource.getFilename();
        String documentId = UUID.randomUUID().toString();
        
        // 1. 使用Tika解析文档
        List<Document> documents = documentService.parseDocumentWithMetadata(
                resource.getInputStream(), filename);
        
        // 2. 智能分块处理
        List<Document> allChunks = new ArrayList<>();
        for (Document doc : documents) {
            List<Document> chunks = documentService.chunkDocumentSmart(
                doc.getText(), documentId, doc.getMetadata());
            allChunks.addAll(chunks);
        }
        
        // 3. 存入向量数据库
        vectorStore.add(allChunks);
        
        // 4. 记录分块统计信息
        logChunkingStats(filename, allChunks);
    }

    private void logChunkingStats(String filename, List<Document> chunks) {
        int totalLength = chunks.stream().mapToInt(doc -> doc.getText().length()).sum();
        int avgLength = totalLength / chunks.size();
        int minLength = chunks.stream().mapToInt(doc -> doc.getText().length()).min().orElse(0);
        int maxLength = chunks.stream().mapToInt(doc -> doc.getText().length()).max().orElse(0);
        
        System.out.println(String.format(
            "文档 %s 分块统计: 总块数=%d, 平均长度=%d, 最小长度=%d, 最大长度=%d",
            filename, chunks.size(), avgLength, minLength, maxLength
        ));
    }

    // 增强的查询，可以基于chunk元数据进行更精确的检索
    public Flux<String> queryWithChunkContext(String question,String translatedQuestion) {
        // 1. 向量搜索
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder().topK(8)
                        .query(translatedQuestion)
                        .build()
        );
        
        // 2. 按文档分组和重排序
        Map<String, List<Document>> docGroups = groupChunksByDocument(similarDocs);
        
        // 3. 构建上下文，保持文档结构
        StringBuilder context = new StringBuilder();

        for (Map.Entry<String, List<Document>> entry : docGroups.entrySet()) {
            String docId = entry.getKey();
            List<Document> chunks = entry.getValue();
            
            // 按chunk_index排序
            chunks.sort(Comparator.comparing(doc -> ((Long) doc.getMetadata().getOrDefault("chunk_index", 0L)).intValue()));
            
            // 构建文档片段
            context.append("【文档片段】\n");
            for (Document chunk : chunks) {
                context.append(chunk.getText()).append("\n");
            }
            context.append("\n");
        }
        
        // 4. 填充 PromptTemplate
        PromptTemplate template = new PromptTemplate("""
			你将作为一名Sense-U产品说明的智能助理，对于用户的问题作出解答。
			请基于以下文档片段回答问题。注意文档片段可能来自不同的文档，请综合考虑所有相关信息。
			
			上下文：
			{context}
			
			问题：
			{question}
			
			请提供准确、详细的回答：""");

        Map<String, Object> variables = Map.of(
                "context", context.toString(),
                "question", question
        );
        Prompt prompt = template.create(variables);
        //5. 生成回答 封装prompt并调用大模型
        return ollamaChatClient
                .prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "007"))
                .stream()
                .content();
    }

    private Map<String, List<Document>> groupChunksByDocument(List<Document> chunks) {
        return chunks.stream()
            .collect(Collectors.groupingBy(doc ->
                (String) doc.getMetadata().getOrDefault("document_id", "unknown")));
    }
}