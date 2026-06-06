package com.example.quiz.dome.util;

import com.example.quiz.sys.common.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DocumentParser {

    public static String parse(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException("文件名不能为空");
        }
        String name = originalName.toLowerCase();
        try {
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } else if (name.endsWith(".docx")) {
                return parseWord(file);
            } else if (name.endsWith(".pdf")) {
                return parsePdf(file);
            } else {
                throw new BusinessException("不支持的文件类型，仅支持 TXT / DOCX / PDF");
            }
        } catch (IOException e) {
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }
    }

    private static String parseWord(MultipartFile file) throws IOException {
        XWPFDocument doc = new XWPFDocument(file.getInputStream());
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : doc.getParagraphs()) {
            String text = p.getText();
            if (text != null && !text.isBlank()) {
                sb.append(text).append("\n");
            }
        }
        doc.close();
        return sb.toString();
    }

    private static String parsePdf(MultipartFile file) throws IOException {
        var doc = Loader.loadPDF(file.getBytes());
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String text = stripper.getText(doc);
        doc.close();
        return text;
    }

    public static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot + 1).toLowerCase();
    }
}
