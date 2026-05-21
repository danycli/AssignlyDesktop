package com.assignly.model;

public class Announcement {
    private final long id;
    private final String title;
    private final String content;
    private final String publishDate;

    public Announcement(long id, String title, String content, String publishDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.publishDate = publishDate;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getPublishDate() {
        return publishDate;
    }
}
