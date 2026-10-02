# References

This document records the upstream references used when designing this persistence plan.

## Hugging Face

- Docker Spaces data persistence: https://huggingface.co/docs/hub/spaces-sdks-docker
- Spaces storage and Storage Buckets: https://huggingface.co/docs/hub/spaces-storage
- Storage Buckets: https://huggingface.co/docs/hub/storage-buckets
- Manage Spaces and volumes: https://huggingface.co/docs/huggingface_hub/guides/manage-spaces

Key platform rule: normal Docker Space filesystem data is ephemeral across restart. An attached Storage Bucket can provide persistent storage at its mounted runtime path. The bucket is not available during Docker image build, so persistence configuration belongs to runtime startup, not Docker build-time initialization.

## OmniRoute

- Repository: https://github.com/diegosouzapw/OmniRoute
- Architecture: https://github.com/diegosouzapw/OmniRoute/wiki/Architecture
- Environment: https://github.com/diegosouzapw/OmniRoute/wiki/Environment
- Docker guide: https://github.com/diegosouzapw/OmniRoute/wiki/Docker-Guide

Key OmniRoute rules used by this plan:

- `DATA_DIR` is the root for SQLite DB, backups and data files.
- OmniRoute uses SQLite and WAL.
- The database and WAL lifecycle must be handled safely during shutdown and backup.
- Docker deployments should mount persistent storage for database, keys and configuration.
- SQLite readonly failures can result from volume permissions, not only file permissions.

## Current repository evidence

The `JishnuPG-tech/Omniroute` Dockerfile currently imports a prebuilt OmniRoute runtime and sets XDG data/config/state locations under `/data`, but the entrypoint historically configured OmniRoute's `DATA_DIR` under `/root/.omniroute` and used snapshot/restore logic. The target design removes that split-brain behavior by making `/data/omniroute` the live `DATA_DIR`.

## Versioning note

Upstream OmniRoute changes frequently. Before production deployment, check the current release notes and validate migrations against a copy of the real database. Do not assume a future release is schema-compatible solely because it has a newer version number.
