#!/usr/bin/env sh
set -eu

: "${INFRAI_API_KEY:?Set INFRAI_API_KEY}"
: "${LEARNER_EMAIL:?Set LEARNER_EMAIL}"
: "${LEARNER_PHONE:?Set LEARNER_PHONE}"

BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

javac -d "$BUILD_DIR" $(find src/main/java -name '*.java' -print)
java -cp "$BUILD_DIR" education.onboarding.CourseOnboardingExample \
  "${LEARNER_NAME:-Mina}" "$LEARNER_EMAIL" "$LEARNER_PHONE" "${SIGNUP_CHANNEL:-EMAIL}" \
  "${COURSE_ID:-java-101}" "${COURSE_TITLE:-Practical Java}" "${LEARNER_DEADLINE:-2026-10-05}" \
  "${ONBOARDING_ID:-enrollment-2048}"
