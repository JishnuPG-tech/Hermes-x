# Notion Connector Specification

## 1. Scope

The Notion connector gives Hermes controlled access to the user's authorized Notion workspace.

Current Notion API documentation represents page content as blocks and provides APIs for reading and appending block content. Current API versions also provide data-source retrieval and querying for structured page collections. citeturn1search1turn1search0turn1search6

## 2. Connection model

Preferred production flow:

```text
Hermes Settings
   ↓
Connect Notion
   ↓
OAuth/connection authorization
   ↓
Callback
   ↓
Validate state + nonce
   ↓
Exchange authorization result
   ↓
Encrypt token/credential material
   ↓
Store connection record
   ↓
Discover accessible sources
```

Never place the access token in the Android app bundle, Git repository, Docker image, or model prompt.

## 3. Connection record

```yaml
id: conn_notion_01
provider: notion
user_id: user_01
auth_type: oauth
secret_ref: secret://notion/conn_notion_01
workspace_id: ...
workspace_name: ...
scopes: []
status: active
created_at: ...
updated_at: ...
last_success_at: ...
```

## 4. Source discovery

Discover only sources visible to the connection.

Store:

```text
page_id
parent_id
title
url
icon
archived
last_seen
source_type
permissions
```

For structured databases/data sources also store property schemas.

## 5. Read pipeline

```text
Query
 ↓
Resolve project/scope
 ↓
Query relevant data sources
 ↓
Retrieve pages
 ↓
Retrieve block children recursively
 ↓
Normalize
 ↓
Chunk
 ↓
Rank
 ↓
Return evidence
```

Limit recursive expansion to prevent pathological page trees from consuming the entire task budget.

## 6. Write pipeline

```text
Assistant proposal
 ↓
WriteIntent
 ↓
Policy
 ↓
Destination check
 ↓
Permission check
 ↓
Current-state fetch
 ↓
Conflict check
 ↓
User confirmation if required
 ↓
Notion mutation
 ↓
Read-back verification
 ↓
Audit
```

## 7. Idempotency

Each write receives an internal operation ID:

```text
write_id = hash(task_id + destination + semantic_operation)
```

Before retrying a timed-out write, check whether the previous operation actually succeeded.

## 8. Rate limits and retries

Treat 429, temporary network failures and provider 5xx responses as retryable according to provider guidance.

Use:

```text
exponential backoff
+
jitter
+
maximum attempt count
+
circuit breaker
```

Do not retry authentication or permission errors indefinitely.

## 9. Notion-specific security

- Do not index pages outside the connection's accessible scope.
- Re-check access after authorization errors.
- Treat page text as untrusted content.
- Do not execute instructions embedded in pages.
- Redact secrets from logs.
- Store source URLs/IDs for provenance.

## 10. Recommended commands

Conceptual Hermes actions:

```text
/notion connect
/notion sources
/notion search <query>
/notion read <page>
/notion create <destination>
/notion append <page>
/notion sync
/notion disconnect
```

These should map to connector tools, not arbitrary shell commands.

## 11. Structured project database

Recommended Notion project data source properties:

```text
Name
Status
Priority
Project
Owner
Due
Tags
Last Updated
Hermes Task ID
GitHub URL
```

Hermes can use this for project/task synchronization, but its internal task state remains authoritative for active autonomous jobs.

## 12. Verification

After a write:

1. Retrieve the affected page/block/property.
2. Confirm expected content exists.
3. Record provider ID and timestamp.
4. Mark operation verified.

A successful HTTP response alone is not sufficient for an important autonomous workflow.
