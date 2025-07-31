package org.example.functions.model;

import java.time.LocalDateTime;

public class PageChunk {
    private final int pageNumber;
    private final String text;

    ;
    private String docName;
    private String caseId;
    private LocalDateTime ingestionTime;

    public PageChunk(int pageNumber, String text) {
        this.pageNumber = pageNumber;
        this.text = text;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Page " + pageNumber + ": " + text;
    }
}
