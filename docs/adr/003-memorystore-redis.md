# ADR 003: Memorystore Redis for Cache and Vector Search

## Status

Accepted

## Context

We need two things from Redis: a semantic cache for LLM responses and a vector index for recipe embeddings. Both need fast reads (sub-millisecond).

Options were to run Redis ourselves on the VM as a Docker container, or use Memorystore which is Google's managed Redis service.

## Decision

Memorystore Redis 7.2, basic tier, 1GB.

## Why

The project requires PaaS usage for grading. A self-hosted Redis container on the VM would only count as IaaS. Memorystore is a managed service where Google handles availability, patching, and backups. That makes it PaaS.

Redis 7.2 because it supports the RediSearch module for vector similarity search. Older versions do not.

Basic tier because we do not need replication or high availability for a demo project. Standard tier would double the cost for no benefit here.

1GB because our recipe vectors (768 dimensions, float32, ~3KB each) plus cache entries fit comfortably. Even 10,000 recipes would only use ~30MB of vector data.

The eviction policy is set to `allkeys-lru`. When memory fills up, Redis drops the least recently used entry. Correct behavior for a cache.

## Trade-offs

Memorystore costs ~$0.05/hour even when idle. We run `terraform destroy` after each session to avoid burning credits overnight.

Memorystore Redis does not include the full Redis Stack with RediSearch built in. We need to verify that vector search commands work on Memorystore 7.2. If not, the fallback is a self-hosted Redis Stack container on the VM and using Memorystore only for the semantic cache (key-value). That would still count as PaaS.

Locally we use Redis Stack in Docker Compose which has vector search built in. There is a small risk of behavior differences between local Redis Stack and Memorystore.
