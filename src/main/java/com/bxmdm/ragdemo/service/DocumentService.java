package com.bxmdm.ragdemo.service;

import com.bxmdm.ragdemo.reader.ParagraphTextReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * @Description: 文档服务
 * @Author: jay
 * @Date: 2024/3/18 10:02
 * @Version: 1.0
 */
@Service
public class DocumentService {

	@Autowired
	private VectorStore vectorStore;

	@Autowired
	private ChatClient ollamaChatClient;

	private static final String PATH = "D:\\demo\\ai\\path\\";

	/**
	 * 使用spring ai解析txt文档
	 *
	 * @param file
	 * @throws MalformedURLException
	 */
	public void uploadDocument(MultipartFile file) {
		//保存file到本地
		String textResource = file.getOriginalFilename();
		//判断文件是否是TXT
		if (!textResource.endsWith(".txt")) {
			throw new RuntimeException("只支持txt格式文件");
		}
		String filepath = PATH + textResource;
		File file1 = new File(filepath);
		if (file1.exists()) {
			throw new RuntimeException("文件已存在");
		}
		try {
			file.transferTo(file1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<Document> documentList = paragraphTextReader(file1);
		vectorStore.add(documentList);
	}

	public void analysisDocument() {
		List<Document> documentList = paragraphTextReader();
		vectorStore.add(documentList);
	}

	private List<Document> paragraphTextReader(File file) {
		List<Document> docs = null;
		try {
			ParagraphTextReader reader = new ParagraphTextReader(new FileUrlResource(file.toURI().toURL()), 5);
			reader.getCustomMetadata().put("filename", file.getName());
			reader.getCustomMetadata().put("filepath", file.getAbsolutePath());
			docs = reader.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return docs;
	}

	@Autowired
	private DocTikaDocumentReader docTikaDocumentReader;

	public void parseDoc() {
		List<Document> documents = docTikaDocumentReader.loadText();
		vectorStore.add(documents);
	}

	public void parseCameraAQ() throws IOException {
		List<Document> documents = new ArrayList<>();

		ClassPathResource resource = new ClassPathResource("sock售后相关问题.xlsx");
		Workbook workbook = WorkbookFactory.create(resource.getInputStream());
		Sheet sheet = workbook.getSheetAt(0);

		Iterator<Row> rowIterator = sheet.rowIterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			if (row.getRowNum() == 0) continue; // Skip header row
			Cell cell0 = row.getCell(0);
			if (cell0 == null) {
				continue;
			}
			String question = cell0.getStringCellValue();
			Cell cell1 = row.getCell(1);
			if (cell1 == null) {
				continue;
			}
			String answer = cell1.getStringCellValue();
			if (!question.isEmpty() && !answer.isEmpty()) {
				String content = "Q: " + question + "\nA: " + answer;
				Document doc = new Document(content);
				documents.add(doc);
			}
		}
		vectorStore.add(documents);
	}

	private List<Document> paragraphTextReader() {
		List<Document> docs = null;
		try {
			ClassPathResource resource = new ClassPathResource("Spring AI简介.txt");

			ParagraphTextReader reader = new ParagraphTextReader(resource, 5);
			reader.getCustomMetadata().put("filename", resource.getFilename());
			reader.getCustomMetadata().put("filepath", resource.getPath());
			docs = reader.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return docs;
	}

	/**
	 * 根据关键词搜索向量库
	 *
	 * @param keyword 关键词
	 * @return 文档列表
	 */
	public List<Document> search(String keyword) {
		return vectorStore.similaritySearch(SearchRequest.builder().topK(3)
				.similarityThreshold(0.6)
				.query(keyword)
				.build());
	}

	/**
	 * 问答,根据输入内容回答
	 *
	 * @param message 输入内容
	 * @return 回答内容
	 */
	public Flux<String> chat(String message,String translatedMessage) {
		// 1. 从 VectorStore 中检索相关文档
		List<Document> documents = search(translatedMessage);

		// 2. 拼接文档内容为 context 字符串
		String context = documents.stream()
				.map(Document::getText)
				.collect(Collectors.joining("\n\n"));

		// 3. 填充 PromptTemplate
		PromptTemplate template = new PromptTemplate("""
			你将作为一名Sense-U产品说明的智能助理，对于用户的问题作出解答。
			请基于以下文档片段回答问题。注意文档片段可能来自不同的文档，请综合考虑所有相关信息。
			如果上下文中没有相关信息，请说明无法找到相关信息。
			
			上下文：
			{context}
			
			问题：
			{question}
			
			请提供准确、详细的回答，并在可能的情况下引用具体的文档片段：""");

		Map<String, Object> variables = Map.of(
				"context", context,
				"question", message
		);
		Prompt prompt = template.create(variables);
		//封装prompt并调用大模型
		return ollamaChatClient
				.prompt(prompt)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "007"))
				.stream()
				.content();
	}

	/**
	 * 获取prompt
	 *
	 * @param message 提问内容
	 * @param context 上下文
	 * @return prompt
	 */
	private String getChatPrompt2String(String message, String context) {
		String promptText = """
				请用仅用以下内容回答"%s":
				%s
				""";
		return String.format(promptText, message, context);
	}
}
