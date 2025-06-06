package com.bxmdm.ragdemo.controller;

import com.bxmdm.ragdemo.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/vi/rag")
@Tag(name = "RAG demo")
public class IndexController {

	@Autowired
	private DocumentService documentService;


	@Operation(summary = "上传文档")
	@PostMapping("/upload")
	public ResponseEntity upload(@RequestBody MultipartFile file) {
		documentService.uploadDocument(file);
		return ResponseEntity.ok("success");
	}

	@Operation(summary = "解析文档")
	@GetMapping("/analysis")
	public ResponseEntity analysis() {
        try {
            documentService.parseAQ();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//		documentService.analysisDocument();
        return ResponseEntity.ok("success");
	}

	@Operation(summary = "搜索文档")
	@GetMapping("/search")
	public ResponseEntity<List<Document>> searchDoc(@RequestParam String keyword) {
		return ResponseEntity.ok(documentService.search(keyword));
	}

	@CrossOrigin(origins = "*")
	@Operation(summary = "问答文档")
//	@GetMapping(value = "/chat",produces = "text/html;charset=UTF-8")
	@GetMapping(value = "/chat",produces = "text/event-stream; charset=UTF-8")
	public Flux<String> chat(@RequestParam String message) {
		return documentService.chat(message);
	}


}
