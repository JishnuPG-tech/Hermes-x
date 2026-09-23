# Dynamic Capability Discovery

## 1. Problem with Hardcoded Keyword Tools

In legacy chatbot implementations, developers wrote keyword checks:
```python
# ANTI-PATTERN: DO NOT USE
if "notion" in prompt or "notes" in prompt:
    tools = [notion_search, notion_read]
elif "ls" in prompt or "dir" in prompt:
    tools = [list_dir]
```
This fails when:
- Users speak naturally: *"Where did I put the deployment keys?"*
- Synonyms are used: *"Find my stored documentation"*
- Multi-domain tasks are given: *"Find the credentials in Notion and configure the nginx container"*

## 2. CapabilityIndex Architecture

Hermes implements `CapabilityIndex` (`hermes_core/runtime/tool_discovery.py`):
1. **Tool Metadata Registry**: Every tool registers with name, description, category, parameter schema, required permissions, and side-effect indicators.
2. **Semantic Query Matching**: Ranks tools dynamically based on objective tokens matching tool descriptions, parameter schemas, and category tags.
3. **The `search_tools` Meta-Tool**: Always exposed to the model. If a model needs a capability that isn't currently in its prompt context, it calls:
   ```json
   {
     "name": "search_tools",
     "arguments": {"query": "manage git branches and pull requests"}
   }
   ```
   `CapabilityIndex` returns matching tool schemas, which are injected into the agent's active tool palette for the very next turn.
4. **Zero-Code Tool Addition**: Adding a new tool (e.g. `get_weather` or `deploy_kubernetes`) only requires registering it with `@registry.register()`. No prompt modification or routing logic changes are required.
