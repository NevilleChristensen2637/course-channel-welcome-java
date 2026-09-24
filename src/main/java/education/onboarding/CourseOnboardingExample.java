package education.onboarding;

import education.onboarding.client.InfraiClient;
import education.onboarding.client.InfraiException;
import education.onboarding.config.InfraiConfig;
import education.onboarding.domain.LearnerSignup;
import education.onboarding.domain.OnboardingReport;
import education.onboarding.domain.SignupChannel;
import education.onboarding.service.CourseWelcomeService;

public final class CourseOnboardingExample {
    private CourseOnboardingExample() {}

    public static void main(String[] args) {
        if (args.length != 8) {
            System.err.println("Expected: name email phone EMAIL|SMS course-id course-title deadline idempotency-key");
            System.exit(2);
        }

        LearnerSignup signup = new LearnerSignup(
                args[0], args[1], args[2], SignupChannel.valueOf(args[3].toUpperCase()),
                args[4], args[5], args[6], args[7]);
        InfraiConfig config = InfraiConfig.fromEnvironment();
        CourseWelcomeService service = new CourseWelcomeService(new InfraiClient(config));
        try {
            OnboardingReport report = service.onboard(signup);
            System.out.printf("learner=%s course=%s deadline=%s delivered_by=%s message_id=%s%n",
                    report.learnerName(), report.courseId(), report.deadline(),
                    report.deliveredBy(), report.messageId());
        } catch (InfraiException error) {
            System.err.printf("request_rejected code=%s status=%d message=%s%n",
                    error.code(), error.statusCode(), error.getMessage());
            System.exit(error.statusCode() >= 400 && error.statusCode() < 500 ? 2 : 1);
        }
    }
}
