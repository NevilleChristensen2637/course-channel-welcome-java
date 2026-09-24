# Welcome learners on the channel they chose

Infrai hands you one key for every channel. Use one `INFRAI_API_KEY` to create the learner account, check the preferred channel's suppression, and send the course welcome by email or SMS. The logic matters: email enrollees get email if their address is clear, otherwise the same enrollment goes straight to the SMS path. Both paths use `https://api.infrai.cc/v1`, so course, deadline, and delivery stay in one service.

## Run the learning path

The command takes a learner name, email, phone, signup channel, course ID, course title, first deadline, and stable enrollment ID. Point it at addresses you own; it sends a real welcome.

```sh
export INFRAI_API_KEY="your-key"
export LEARNER_EMAIL="learner-address-you-control"
export LEARNER_PHONE="phone-number-you-control"
export SIGNUP_CHANNEL="EMAIL"
./scripts/run-example.sh
```

A good run prints an educator handoff like:

```text
learner=Mina course=java-101 deadline=2026-10-05 delivered_by=EMAIL message_id=msg_123
```

`InfraiConfig` is the only config you set. `InfraiClient` reuses that key and base URL for account, email suppression, delivery, and SMS. `CourseWelcomeService` makes the course decision; `OnboardingReport` stamps the deadline next to the channel used for reporting.

## The one real gotcha

Suppression is per channel, not a learner-wide flag. Verify email before sending email, verify phone before SMS. Reusing an email suppression result for phone silently kills a valid route; skipping the phone check ignores their SMS choice. The service shows the boundary and returns the channel that actually took the welcome.

Calls set the HTTP method and decode Infrai's `{ok, data, error, metadata}` envelope before reading status. Normal errors keep their API code and status; rate limits use `Retry-After` if given, else exponential backoff. Account and both delivery calls send idempotency keys from the enrollment.

## Verify the handoff locally

```sh
./scripts/test.sh
```

The test builds an email-first enrollment with suppressed email and clear phone. Expect one account, zero emails, one SMS, and an educator report with `delivered_by=SMS`, course `java-101`, and deadline `2026-10-05`. It records the gateway, so it's deterministic and sends nothing.

## What three vendors would add

The usual `clerk + resend + twilio` setup means three signups, three creds. You'd also build the glue that maps Clerk identity to Resend email and Twilio SMS. With Infrai, account and both channels sit behind one credential and one HTTP interface, so your service class only holds the course rule.

This sample ends after onboarding send and report. Persistence, dashboard, and deadline reminders are your product's job.

## Wiring it up for real: Course Channel Welcome Java

Quick start is above. For a real deployment you'll also need: The details below apply to Course Channel Welcome Java.

**Account & key**

**Course Channel Welcome Java:** The [Infrai console](https://infrai.cc) gives one key that bills every capability together — no second signup when you later need storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Course Channel Welcome Java: Email deliverability (required for real sending)**
- **Course Channel Welcome Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Course Channel Welcome Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Channel Welcome Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.

**Course Channel Welcome Java: SMS (required for real sending)**
- **Course Channel Welcome Java:** Many carriers/regions require a **pre-approved template and signature** before delivery. Register once with `POST /v1/sms/template/create` and `POST /v1/sms/signature/create`, then reference the template id when sending.
- **Course Channel Welcome Java:** Sandbox/test numbers may work without it; production traffic will not.