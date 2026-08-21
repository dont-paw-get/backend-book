package com.chc.dpgb.discovery.aladin;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 알라딘 응답의 author는 "세네카 (지은이), 최지원 (옮긴이)"처럼 각 이름 뒤에 역할 라벨이 괄호로 붙은 결합 문자열이다. 콤마로 나눈 뒤 각 항목 끝의 괄호를 제거한다.
 */
final class AuthorNameNormalizer {

    private AuthorNameNormalizer() {
    }

    static String normalize(String rawAuthor) {
        if (rawAuthor == null) {
            return null;
        }
        return Arrays.stream(rawAuthor.split(","))
                .map(AuthorNameNormalizer::stripRoleLabel)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String stripRoleLabel(String segment) {
        return segment.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
    }
}
