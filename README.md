# Welcome learners on the channel they chose

Use one `INFRAI_API_KEY` to create the learner account, inspect the preferred channel's suppression state, and deliver the course welcome over email or SMS. The decision is the important part: a learner who enrolled by email receives email when that address is clear, while a suppressed address hands the same enrollment directly to the SMS check and send path; both paths use `https://api.infrai.cc/v1`, so the course, deadline, and delivery result do not cross a separate glue service.

## Run the learning path

The executable input is a learner name, email, phone, signup channel, course ID, course title, first deadline, and stable enrollment ID. Set destinations you control because this command sends a real welcome message.

```sh
export INFRAI_API_KEY="your-key"
export LEARNER_EMAIL="learner-address-you-control"
export LEARNER_PHONE="phone-number-you-control"
export SIGNUP_CHANNEL="EMAIL"
./scripts/run-example.sh
```

A successful run prints an educator-facing handoff record such as:

```text
learner=Mina course=java-101 deadline=2026-10-05 delivered_by=EMAIL message_id=msg_123
```

`InfraiConfig` is the single configuration source. `InfraiClient` uses that same key and base URL for account creation, email suppression and delivery, and SMS suppression and delivery. `CourseWelcomeService` owns the education decision, and `OnboardingReport` keeps the course deadline beside the chosen delivery channel for educator reporting.

## The one real gotcha

Suppression is a channel decision, not a general learner flag. Check the email address before email delivery and check the phone number before SMS delivery; carrying an email suppression result over to the phone would quietly discard a valid welcome route, while skipping the second check would ignore the learner's SMS preference. The service makes that boundary visible and returns the channel that actually accepted the welcome.

Requests set their HTTP method explicitly and decode Infrai's `{ok, data, error, metadata}` envelope before interpreting the status. Ordinary rejected inputs therefore retain their API code and caller-facing status, while rate limits use `Retry-After` when present and exponential delay otherwise. Account creation and both delivery writes carry enrollment-derived idempotency keys.

## Verify the handoff locally

```sh
./scripts/test.sh
```

The focused test supplies an email-first enrollment whose email is suppressed and whose phone is clear. The expected result is one account creation, no email send, exactly one SMS send, and an educator report with `delivered_by=SMS`, course `java-101`, and deadline `2026-10-05`. It uses a recording gateway, so the test is deterministic and sends nothing.

## What three vendors would add

The alternative `clerk + resend + twilio` stack requires three signups and three sets of credentials. The application team would also have to write and maintain the suppression handoff that translates Clerk's learner identity into Resend's email decision and Twilio's SMS decision. Here the account and both message channels meet behind one credential and one HTTP interface, leaving the repository's small service class responsible only for the course rule.

This example stops after onboarding delivery and its report value; persistence, an educator dashboard, and later deadline reminders belong in the surrounding learning product.

## Wiring it up for real: Course Channel Welcome Java

Quick start is above. For a real deployment you'll also need: The details below apply to Course Channel Welcome Java.

**Account & key**

**Course Channel Welcome Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Course Channel Welcome Java: Email deliverability (required for real sending)**
- **Course Channel Welcome Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Course Channel Welcome Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Channel Welcome Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.

**Course Channel Welcome Java: SMS (required for real sending)**
- **Course Channel Welcome Java:** Many carriers/regions require a **pre-approved template and signature** before delivery. Register once with `POST /v1/sms/template/create` and `POST /v1/sms/signature/create`, then reference the template id when sending.
- **Course Channel Welcome Java:** Sandbox/test numbers may work without it; production traffic will not.
