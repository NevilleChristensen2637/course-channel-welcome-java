package education.onboarding.client;

import education.onboarding.domain.LearnerSignup;

public interface InfraiGateway {
    void createAccount(LearnerSignup signup);
    boolean isEmailSuppressed(String email);
    boolean isSmsSuppressed(String phone);
    String sendWelcomeEmail(LearnerSignup signup);
    String sendWelcomeSms(LearnerSignup signup);
}
