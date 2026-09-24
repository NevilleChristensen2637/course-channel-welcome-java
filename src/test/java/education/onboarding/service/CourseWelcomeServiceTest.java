package education.onboarding.service;

import education.onboarding.client.InfraiGateway;
import education.onboarding.domain.LearnerSignup;
import education.onboarding.domain.OnboardingReport;
import education.onboarding.domain.SignupChannel;

public final class CourseWelcomeServiceTest {
    public static void main(String[] args) {
        LearnerSignup signup = new LearnerSignup(
                "Mina", "mina@example.test", "+15550101010", SignupChannel.EMAIL,
                "java-101", "Practical Java", "2026-10-05", "enrollment-2048");
        RecordingGateway gateway = new RecordingGateway();

        OnboardingReport report = new CourseWelcomeService(gateway).onboard(signup);

        check(!gateway.accountCreated, "onboarding must not create an auth user that cannot be deleted");
        check(gateway.emailChecks == 1, "preferred email should be checked once");
        check(gateway.smsChecks == 1, "SMS fallback should be checked once");
        check(gateway.emailSends == 0, "suppressed email must not be sent");
        check(gateway.smsSends == 1, "clear SMS fallback should be sent once");
        check(report.deliveredBy() == SignupChannel.SMS, "report should expose the handoff to SMS");
        check(report.courseId().equals("java-101"), "educator report should retain the course");
        check(report.deadline().equals("2026-10-05"), "educator report should retain the deadline");
        System.out.println("PASS email suppression hands welcome delivery to SMS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class RecordingGateway implements InfraiGateway {
        private boolean accountCreated;
        private int emailChecks;
        private int smsChecks;
        private int emailSends;
        private int smsSends;

        @Override public void createAccount(LearnerSignup signup) { accountCreated = true; }
        @Override public boolean isEmailSuppressed(String email) { emailChecks++; return true; }
        @Override public boolean isSmsSuppressed(String phone) { smsChecks++; return false; }
        @Override public String sendWelcomeEmail(LearnerSignup signup) { emailSends++; return "email-unused"; }
        @Override public String sendWelcomeSms(LearnerSignup signup) { smsSends++; return "sms-lesson-42"; }
    }
}
