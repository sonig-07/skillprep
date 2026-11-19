package com.skillprep.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.skillprep.model.Question;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


@Service
public class ExportService {

    public byte[] toCsv(List<Question> questions) {
        StringBuilder sb = new StringBuilder("\uFEFF");   // BOM so Excel reads UTF-8 correctly
        sb.append("Question,Topic,Type,Difficulty,Answer,Score,Feedback\n");
        for (Question q : questions) {
            if (q.getAttempts().isEmpty()) {
                sb.append(csvRow(q.getQuestionText(), q.getSkillTopic(), q.getQuestionType(), q.getDifficulty(), "", "", ""));
            }
            for (Question.Attempt a : q.getAttempts()) {
                sb.append(csvRow(q.getQuestionText(), q.getSkillTopic(), q.getQuestionType(), q.getDifficulty(),
                        a.getAnswerText(), String.valueOf(a.getScore()), a.getIdealAnswerNotes()));
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toPdf(List<Question> questions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(new PdfDocument(new PdfWriter(out)));

        doc.add(new Paragraph("SkillPrep - Question Bank").setBold().setFontSize(16));
        for (Question q : questions) {
            doc.add(new Paragraph(q.getQuestionText()).setBold().setFontSize(12));
            doc.add(new Paragraph(q.getSkillTopic() + " | " + q.getQuestionType() + " | " + q.getDifficulty()).setFontSize(9));
            if (q.getAttempts().isEmpty()) {
                doc.add(new Paragraph("Not answered yet.").setItalic().setFontSize(10));
            }
            for (Question.Attempt a : q.getAttempts()) {
                doc.add(new Paragraph("Answer (score " + a.getScore() + "): " + a.getAnswerText()).setFontSize(10));
            }
            doc.add(new Paragraph(" "));
        }
        doc.close();
        return out.toByteArray();
    }

    
    private String csvRow(String... values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) row.append(",");
            String v = values[i] == null ? "" : values[i];
            row.append("\"").append(v.replace("\"", "\"\"")).append("\"");
        }
        return row.append("\n").toString();
    }
}
