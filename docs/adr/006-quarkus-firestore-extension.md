# ADR 006: Quarkiverse Firestore Extension

## Status

Accepted

## Context

We need a Firestore client in the Kotlin/Quarkus backend. Two options: use the Google Cloud Firestore Java SDK directly, or use the Quarkiverse Google Cloud Services extension.

## Decision

Quarkiverse extension (`quarkus-google-cloud-firestore`).

## Why

The extension gives us three things for free. First, dependency injection: we write `@Inject lateinit var firestore: Firestore` and Quarkus handles the client lifecycle. No manual setup, no connection management.

Second, a built-in dev service. In dev mode, the extension starts a local Firestore emulator automatically. We do not need to install anything or add it to Docker Compose for development.

Third, it reads the GCP project ID from `application.properties` and handles authentication through the standard GCP credential chain. On the VM, it picks up the service account. Locally, it uses `gcloud auth application-default`.

With the raw SDK we would write all of that ourselves. For a project this size, that is wasted effort.

## Trade-offs

The extension is maintained by the Quarkiverse community, not by Google or Red Hat directly. If it breaks or falls behind the SDK, we would need to switch. The version is managed by the `quarkus-google-cloud-services-bom`, currently aligned with Quarkus Platform 3.34.x via the `quarkusPlatformVersion` Gradle property; switching the platform version updates the Firestore extension automatically.
