package education.onboarding.service;

import education.onboarding.client.InfraiGateway;
import education.onboarding.domain.LearnerSignup;
import education.onboarding.domain.OnboardingReport;
import education.onboarding.domain.SignupChannel;

public final class CourseWelcomeService {
    private final InfraiGateway infrai;

    public CourseWelcomeService(InfraiGateway infrai) {
        this.infrai = infrai;
    }

    public OnboardingReport onboard(LearnerSignup signup) {
        SignupChannel first = signup.signupChannel();
        if (first == SignupChannel.EMAIL && !infrai.isEmailSuppressed(signup.email())) {
            return report(signup, SignupChannel.EMAIL, infrai.sendWelcomeEmail(signup));
        }
        if (first == SignupChannel.SMS && !infrai.isSmsSuppressed(signup.phone())) {
            return report(signup, SignupChannel.SMS, infrai.sendWelcomeSms(signup));
        }

        SignupChannel fallback = first == SignupChannel.EMAIL ? SignupChannel.SMS : SignupChannel.EMAIL;
        if (fallback == SignupChannel.SMS && !infrai.isSmsSuppressed(signup.phone())) {
            return report(signup, SignupChannel.SMS, infrai.sendWelcomeSms(signup));
        }
        if (fallback == SignupChannel.EMAIL && !infrai.isEmailSuppressed(signup.email())) {
            return report(signup, SignupChannel.EMAIL, infrai.sendWelcomeEmail(signup));
        }
        throw new IllegalStateException("No consented welcome channel is available for this learner");
    }

    private static OnboardingReport report(LearnerSignup signup, SignupChannel channel, String messageId) {
        return new OnboardingReport(signup.learnerName(), signup.courseId(), signup.deadline(), channel, messageId);
    }
}
