package education.onboarding.domain;

public record OnboardingReport(
        String learnerName,
        String courseId,
        String deadline,
        SignupChannel deliveredBy,
        String messageId) {
}
