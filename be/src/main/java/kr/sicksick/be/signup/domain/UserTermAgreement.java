package kr.sicksick.be.signup.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저의 약관 동의 이력. 선택 약관은 거부(agreed=false)도 기록으로 남긴다. */
@Entity
@Table(name = "user_term_agreements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermAgreement {

    @EmbeddedId
    private Key key;

    @Column(nullable = false)
    private boolean agreed;

    @Column(nullable = false)
    private Instant agreedAt;

    private UserTermAgreement(Key key, boolean agreed, Instant agreedAt) {
        this.key = key;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    public static UserTermAgreement of(Long userId, Long termId, boolean agreed, Instant now) {
        return new UserTermAgreement(new Key(userId, termId), agreed, now);
    }

    public void update(boolean agreed, Instant now) {
        this.agreed = agreed;
        this.agreedAt = now;
    }

    public Long userId() {
        return key.getUserId();
    }

    public Long termId() {
        return key.getTermId();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(nullable = false)
        private Long userId;

        @Column(nullable = false)
        private Long termId;

        Key(Long userId, Long termId) {
            this.userId = userId;
            this.termId = termId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key other)) {
                return false;
            }
            return Objects.equals(userId, other.userId) && Objects.equals(termId, other.termId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, termId);
        }
    }
}
