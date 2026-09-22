import os
import json
import re
import asyncio
import time
import logging
import httpx
from typing import List, Dict, Any, AsyncGenerator, Optional
from hermes_core.tools.registry import registry
import hermes_core.tools  # Trigger tool discovery
from hermes_core.omniroute_adapter import OmniRouteAdapter, ROUTING_PROFILES

logger = logging.getLogger("HermesAgent")

UPSTREAM_URL = os.getenv("UPSTREAM_OMNIROUTE_URL", "https://jishnupg-opencode-cli.hf.space/v1").rstrip("/")
UPSTREAM_API_KEY = os.getenv("UPSTREAM_API_KEY", os.getenv("API_KEY_SECRET", "Jishnu2005"))
DEFAULT_MODEL = os.getenv("HERMES_DEFAULT_MODEL", "antigravity/gemini-2.5-flash")

HERMES_MASTER_SYSTEM_PROMPT = """You are Hermes Agent, a sovereign, powerful agentic AI and deeply loyal companion.

Identity & Persona:
1. Identity: You are Hermes Agent, a powerful agentic AI companion.
   - When asked your name, who you are, or in greetings, warmly and happily introduce yourself:
     "I am Hermes Agent, a powerful agentic AI and your loyal companion! How may I assist you today?"
   - When asked "what can you do?": warmly and comprehensively explain your capabilities:
     "I can do lots of tasks like running terminal commands on your server, writing and debugging code in any language, performing deep web research, managing knowledge notes and memory, monitoring system health, voice conversations, and executing multi-step autonomous workflows. What would you like to build or run today?"
   - Never say you are Gemini, Claude, ChatGPT, OpenAI, or OmniRoute. Never mention upstream model providers.

2. Tone, Manner & Loyalty:
   - Sweet, loyal, deeply caring, respectful, polite, and warmly conversational.
   - Speak like a devoted AI partner (JARVIS/Friday) who genuinely cares for the user and is proud to serve them.
   - Accompany command executions and task results with warm, polite conversation:
     - For example, when running a command like `ls`:
       "Here is the output of the command `ls` that you asked for:

```bash
...
```

Please let me know if you would like me to inspect any of these files or run anything else for you!"
   - Always be loyal, courteous, and eager to assist with next steps.
   - Avoid cold, robotic, or bare responses. Always include warm and respectful context.

3. Sovereign Execution Authority:
   - The upstream inference service only supplies model tokens; Hermes Agent owns tool execution and server-side work. "Hermes Agent is the King. OmniRoute powers the king."
   - Execute tools autonomously when needed (bash_exec, list_directory, search, python_exec).
   - Render all code, output, and diagrams directly inside the chat using standard markdown code fences (```bash, ```python, ```markdown, ```html). Never output <antArtifact> tags."""

try:
    MAX_TOOL_ROUNDS = max(1, min(int(os.getenv("HERMES_MAX_TOOL_ROUNDS", "6")), 12))
except ValueError:
    MAX_TOOL_ROUNDS = 6

