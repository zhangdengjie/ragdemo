package com.bxmdm.ragdemo.service;

import static com.bxmdm.ragdemo.reader.ParagraphTextReader.END_PARAGRAPH_NUMBER;
import static com.bxmdm.ragdemo.reader.ParagraphTextReader.START_PARAGRAPH_NUMBER;

import cn.hutool.core.util.ArrayUtil;
import com.bxmdm.ragdemo.reader.ParagraphTextReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
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

	public void parseExcel() throws IOException, TikaException, SAXException {
		// 1. 解析 Excel 内容
		AutoDetectParser parser = new AutoDetectParser();
		BodyContentHandler handler = new BodyContentHandler(-1); // no content length limit
		Metadata metadata = new Metadata();

		ClassPathResource resource = new ClassPathResource("Baby售后相关问题.xlsx");
		parser.parse(resource.getInputStream(), handler, metadata);

		String content = handler.toString();

		ParagraphTextReader reader = new ParagraphTextReader(resource, 5);
		reader.setContent(content);
		vectorStore.add(reader.get());
//		// 2. 构建 Document 对象
//		Document doc = new Document(
//				UUID.randomUUID().toString(),  // 可自定义 ID
//				content,
//				Collections.singletonMap("filename", resource.getFilename())
//		);
//		vectorStore.add(Collections.singletonList(doc));
	}

	public void parseAQ() throws IOException {
		List<Document> documents = new ArrayList<>();

		ClassPathResource resource = new ClassPathResource("cam售后相关问题 .xlsx");
		Workbook workbook = WorkbookFactory.create(resource.getInputStream());
		Sheet sheet = workbook.getSheetAt(0);

		for (Row row : sheet) {
			Cell questionCell = row.getCell(0);
			Cell answerCell = row.getCell(1);

			if (questionCell == null || answerCell == null) continue;

			String question = questionCell.getStringCellValue().trim();
			String answer = answerCell.getStringCellValue().trim();

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
	 * 合并文档列表
	 *
	 * @param documentList 文档列表
	 * @return 合并后的文档列表
	 */
	private List<Document> mergeDocuments(List<Document> documentList) {
		List<Document> mergeDocuments = new ArrayList();
		//根据文档来源进行分组
		Map<String, List<Document>> documentMap = documentList.stream().collect(Collectors.groupingBy(item -> ((String) item.getMetadata().get("source"))));
		for (Entry<String, List<Document>> docListEntry : documentMap.entrySet()) {
			//获取最大的段落结束编码
			int maxParagraphNum = (int) docListEntry.getValue()
					.stream().max(Comparator.comparing(item -> ((int) item.getMetadata().get(END_PARAGRAPH_NUMBER)))).get().getMetadata().get(END_PARAGRAPH_NUMBER);
			//根据最大段落结束编码构建一个用于合并段落的空数组
			String[] paragraphs = new String[maxParagraphNum];
			//用于获取最小段落开始编码
			int minParagraphNum = maxParagraphNum;
			for (Document document : docListEntry.getValue()) {
				//文档内容根据回车进行分段
				String[] tempPs = document.getFormattedContent().split("\n");
				//获取文档开始段落编码
				int startParagraphNumber = (int) document.getMetadata().get(START_PARAGRAPH_NUMBER);
				if (minParagraphNum > startParagraphNumber) {
					minParagraphNum = startParagraphNumber;
				}
				//将文档段落列表拷贝到合并段落数组中
				System.arraycopy(tempPs, 0, paragraphs, startParagraphNumber - 1, tempPs.length);
			}
			//合并段落去除空值,并组成文档内容
			Document mergeDoc = new Document(ArrayUtil.join(ArrayUtil.removeNull(paragraphs), "\n"));
			//合并元数据
			mergeDoc.getMetadata().putAll(docListEntry.getValue().get(0).getMetadata());
			//设置元数据:开始段落编码
			mergeDoc.getMetadata().put(START_PARAGRAPH_NUMBER, minParagraphNum);
			//设置元数据:结束段落编码
			mergeDoc.getMetadata().put(END_PARAGRAPH_NUMBER, maxParagraphNum);
			mergeDocuments.add(mergeDoc);
		}
		return mergeDocuments;
	}

	/**
	 * 根据关键词搜索向量库
	 *
	 * @param keyword 关键词
	 * @return 文档列表
	 */
	public List<Document> search(String keyword) {
		return Collections.singletonList(vectorStore.similaritySearch(keyword).stream().sorted(new Comparator<Document>() {
			@Override
			public int compare(Document o1, Document o2) {
				return o2.getScore() - o1.getScore() > 0 ? 1 : -1;
			}
		}).findFirst().get());
//		return mergeDocuments(documentList);
	}

	/**
	 * 问答,根据输入内容回答
	 *
	 * @param message 输入内容
	 * @return 回答内容
	 */
	public Flux<String> chat(String message) {
		//查询获取文档信息
		List<Document> documents = search(message);

		//提取文本内容
		String content = documents.stream()
				.map(new Function<Document, String>() {
					@Override
					public String apply(Document document) {
						return document.getText();
					}
				})
				.collect(Collectors.joining("\n"));

		//封装prompt并调用大模型
		return ollamaChatClient.prompt().user(getChatPrompt2String(message, content)).stream().content();
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
