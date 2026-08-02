package kr.sicksick.be.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** 난수 생성과 해시. 토큰·인증번호처럼 원문을 저장하면 안 되는 값에 쓴다. */
public final class Secrets {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private Secrets() {
    }

    /** URL 에 그대로 실을 수 있는 난수 문자열. */
    public static String randomUrlSafe(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return URL_ENCODER.encodeToString(buffer);
    }

    /** 앞자리 0 이 유지되는 n 자리 숫자 코드. */
    public static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /** 저장용 SHA-256 hex. 컬럼 길이 64 에 맞는다. */
    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다", e);
        }
    }

    /**
     * 상수 시간 비교.
     *
     * <p>{@code String.equals} 는 처음 다른 문자에서 즉시 반환하므로 비교 시간이
     * 일치한 접두사 길이를 흘린다. state·인증번호 대조에는 이 메서드를 쓴다.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
