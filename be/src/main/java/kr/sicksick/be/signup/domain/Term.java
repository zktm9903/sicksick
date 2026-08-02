package kr.sicksick.be.signup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 약관 한 건의 특정 버전.
 *
 * <p>개정하면 같은 code 의 새 version 행을 추가하고 옛 행의 active 를 내린다.
 * 이미 저장된 동의 이력은 옛 행을 계속 가리키므로 "이 유저가 어느 버전에 동의했는지"가
 * 보존된다. 분쟁 시 증명해야 하는 정보다.
 */
@Entity
@Table(name = "terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;
}
