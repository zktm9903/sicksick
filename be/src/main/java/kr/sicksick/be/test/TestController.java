package kr.sicksick.be.test;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드와의 연결을 확인하기 위한 테스트 엔드포인트.
 */
@RestController
@RequestMapping("/api/v1/test")
class TestController {

    @GetMapping
    Map<String, String> test() {
        return Map.of("status", "ok");
    }
}
