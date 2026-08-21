package com.chc.dpgb.discovery.aladin;

record AladinItem(
        String title,
        String author,
        String pubDate,
        String isbn,
        String isbn13,
        String publisher,
        String cover,
        AladinSubInfo subInfo
) {
}
