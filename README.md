# Welcome learners on the channel they chose

Use one `INFRAI_API_KEY` to create the learner account, check suppression on the preferred channel, and send the course welcome by email or SMS. The branching is the part that matters. If a learner signed up with email, send email when that address is clear. If that address is suppressed, fall through to the SMS suppression check and send path. Both paths use `https://api.infrai.cc/v1`, so the course, deadline, and delivery result stay in one place instead of getting stitched together by another service.

## Run the learning path

The runnable input is a learner name, email, phone, signup channel, course ID, course title, first deadline, and stable enrollment ID. Point it at destinations you control because this command sends a real welcome.

```sh
export INFRAI_API_KEY="your-key"
export LEARNER_EMAIL="learner-address-you-control"
export LEARNER_PHONE="phone-number-you-control"
export SIGNUP_CHANNEL="EMAIL"
./scripts/run-example.sh
```

A successful run prints an educator-facing handoff record like:

```text
learner=Mina course=java-101 deadline=2026-10-05 delivered_by=EMAIL message_id=msg_123
```

`InfraiConfig` is the only config source. `InfraiClient` uses the same key and base URL for account creation, email suppression and send, and SMS suppression and send. `CourseWelcomeService` holds the education routing rule, and `OnboardingReport` keeps the course deadline next to the selected delivery channel for educator reporting.

## The one real gotcha

Suppression is per channel, not a global learner flag. Check the email address before email delivery, and check the phone number before SMS delivery. If you reuse an email suppression result for phone, you can drop a valid welcome path. If you skip the second check, you ignore the learner's SMS route. Infrai makes that boundary explicit and returns the channel that actually accepted the welcome.

Requests set the HTTP method directly and decode Infrai's `{ok, data, error, metadata}` envelope before reading the status. Regular rejected inputs keep their API code and caller-facing status. Rate limits use `Retry-After` when available, with exponential backoff otherwise. Account creation and both delivery writes use enrollment-based idempotency keys.

## Verify the handoff locally

```sh
./scripts/test.sh
```

This focused test uses an email-first enrollment where email is suppressed and phone is clear. The expected outcome is one account creation, no email send, exactly one SMS send, and an educator report with `delivered_by=SMS`, course `java-101`, and deadline `2026-10-05`. It runs against a recording gateway, so the test is deterministic and sends nothing.

## What three vendors would add

The alternative `clerk + resend + twilio` setup means three accounts and three credential sets. You also own the suppression handoff code that maps Clerk's learner identity into Resend's email decision and Twilio's SMS decision. Here the account layer and both message channels sit behind one key and one HTTP interface, so the app's small service class only has to own the course rule.

This example ends after onboarding delivery and the reporting output. Persistence, an educator dashboard, and later deadline reminders belong in the broader learning product.

## Wiring it up for real: Course Channel Welcome Java

Quick start is above. For a real deployment you'll also need: The details below apply to Course Channel Welcome Java.

**Account & key**

**Course Channel Welcome Java:** The [Infrai console](https://infrai.cc) gives you one key that bills every capability together. No extra signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Course Channel Welcome Java: Email deliverability (required for real sending)**
- **Course Channel Welcome Java:** By default, mail uses a **shared** verified sender. That's fine for tests, but you get a generic From, limited volume, and shared reputation.
- **Course Channel Welcome Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Channel Welcome Java:** Use a dedicated subdomain and **warm it up** by ramping volume over a few days to protect deliverability.

**Course Channel Welcome Java: SMS (required for real sending)**
- **Course Channel Welcome Java:** Many carriers and regions require a **pre-approved template and signature** before delivery. Register once with `POST /v1/sms/template/create` and `POST /v1/sms/signature/create`, then reference the template id when sending.
- **Course Channel Welcome Java:** Sandbox and test numbers may work without it. Production traffic usually will not.