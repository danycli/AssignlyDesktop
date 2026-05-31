package com.assignly.service;

public record SearchRecord(
    String emoji,
    String title,
    String description,
    String category,
    String keywords,
    Runnable action
) {}
