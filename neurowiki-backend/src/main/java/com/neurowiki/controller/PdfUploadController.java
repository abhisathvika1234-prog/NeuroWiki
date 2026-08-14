package com.neurowiki.controller;

import com.neurowiki.dto.DocumentResponse;
import com.neurowiki.service.PdfService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pdf")
public class PdfUploadController {

    private final PdfService pdfService;

    public PdfUploadController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadPdf(@RequestParam("file") MultipartFile file) {
        DocumentResponse response = pdfService.uploadPdf(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