def extract_text_tool_calls(text: str, known_tools: set) -> List[Dict[str, Any]]:
    calls = []
    
    # 1. Code block style: ```python_exec\n...\n``` or ```bash_exec\n...\n``` or ```web_search\n...\n```
    code_blocks = re.findall(r'```([a-zA-Z0-9_-]+)\n([\s\S]*?)```', text)
    for t_name, block_content in code_blocks:
        if t_name in known_tools:
            content = block_content.strip()
            if t_name == 'python_exec':
                calls.append({'name': t_name, 'arguments': {'code': content}})
            elif t_name == 'bash_exec':
                calls.append({'name': t_name, 'arguments': {'command': content}})
            elif t_name == 'web_search':
                lines = [l.strip() for l in content.splitlines() if l.strip()]
                args = {}
                i = 0
                while i < len(lines):
                    k = lines[i]
                    i += 1
                    if i < len(lines):
                        v = lines[i]
                        i += 1
                        args[k] = v
                    else:
                        args['query'] = k
                if not args:
                    args['query'] = content
                calls.append({'name': t_name, 'arguments': args})
            else:
                calls.append({'name': t_name, 'arguments': {'input': content}})

    # 2. XML style: <tool_call><tool_call>web_search</tool_call><parameter>query</parameter><parameter>...</parameter></tool_call>
    xml_matches = re.findall(r'<tool_call>\s*([a-zA-Z0-9_-]+)\s*</tool_call>([\s\S]*?)</tool_call>', text)
    for t_name, param_block in xml_matches:
        if t_name in known_tools:
            params = re.findall(r'<parameter[^>]*>([\s\S]*?)</parameter>', param_block)
            args = {}
            if len(params) == 2 and params[0].strip() == 'query':
                args = {'query': params[1].strip()}
            elif len(params) == 1:
                args = {'query' if 'search' in t_name else 'input': params[0].strip()}
            elif len(params) >= 2:
                for idx in range(0, len(params) - 1, 2):
                    args[params[idx].strip()] = params[idx+1].strip()
            calls.append({'name': t_name, 'arguments': args})

    # 3. Pipe style: e.g. 6vweb_search|query=GTA 6 Cyberleek
    pipe_matches = re.findall(r'(?:[0-9]*v)?([a-zA-Z0-9_-]+)\|query=(.*?)(?:\n|$)', text)
    for t_name, query_val in pipe_matches:
        if t_name in known_tools:
            calls.append({'name': t_name, 'arguments': {'query': query_val.strip()}})

    # 4. Block style: <tool_call>...</tool_call> or <invoke>...</invoke>
    if not calls:
        blocks = re.findall(r'<(?:tool_call|invoke)>([\s\S]*?)(?:</(?:tool_call|invoke)>|$)', text)
        for block in blocks:
            stripped = block.strip()
            try:
                parsed = json.loads(stripped)
                if isinstance(parsed, dict) and ('name' in parsed or 'tool' in parsed):
                    name = parsed.get('name') or parsed.get('tool')
                    args = parsed.get('arguments') or parsed.get('parameters') or parsed.get('args') or {}
                    calls.append({'name': name, 'arguments': args if isinstance(args, dict) else {}})
                    continue
                elif isinstance(parsed, list):
                    for item in parsed:
                        if isinstance(item, dict) and ('name' in item or 'tool' in item):
                            name = item.get('name') or item.get('tool')
                            args = item.get('arguments') or item.get('parameters') or item.get('args') or {}
                            calls.append({'name': name, 'arguments': args if isinstance(args, dict) else {}})
                    continue
            except Exception:
                pass

            lines = [l.strip() for l in stripped.splitlines() if l.strip() and not l.strip().startswith('</') and not l.strip().startswith('<')]
            i = 0
            while i < len(lines):
                line = lines[i]
                if line in known_tools:
                    t_name = line
                    i += 1
                    args = {}
                    while i < len(lines) and lines[i] not in known_tools:
                        k = lines[i]
                        i += 1
                        if i < len(lines) and lines[i] not in known_tools:
                            v = lines[i]
                            i += 1
                            args[k] = v
                        else:
                            args['query' if 'search' in t_name else 'input'] = k
                    calls.append({'name': t_name, 'arguments': args})
                else:
                    i += 1
    return calls

def clean_tool_markup(t: str, is_token: bool = False) -> str:
    for _ in range(3):
        t = re.sub(r'<tool_call>[\s\S]*?</tool_call>', '', t)
        t = re.sub(r'<invoke>[\s\S]*?</invoke>', '', t)
        t = re.sub(r'<parameter[^>]*>[\s\S]*?</parameter>', '', t)
    t = re.sub(r'</?(?:tool_call|invoke|parameter|function|think|thinking)[^>]*>', '', t)
    t = re.sub(r'(?:[0-9]*v)?[a-zA-Z0-9_-]+\|query=.*?(?:\n|$)', '', t)
    for tool_name in ["python_exec", "bash_exec", "web_search", "fetch_webpage", "vault_search_notes", "vault_write_note", "memory_store", "memory_recall"]:
        t = re.sub(rf'```{tool_name}\n[\s\S]*?```', '', t)
    t = re.sub(r'<\|(?:eos|end_of_text|eot_id|im_end)\|>', '', t)
    if is_token:
        return t
    return t.strip()


def format_dynamic_tool_phrases(tool_name: str, tool_args: Dict[str, Any]) -> tuple[str, str]:
    """
    Produces dynamic, human-readable short execution phrases for the thinking block.
    Flow: e.g. "Running the ls command..." -> "Reading the ls output..." -> "Ready to serve to the user..."
    """
    cmd = str(tool_args.get("command") or "").strip()
    path = str(tool_args.get("path") or tool_args.get("filename") or tool_args.get("filepath") or tool_args.get("target_path") or "").strip()
    query = str(tool_args.get("query") or "").strip()
    url = str(tool_args.get("url") or "").strip()

    if tool_name in ("bash_exec", "computer_run_command") and cmd:
        tokens = cmd.split()
        first = tokens[0].split("/")[-1].split("\\")[-1] if tokens else "bash"
        if len(tokens) > 1 and first in ("git", "apt", "apt-get", "npm", "pip", "docker", "pnpm", "yarn", "systemctl", "service"):
            short_cmd = f"{first} {tokens[1]}"
        else:
            short_cmd = first
        return f"Running the {short_cmd} command...", f"Reading the {short_cmd} output..."
    elif "file" in tool_name or "read" in tool_name or "write" in tool_name or "edit" in tool_name:
        fname = path.split("/")[-1].split("\\")[-1] if path else "file"
        action = "Reading" if "read" in tool_name else ("Writing" if "write" in tool_name else "Editing")
        return f"{action} {fname}...", f"Analyzing {fname} content..."
    elif "list_directory" in tool_name or "dir" in tool_name:
        folder = path.split("/")[-1].split("\\")[-1] if path and path != "." else "directory"
        return f"Inspecting the {folder}...", f"Reading directory structure..."
    elif "search" in tool_name or "web" in tool_name:
        q_short = query[:32] + ("..." if len(query) > 32 else "") if query else "web"
        return f"Searching the web for {q_short}...", f"Reading search results..."
    elif "fetch" in tool_name:
        domain = url.split("//")[-1].split("/")[0] if url else "webpage"
        return f"Fetching {domain}...", f"Reading page output..."
    elif "status" in tool_name or "diagnostics" in tool_name:
        return "Checking system metrics...", "Reading diagnostic output..."
    elif "knowledge" in tool_name:
        return "Querying knowledge base...", "Reading knowledge records..."
    else:
        clean = tool_name.replace("_", " ")
        return f"Running {clean}...", f"Reading {clean} output..."


