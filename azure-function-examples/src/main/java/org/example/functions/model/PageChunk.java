package org.example.functions.model;

public class PageChunk {
    private final int pageNumber;
    private final String text;

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
