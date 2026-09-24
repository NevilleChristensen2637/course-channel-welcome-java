package education.onboarding.domain;

import java.util.Objects;

public record LearnerSignup(
        String learnerName,
        String email,
        String phone,
        SignupChannel signupChannel,
        String courseId,
        String courseTitle,
        String deadline,
        String idempotencyKey) {

    public LearnerSignup {
        Objects.requireNonNull(learnerName);
        Objects.requireNonNull(email);
        Objects.requireNonNull(phone);
        Objects.requireNonNull(signupChannel);
        Objects.requireNonNull(courseId);
        Objects.requireNonNull(courseTitle);
        Objects.requireNonNull(deadline);
        Objects.requireNonNull(idempotencyKey);
    }
}
