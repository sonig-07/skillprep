package com.skillprep.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.opencsv.CSVWriter;
import com.skillprep.model.Question;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

@Service
public class ExportService {

    private static final String[] HEADERS = {
            "Question", "Topic", "Type", "Difficulty", "Experience Level",
            "Answer", "Score", "Feedback Notes"
    };

    public byte[] toCsv(List<Question> questions) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {

            writer.writeNext(HEADERS);
            for (Question q : questions) {
                if (q.getAttempts().isEmpty()) {
                    writer.writeNext(row(q, null));
                } else {
                    for (Question.Attempt a : q.getAttempts()) {
                        writer.writeNext(row(q, a));
                    }
                }
            }
            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build CSV export: " + e.getMessage(), e);
        }
    }

    private String[] row(Question q, Question.Attempt a) {
        return new String[] {
                q.getQuestionText(),
                q.getSkillTopic(),
                q.getQuestionType(),
                q.getDifficulty(),
                q.getExperienceLevel() == null ? "" : String.valueOf(q.getExperienceLevel()),
                a == null ? "" : a.getAnswerText(),
                a == null ? "" : String.valueOf(a.getScore()),
                a == null ? "" : a.getIdealAnswerNotes()
        };
    }

    public byte[] toPdf(List<Question> questions) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdfDoc);

            doc.add(new Paragraph("SkillPrep — Question Bank Export").setBold().setFontSize(16));
            doc.add(new Paragraph(" "));

            for (Question q : questions) {
                doc.add(new Paragraph(q.getQuestionText()).setBold().setFontSize(12));
                doc.add(new Paragraph(String.format("Topic: %s | Type: %s | Difficulty: %s | Level: %s",
                        q.getSkillTopic(), q.getQuestionType(), q.getDifficulty(),
                        q.getExperienceLevel() == null ? "-" : q.getExperienceLevel())).setFontSize(9));

                if (q.getAttempts().isEmpty()) {
                    doc.add(new Paragraph("Not yet answered.").setItalic().setFontSize(10));
                } else {
                    Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 3}))
                            .useAllAvailableWidth();
                    table.addHeaderCell(new Cell().add(new Paragraph("Answer").setBold()));
                    table.addHeaderCell(new Cell().add(new Paragraph("Score").setBold()));
                    table.addHeaderCell(new Cell().add(new Paragraph("Feedback").setBold()));
                    for (Question.Attempt a : q.getAttempts()) {
                        table.addCell(new Cell().add(new Paragraph(a.getAnswerText())));
                        table.addCell(new Cell().add(new Paragraph(String.valueOf(a.getScore()))));
                        table.addCell(new Cell().add(new Paragraph(a.getIdealAnswerNotes())));
                    }
                    doc.add(table);
                }
                doc.add(new Paragraph(" "));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build PDF export: " + e.getMessage(), e);
        }
    }
}
