package kr.sicksick.be.auth.oauth;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;

/**
 * 각 사 JSON 응답을 다루는 잡일 모음.
 *
 * <p>구조가 제각각이고 선택 동의 항목은 키 자체가 없는 경우가 흔해서, 전용 DTO 를
 * 만들기보다 Map 으로 받아 방어적으로 꺼내는 편이 실수가 적다.
 */
final class OAuthResponses {

    static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private OAuthResponses() {
    }

    static String stringOrNull(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> nestedOrEmpty(Map<String, Object> source, String key) {
        if (source == null) {
            return Map.of();
        }
        Object value = source.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /** 실패 로그에 남길 요약. 액세스 토큰 같은 값이 섞이지 않도록 오류 필드만 뽑는다. */
    static String errorSummary(Map<String, Object> response) {
        if (response == null) {
            return "(빈 응답)";
        }
        String error = stringOrNull(response, "error");
        String description = stringOrNull(response, "error_description");
        if (error == null && description == null) {
            return "(오류 필드 없음)";
        }
        return error + " - " + description;
    }
}