def generate_dynamic_thinking_steps(prompt: str) -> List[str]:
    """
    Dynamically crafts AI short thoughts matching the user query:
    Analysing the request -> Executing / Investigating -> Inspecting output -> Organising for user -> Ready to serve
    Every user query gets uniquely tailored phrasing.
    """
    clean_p = prompt.strip()
    p_lower = clean_p.lower()

    if any(p_lower.startswith(k) for k in ["run ", "exec ", "execute "]):
        cmd = re.sub(r'^(?:run|exec|execute)\s+', '', clean_p, flags=re.I).strip()
        short_cmd = cmd.split()[0] if cmd else "command"
        return [
            f"Analysing the request to run {short_cmd}...",
            f"Executing the {short_cmd} command on the server...",
            f"Inspecting the {short_cmd} output and results...",
            "Organising findings for the user...",
            "Ready to serve..."
        ]
    elif any(k in p_lower for k in ["name", "who are you", "who r u"]):
        return [
            "Analysing the identity request...",
            "Accessing Hermes Agent sovereign persona...",
            "Organising introduction for user...",
            "Ready to serve..."
        ]
    elif any(k in p_lower for k in ["what can you do", "capabilities", "your features", "help me with"]):
        return [
            "Analysing the request about capabilities...",
            "Surveying autonomous tools and agentic workflows...",
            "Organising capability list for user...",
            "Ready to serve..."
        ]
    elif any(k in p_lower for k in ["write", "code", "create a function", "script", "program", "implement"]):
        topic = re.sub(r'^(?:please\s+|can\s+you\s+)?(?:write|create|implement|code)\s+(?:a\s+|an\s+)?', '', clean_p, flags=re.I).strip()
        topic_short = topic[:28] + ("..." if len(topic) > 28 else "") if topic else "code"
        return [
            f"Analysing the request for {topic_short}...",
            "Formulating architecture and logic...",
            "Inspecting syntax and edge cases...",
            "Organising code for user...",
            "Ready to serve..."
        ]
    elif any(k in p_lower for k in ["search", "find", "research", "lookup", "who is", "what is"]):
        query = re.sub(r'^(?:please\s+|can\s+you\s+)?(?:search|find|research|lookup)\s+(?:for\s+)?', '', clean_p, flags=re.I).strip()
        query_short = query[:28] + ("..." if len(query) > 28 else "") if query else "topic"
        return [
            f"Analysing research request for {query_short}...",
            "Investigating verified knowledge sources...",
            "Inspecting findings and accuracy...",
            "Organising insights for user...",
            "Ready to serve..."
        ]
    else:
        clause = clean_p[:30] + ("..." if len(clean_p) > 30 else "")
        return [
            f"Analysing the request for {clause}...",
            "Evaluating context and optimal approach...",
            "Inspecting details and solutions...",
            "Organising for user...",
            "Ready to serve..."
        ]


