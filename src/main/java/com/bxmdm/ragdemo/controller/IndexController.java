package com.bxmdm.ragdemo.controller;

import com.bxmdm.ragdemo.service.AliService;
import com.bxmdm.ragdemo.service.DocumentService;
import com.bxmdm.ragdemo.service.EnhancedDocumentService;
import com.bxmdm.ragdemo.service.EnhancedRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/vi/rag")
@Tag(name = "RAG demo")
public class IndexController {

	@Autowired
	private DocumentService documentService;

	@Autowired
	private AliService aliService;

	@Autowired
	private EnhancedRagService docService;


	@Operation(summary = "上传文档")
	@PostMapping("/upload")
	public ResponseEntity upload(@RequestBody MultipartFile file) {
		documentService.uploadDocument(file);
		return ResponseEntity.ok("success");
	}

	@Operation(summary = "解析excel文档")
	@GetMapping("/analysisExcel")
	public ResponseEntity analysis() {
        try {
            documentService.parseCameraAQ();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("success");
	}

	@Operation(summary = "解析doc文档")
	@GetMapping("/analysisDoc")
	public ResponseEntity analysisDoc() {
        try {
            docService.indexDocumentWithSmartChunking();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("success");
	}

	@Operation(summary = "搜索文档")
	@GetMapping("/search")
	public ResponseEntity<List<Document>> searchDoc(@RequestParam String keyword) {
		String translated = aliService.translate(keyword);
		return ResponseEntity.ok(documentService.search(translated));
	}

	@CrossOrigin(origins = "*")
	@Operation(summary = "问答文档")
//	@GetMapping(value = "/chat",produces = "text/html;charset=UTF-8")
	@GetMapping(value = "/chat",produces = "text/event-stream; charset=UTF-8")
	public Flux<String> chat(@RequestParam String message) {
		String translated = aliService.translate(message);
//		return documentService.chat(message,translated);
		return docService.queryWithChunkContext(message, translated);
	}
}
