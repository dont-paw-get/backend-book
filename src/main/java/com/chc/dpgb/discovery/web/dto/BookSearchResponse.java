package com.chc.dpgb.discovery.web.dto;

import java.util.List;

import com.chc.dpgb.discovery.ExternalBook;

public record BookSearchResponse(List<ExternalBook> books) {
}