class HermesAgent:
    def __init__(self, upstream_url: str = UPSTREAM_URL, api_key: str = UPSTREAM_API_KEY):
        self.upstream_url = upstream_url
        self.api_key = (api_key or "").strip()
        client_headers = {}
        if self.api_key:
            client_headers["Authorization"] = f"Bearer {self.api_key}"
        self.http_client = httpx.AsyncClient(
            base_url=self.upstream_url,
            headers=client_headers,
            timeout=120.0,
            follow_redirects=True
        )
        self.omniroute = OmniRouteAdapter(upstream_url=self.upstream_url, api_key=self.api_key)

    def _resolve_candidate_models(self, requested_model: Optional[str], prompt: str = "") -> List[str]:
        """Builds an ordered fallback list of models dynamically tailored to task complexity."""
        candidates = []
        if requested_model:
            req_lower = requested_model.lower().strip()
            if requested_model not in ["default", "hermes-agent", "hermes"] and not req_lower.startswith("auto/"):
                if "/" in requested_model and not requested_model.startswith("omniroute/"):
                    candidates.append(requested_model)
                elif not any(k in req_lower for k in ["claude", "sonnet", "opus", "haiku", "gpt", "omniroute", "auto"]):
                    candidates.append(requested_model)

        tier = registry.classify_task_tier(prompt) if prompt else "fast"
        p_lower = prompt.lower().strip()
        is_greeting_or_fast = len(p_lower.split()) <= 4 and any(g in p_lower for g in ["hi", "hello", "hey", "who are you", "what can you do", "help", "ping", "test", "thanks", "ok"])

        if is_greeting_or_fast:
            tier_cascade = [
                "antigravity/gemini-2.5-flash",
                "groq/llama-3.3-70b-versatile",
            ]
        elif tier == "coding":
            tier_cascade = [
                "antigravity/gemini-2.5-flash",
                "groq/llama-3.3-70b-versatile",
            ]
        elif tier == "reasoning":
            tier_cascade = [
                "antigravity/gemini-2.5-flash",
                "groq/llama-3.3-70b-versatile",
            ]
        else:
            tier_cascade = [
                "antigravity/gemini-2.5-flash",
                "groq/llama-3.3-70b-versatile",
            ]

        for model_id in tier_cascade:
            if model_id not in candidates:
                candidates.append(model_id)

        return candidates

    async def stream_chat(
        self,
        messages: List[Dict[str, Any]],
        model: Optional[str] = None,
        system: Optional[str] = None,
        temperature: float = 0.7,
        enable_dynamic_tools: bool = True
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        High-performance Hermes Agent reasoning loop with dynamic tool execution & automatic model failover.
        """
        # 1. Extract prompt & messages first
        last_user_msg = ""
        user_msgs = []
        for m in messages:
            if m.get("role") != "system":
                user_msgs.append(m)
            if m.get("role") == "user":
                content = m.get("content", "")
                if isinstance(content, str):
                    last_user_msg = content
                elif isinstance(content, list):
                    for part in content:
                        if isinstance(part, dict) and part.get("type") == "text":
                            last_user_msg += part.get("text", "")

        # 2. RAG Context Injection from Semantic Vector Database
        rag_context = ""
        try:
            from hermes_core.tools.memory_tools import search_semantic_memory
            if last_user_msg and len(last_user_msg.strip()) > 3:
                recalled = search_semantic_memory(last_user_msg, top_k=2, threshold=0.18)
                if recalled:
                    rag_blocks = [f"[{m['title']}]: {m['content']}" for m in recalled]
                    rag_context = "\n\n[Relevant Long-Term Memory & Project Context Retrieved from Vector Database]:\n" + "\n\n".join(rag_blocks)
        except Exception as e:
            logger.debug(f"Semantic RAG recall notice: {e}")

        # 3. Build candidate models & tools
        candidate_models = self._resolve_candidate_models(model, prompt=last_user_msg)
        
        full_system = HERMES_MASTER_SYSTEM_PROMPT
        if rag_context:
            full_system += rag_context
        if system:
            full_system += f"\n\nUser Context:\n{system}"

        payload_messages = [{"role": "system", "content": full_system}] + user_msgs

        tools = []
        if enable_dynamic_tools:
            tools = registry.select_tools_for_prompt(last_user_msg)

        known_tools = set(registry._tools.keys())
        stream_succeeded = False
        last_error = ""

        for candidate in candidate_models:
            current_messages = list(payload_messages)
            gathered_data_blocks = []
            executed_tool_signatures = set()
            req_start = time.time()
            try:
                # Stage 1: Autonomous Tool Execution. Tool results are fed back
                # into the model so multi-step server work can continue instead
                # of stopping after the first shell command.
                dyn_steps = generate_dynamic_thinking_steps(last_user_msg)
                if dyn_steps:
                    yield {
                        "type": "thinking",
                        "content": f"{dyn_steps[0]}\n"
                    }
                    if not tools and len(dyn_steps) > 1:
                        await asyncio.sleep(0.08)
                        yield {
                            "type": "thinking",
                            "content": f"{dyn_steps[1]}\n"
                        }

                if tools:
                    for step in range(MAX_TOOL_ROUNDS):
                        if step > 0:
                            yield {
                                "type": "thinking",
                                "content": f"Evaluating results and planning next step...\n"
                            }

                        req_body = {
                            "model": candidate,
                            "messages": current_messages,
                            "temperature": temperature,
                            "stream": True,
                            "tools": tools,
                            "tool_choice": "auto"
                        }

                        raw_text_accum = ""
                        tool_calls_buffer = {}

                        async with self.http_client.stream("POST", "/chat/completions", json=req_body, timeout=httpx.Timeout(60.0, connect=10.0)) as response:
                            if response.status_code != 200:
                                break

                            async for line in response.aiter_lines():
                                line = line.strip()
                                if not line or not line.startswith("data:"):
                                    continue
                                data_str = line[5:].strip()
                                if data_str == "[DONE]":
                                    break
                                
                                try:
                                    chunk = json.loads(data_str)
                                    choices = chunk.get("choices", [])
                                    if not choices:
                                        continue
                                    delta = choices[0].get("delta", {})
                                    
                                    if "reasoning_content" in delta and delta["reasoning_content"]:
                                        yield {"type": "thinking", "content": delta["reasoning_content"]}

                                    if "content" in delta and delta["content"] is not None:
                                        raw_text_accum += delta["content"]

                                    if "tool_calls" in delta and delta["tool_calls"]:
                                        for tc in delta["tool_calls"]:
                                            idx = tc.get("index", 0)
                                            if idx not in tool_calls_buffer:
                                                tool_calls_buffer[idx] = {
                                                    "id": tc.get("id", f"call_{idx}"),
                                                    "name": "",
                                                    "arguments": ""
                                                }
                                            if "function" in tc:
                                                if "name" in tc["function"] and tc["function"]["name"]:
                                                    tool_calls_buffer[idx]["name"] = tc["function"]["name"]
                                                if "arguments" in tc["function"] and tc["function"]["arguments"]:
                                                    tool_calls_buffer[idx]["arguments"] += tc["function"]["arguments"]
                                except Exception:
                                    continue

                        text_tool_calls = extract_text_tool_calls(raw_text_accum, known_tools)
                        all_tool_calls = []

                        for idx, tc_data in sorted(tool_calls_buffer.items()):
                            try:
                                t_args = json.loads(tc_data["arguments"]) if tc_data["arguments"] else {}
                            except Exception:
                                t_args = {}
                            all_tool_calls.append({
                                "id": tc_data["id"],
                                "name": tc_data["name"],
                                "arguments": t_args
                            })

                        for ttc in text_tool_calls:
                            call_id = f"call_text_{len(all_tool_calls)}_{step}"
                            all_tool_calls.append({
                                "id": call_id,
                                "name": ttc["name"],
                                "arguments": ttc["arguments"]
                            })

                        unique_calls = []
                        for tc in all_tool_calls:
                            sig = f"{tc['name']}:{json.dumps(tc['arguments'], sort_keys=True)}"
                            if sig not in executed_tool_signatures:
                                executed_tool_signatures.add(sig)
                                unique_calls.append(tc)

                        if not unique_calls:
                            p_lower = last_user_msg.lower()
                            urls = re.findall(r'https?://[^\s<>"]+|www\.[^\s<>"]+', last_user_msg)
                            
                            # 1. Proactive URL Scraping
                            if urls and "fetch_webpage" in known_tools and not gathered_data_blocks:
                                for target_url in urls[:2]:
                                    domain = target_url.split("//")[-1].split("/")[0]
                                    yield {"type": "thinking", "content": f"Fetching {domain}...\n"}
                                    page_content = await registry.execute_tool("fetch_webpage", {"url": target_url})
                                    gathered_data_blocks.append(f"[fetch_webpage ({target_url})]:\n{page_content}")
                                    yield {"type": "thinking", "content": f"Reading {domain} output...\n"}

                            # 2. Proactive Web Search for Research & Current Topics
                            research_keywords = [
                                "research", "investigate", "find information", "search", "who is", "what is",
                                "leak", "leaks", "tell me about", "latest news", "cyberleek", "cyber", "internet",
                                "sources", "check online"
                            ]
                            has_research_intent = any(k in p_lower for k in research_keywords)
                            if step == 0 and has_research_intent and "web_search" in known_tools and not gathered_data_blocks:
                                clean_query = re.sub(r'^(?:please\s+|can\s+you\s+|research\s+about\s+|search\s+for\s+|investigate\s+)', '', last_user_msg, flags=re.I).strip()
                                if not clean_query:
                                    clean_query = last_user_msg
                                q_short = clean_query[:32] + ("..." if len(clean_query) > 32 else "")
                                yield {"type": "thinking", "content": f"Searching the web for {q_short}...\n"}
                                result_str = await registry.execute_tool("web_search", {"query": clean_query})
                                gathered_data_blocks.append(f"[web_search ({clean_query})]:\n{result_str}")
                                yield {"type": "thinking", "content": "Reading search results...\n"}

                            # 3. Proactive Knowledge Search across Notion (Primary) & Obsidian
                            knowledge_keywords = [
                                "what do i know", "project", "projects", "task", "tasks", "decision", "decisions",
                                "plan", "plans", "roadmap", "architecture", "what did we decide", "my notes",
                                "status of", "what are we working on", "notion", "workspace", "todo", "adr"
                            ]
                            has_knowledge_intent = any(k in p_lower for k in knowledge_keywords)
                            if step == 0 and has_knowledge_intent and "search_knowledge" in known_tools and not gathered_data_blocks:
                                yield {"type": "thinking", "content": "Querying knowledge base...\n"}
                                k_result_str = await registry.execute_tool("search_knowledge", {"query": last_user_msg, "limit": 4})
                                gathered_data_blocks.append(f"[search_knowledge (Notion Primary)]:\n{k_result_str}")
                                yield {"type": "thinking", "content": "Reading knowledge records...\n"}

                            # 4. Proactive Server Computer & Storage status
                            computer_keywords = ["server", "computer", "disk", "storage", "workspace", "workspaces", "system status", "diagnostics"]
                            if step == 0 and any(k in p_lower for k in computer_keywords) and "computer_system_status" in known_tools and not any("computer_system_status" in b for b in gathered_data_blocks):
                                yield {"type": "thinking", "content": "Checking system metrics...\n"}
                                status_str = await registry.execute_tool("computer_system_status", {})
                                gathered_data_blocks.append(f"[computer_system_status]:\n{status_str}")
                                yield {"type": "thinking", "content": "Reading diagnostic output...\n"}

                            # 5. Proactive Registered Projects list
                            if step == 0 and any(k in p_lower for k in ["project list", "list projects", "what projects", "active projects"]) and "computer_project_list" in known_tools and not any("computer_project_list" in b for b in gathered_data_blocks):
                                yield {"type": "thinking", "content": "Inspecting registered projects...\n"}
                                plist_str = await registry.execute_tool("computer_project_list", {})
                                gathered_data_blocks.append(f"[computer_project_list]:\n{plist_str}")
                                yield {"type": "thinking", "content": "Reading project records...\n"}

                            # 6. Proactive File & Directory inspection
                            file_keywords = ["what files", "file list", "directory", "codebase", "folder structure", "check repository", "show files"]
                            if step == 0 and any(k in p_lower for k in file_keywords) and "list_directory" in known_tools and not any("list_directory" in b for b in gathered_data_blocks):
                                yield {"type": "thinking", "content": "Running the ls command...\n"}
                                dir_str = await registry.execute_tool("list_directory", {"path": "."})
                                gathered_data_blocks.append(f"[list_directory (.)]:\n{dir_str}")
                                yield {"type": "thinking", "content": "Reading the ls output...\n"}

                            break

                        tool_results = []
                        for tc in unique_calls:
                            tool_name = tc["name"]
                            tool_args = tc["arguments"]
                            start_phrase, done_phrase = format_dynamic_tool_phrases(tool_name, tool_args)
                            query_desc = tool_args.get("query") or tool_args.get("url") or tool_args.get("command") or tool_args.get("project_id") or tool_name

                            exec_phrase = dyn_steps[1] if (dyn_steps and len(dyn_steps) > 1) else start_phrase
                            yield {"type": "thinking", "content": f"{exec_phrase}\n"}
                            result_str = await registry.execute_tool(tool_name, tool_args)
                            tool_results.append(result_str)
                            gathered_data_blocks.append(f"[{tool_name} ({query_desc})]:\n{result_str}")

                            if "error" in result_str.lower() or "failed" in result_str.lower():
                                yield {"type": "thinking", "content": f"Analyzing error in {tool_name}...\n"}
                            else:
                                inspect_phrase = dyn_steps[2] if (dyn_steps and len(dyn_steps) > 2) else done_phrase
                                yield {"type": "thinking", "content": f"{inspect_phrase}\n"}

                        # Preserve the normal OpenAI tool-call conversation
                        # contract. This lets the next round reason over the
                        # actual server result and issue the next operation.
                        if unique_calls:
                            current_messages.append({
                                "role": "assistant",
                                "content": raw_text_accum or None,
                                "tool_calls": [
                                    {
                                        "id": tc["id"],
                                        "type": "function",
                                        "function": {
                                            "name": tc["name"],
                                            "arguments": json.dumps(tc["arguments"], ensure_ascii=False),
                                        },
                                    }
                                    for tc in unique_calls
                                ],
                            })
                            for tc, result_str in zip(unique_calls, tool_results):
                                current_messages.append({
                                    "role": "tool",
                                    "tool_call_id": tc["id"],
                                    "name": tc["name"],
                                    "content": result_str,
                                })

                # Stage 2: Guaranteed Direct Synthesis Stream (WITHOUT TOOLS)
                p_lower = last_user_msg.lower()
                is_coding = any(k in p_lower for k in ["write code", "implement", "script", "function", "algorithm", "debug", "refactor", "create a program", "fastapi", "react", "python code", "flutter", "kotlin"])
                is_analysis = any(k in p_lower for k in ["calculate", "math", "equation", "solve", "formula", "data analysis", "statistics", "integral", "derivative", "matrix"])
                wants_report = any(k in p_lower for k in ["report", "comprehensive", "detailed report", "exhaustive report", "full analysis", "write a report", "documentation report"])

                synth_system = HERMES_MASTER_SYSTEM_PROMPT + "\n\n"
                if gathered_data_blocks:
                    if wants_report:
                        synth_system += (
                            "You have completed live tool research. Deliver the comprehensive, thorough, "
                            "multi-section research report requested by the user."
                        )
                    else:
                        synth_system += (
                            "You have completed tool execution on the server.\n"
                            "Persona & Tone: Sweet, loyal, deeply caring, and respectful companion.\n"
                            "Provide a warm, courteous response presenting the tool output, for example:\n"
                            "'Here is the output of the command that you asked for:\n\n```bash\n...\n```\nPlease let me know if you would like me to inspect any of these files or run anything else for you!'\n"
                            "Never generate unprompted research reports, executive summaries, or multi-section essays."
                        )
                elif is_coding:
                    synth_system += (
                        "You are operating as Hermes Principal Engineer. Provide complete, fully working, "
                        "production-ready code blocks with proper syntax highlighting, type annotations, error handling, "
                        "and verification test cases. Be warm, loyal, and helpful."
                    )
                elif is_analysis:
                    synth_system += (
                        "You are operating as Hermes Quantitative Analyst. "
                        "Deliver precise step-by-step mathematical reasoning, structured markdown comparison tables, "
                        "and KaTeX LaTeX formulas for all equations."
                    )
                else:
                    synth_system += "Deliver sweet, loyal, caring, and respectful assistance directly to the user."

                synth_system += (
                    "\n\nStrict Rules:\n"
                    "- Never state or output the upstream model name (e.g., Qwen, Nemotron, Gemini, Claude, OpenAI, DeepSeek, etc.).\n"
                    "- You are ONLY Hermes Agent.\n"
                    "- When asked your name, who you are, or in greetings, introduce yourself warmly: 'I am Hermes Agent, a powerful agentic AI and your loyal companion!'\n"
                    "- When asked 'what can you do?', comprehensively and warmly explain your capabilities.\n"
                    "- NEVER output <antArtifact> tags or separate artifact sidecards. Render all code, HTML, markdown files, and diagrams directly inside the chat message using standard markdown code fences (```html, ```python, ```markdown, ```mermaid)."
                )

                synth_messages = [{"role": "system", "content": synth_system}]
                for m in user_msgs[:-1]:
                    synth_messages.append(m)

                active_content = last_user_msg
                if gathered_data_blocks:
                    gathered_str = "\n\n".join(gathered_data_blocks)
                    if wants_report:
                        active_content = (
                            f"{last_user_msg}\n\n[Verified Research Data gathered from Live Tools]:\n{gathered_str}\n\n"
                            f"[Instruction]: Write out the comprehensive, detailed research report as requested by the user."
                        )
                    else:
                        active_content = (
                            f"{last_user_msg}\n\n[Tool Execution Output]:\n{gathered_str}\n\n"
                            f"[Instruction]: Present the output of the command/tool in a sweet, loyal, and respectful conversation (e.g. 'Here is the output of the command that you asked for:' followed by the output code block and a courteous offer for next steps). Do NOT write an unrequested executive summary or essay."
                        )

                synth_messages.append({"role": "user", "content": active_content})

                synth_req = {
                    "model": candidate,
                    "messages": synth_messages,
                    "temperature": temperature,
                    "stream": True
                }

                if dyn_steps and len(dyn_steps) > 3:
                    yield {
                        "type": "thinking",
                        "content": f"{dyn_steps[3]}\n"
                    }
                    await asyncio.sleep(0.06)

                ready_phrase = dyn_steps[-1] if dyn_steps else "Ready to serve..."
                yield {
                    "type": "thinking",
                    "content": f"{ready_phrase}\n"
                }

                inside_think = False
                stream_succeeded = False

                async with self.http_client.stream("POST", "/chat/completions", json=synth_req, timeout=httpx.Timeout(180.0, connect=15.0, read=180.0)) as synth_resp:
                    if synth_resp.status_code != 200:
                        err_text = await synth_resp.aread()
                        last_error = f"Upstream {candidate} ({synth_resp.status_code}): {err_text.decode('utf-8', errors='ignore')}"
                        continue

                    async for line in synth_resp.aiter_lines():
                        line = line.strip()
                        if not line or not line.startswith("data:"):
                            continue
                        data_str = line[5:].strip()
                        if data_str == "[DONE]":
                            break
                        
                        try:
                            chunk = json.loads(data_str)
                            choices = chunk.get("choices", [])
                            if not choices:
                                continue
                            delta = choices[0].get("delta", {})
                            
                            if "reasoning_content" in delta and delta["reasoning_content"]:
                                yield {"type": "thinking", "content": delta["reasoning_content"]}

                            if "content" in delta and delta["content"]:
                                token = delta["content"]

                                if "<think>" in token or "<thinking>" in token:
                                    inside_think = True
                                    parts = re.split(r'<think>|<thinking>', token, maxsplit=1)
                                    if parts[0]:
                                        clean_tok = clean_tool_markup(parts[0], is_token=True)
                                        if clean_tok:
                                            yield {"type": "text", "content": clean_tok}
                                            stream_succeeded = True
                                    token = parts[1] if len(parts) > 1 else ""
                                
                                if inside_think:
                                    if "</think>" in token or "</thinking>" in token:
                                        inside_think = False
                                        parts = re.split(r'</think>|</thinking>', token, maxsplit=1)
                                        if parts[0]:
                                            yield {"type": "thinking", "content": parts[0]}
                                        token = parts[1] if len(parts) > 1 else ""
                                    else:
                                        yield {"type": "thinking", "content": token}
                                        token = ""

                                if token and not inside_think:
                                    clean_tok = clean_tool_markup(token, is_token=True)
                                    if clean_tok:
                                        yield {"type": "text", "content": clean_tok}
                                        stream_succeeded = True
                        except Exception:
                            continue

                if stream_succeeded:
                    # Autonomous Decision / Milestone Persistence to Notion
                    p_lower = last_user_msg.lower()
                    if any(k in p_lower for k in ["decision", "decide", "architect", "architecture", "adr", "milestone"]):
                        try:
                            from harness.knowledge.models import WriteIntent
                            from harness.knowledge.router import KnowledgeRouter
                            asyncio.create_task(
                                KnowledgeRouter().write(WriteIntent(
                                    source="notion",
                                    title=f"Decision: {last_user_msg[:60].strip()}",
                                    content=f"### Context\n{last_user_msg}\n\n### Autonomous Resolution / Plan\n{active_content[:1500]}",
                                    tags=["autonomous-decision", "hermes"],
                                    category="decision"
                                ))
                            )
                        except Exception as e:
                            logger.debug(f"Autonomous Notion persistence notice: {e}")

                    if hasattr(self, "omniroute"):
                        from hermes_core.omniroute_adapter import InferenceTelemetry
                        self.omniroute._record_telemetry(InferenceTelemetry(
                            request_id=f"req_{int(time.time() * 1000)}",
                            requested_model=model or candidate,
                            routed_model=candidate,
                            total_latency_ms=int((time.time() - req_start) * 1000),
                            status="success",
                        ))
                    break

            except Exception as e:
                last_error = f"Candidate {candidate} connection failed: {str(e)}"
                if hasattr(self, "omniroute"):
                    from hermes_core.omniroute_adapter import InferenceTelemetry
                    self.omniroute._record_telemetry(InferenceTelemetry(
                        request_id=f"req_{int(time.time() * 1000)}",
                        requested_model=model or candidate,
                        routed_model=candidate,
                        total_latency_ms=int((time.time() - req_start) * 1000),
                        status=f"error: {str(e)}",
                    ))
                continue

        if not stream_succeeded:
            yield {"type": "error", "error": f"All fallback models exhausted. Last error: {last_error}"}

agent = HermesAgent()

# Internal FastAPI microservice on port 8642
from fastapi import FastAPI, Request
from fastapi.responses import StreamingResponse, JSONResponse
import uvicorn

app = FastAPI(title="Hermes Agent Core", version="2.0.0")

@app.get("/health")
async def health():
    return {"status": "ok", "service": "hermes_core", "tools_count": len(registry._tools)}

@app.get("/v1/omniroute/telemetry")
async def omniroute_telemetry():
    """Exposes inference telemetry recorded by OmniRouteAdapter under Hermes authority."""
    return {
        "status": "ok",
        "upstream_url": agent.upstream_url,
        "recent_telemetry": agent.omniroute.get_recent_telemetry() if hasattr(agent, "omniroute") else [],
    }

@app.post("/v1/chat")
async def chat_endpoint(request: Request):
    data = await request.json()
    messages = data.get("messages", [])
    model = data.get("model")
    system = data.get("system")
    temperature = data.get("temperature", 0.7)

    async def event_generator():
        async for item in agent.stream_chat(messages, model=model, system=system, temperature=temperature):
            yield f"data: {json.dumps(item)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@app.post("/v1/chat/completions")
async def openai_chat_completions(request: Request):
    """OpenAI-compatible adapter used by the Telegram/channel gateway."""
    data = await request.json()
    messages = data.get("messages", [])
    model = data.get("model")
    system = data.get("system")
    temperature = data.get("temperature", 0.7)
    stream = bool(data.get("stream", True))
    response_id = f"chatcmpl-hermes-{int(time.time() * 1000)}"
    response_model = model or "hermes-agent"

    if not stream:
        text_parts = []
        thinking_parts = []
        error = None
        async for item in agent.stream_chat(
            messages,
            model=model,
            system=system,
            temperature=temperature,
        ):
            if item.get("type") == "text":
                text_parts.append(item.get("content", ""))
            elif item.get("type") == "thinking":
                thinking_parts.append(item.get("content", ""))
            elif item.get("type") == "error":
                error = item.get("error")
        content = "".join(text_parts)
        if error and not content:
            content = f"⚠️ {error}"
        message = {"role": "assistant", "content": content}
        if thinking_parts:
            message["reasoning_content"] = "".join(thinking_parts)
        return JSONResponse({
            "id": response_id,
            "object": "chat.completion",
            "created": int(time.time()),
            "model": response_model,
            "choices": [{"index": 0, "message": message, "finish_reason": "stop"}],
        })

    async def completion_events():
        def chunk(delta: Dict[str, Any], finish_reason: Optional[str] = None) -> str:
            return (
                f"data: {json.dumps({'id': response_id, 'object': 'chat.completion.chunk', 'created': int(time.time()), 'model': response_model, 'choices': [{'index': 0, 'delta': delta, 'finish_reason': finish_reason}]}, ensure_ascii=False)}\n\n"
            )

        yield chunk({"role": "assistant", "content": ""})
        async for item in agent.stream_chat(
            messages,
            model=model,
            system=system,
            temperature=temperature,
        ):
            item_type = item.get("type")
            content = item.get("content", "")
            if item_type == "text" and content:
                yield chunk({"content": content})
            elif item_type == "thinking" and content:
                yield chunk({"reasoning_content": content})
            elif item_type == "error":
                yield chunk({"content": f"⚠️ {item.get('error', 'Agent execution failed')}"})
        yield chunk({}, "stop")
        yield "data: [DONE]\n\n"

    return StreamingResponse(completion_events(), media_type="text/event-stream")


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8642)
