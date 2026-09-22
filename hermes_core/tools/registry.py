import json
import inspect
import re
from typing import Dict, Any, List, Callable, Optional

class ToolRegistry:
    def __init__(self):
        self._tools: Dict[str, Dict[str, Any]] = {}
        self._handlers: Dict[str, Callable] = {}
        self._categories: Dict[str, List[str]] = {
            "web": [],
            "coding": [],
            "files": [],
            "vault": [],
            "memory": [],
            "system": []
        }
        self._enabled_categories: set = {"web", "coding", "files", "vault", "memory", "system"}

    def register(self, name: str, description: str, parameters: Dict[str, Any], category: str = "system"):
        def decorator(fn: Callable):
            schema = {
                "type": "function",
                "function": {
                    "name": name,
                    "description": description,
                    "parameters": parameters
                }
            }
            self._tools[name] = {
                "schema": schema,
                "category": category,
                "description": description
            }
            self._handlers[name] = fn
            if category not in self._categories:
                self._categories[category] = []
            if name not in self._categories[category]:
                self._categories[category].append(name)
            return fn
        return decorator

    def enable_category(self, category: str):
        self._enabled_categories.add(category)

    def disable_category(self, category: str):
        self._enabled_categories.discard(category)

    def get_all_tools(self) -> List[Dict[str, Any]]:
        return [
            meta["schema"] for name, meta in self._tools.items()
            if meta["category"] in self._enabled_categories
        ]

    def select_tools_for_prompt(self, prompt: str, user_requested_tools: Optional[List[str]] = None) -> List[Dict[str, Any]]:
        """
        Intelligent context-aware tool selection.
        - Returns [] for ultra-short trivial greetings (0ms tool overhead).
        - For all substantive tasks, queries, and instructions, equips Hermes with
          the full autonomous tool suite across web, coding, vault (Notion/Obsidian),
          memory, and system (Server Computer) so Hermes can act without explicit user prompts.
        """
        if user_requested_tools:
            return [
                self._tools[name]["schema"] for name in user_requested_tools
                if name in self._tools and self._tools[name]["category"] in self._enabled_categories
            ]

        p = prompt.lower().strip()
        words = p.split()
        
        # Fast path: instant conversational response for pure greetings or single acknowledgments
        trivial_greetings = {"hi", "hello", "hey", "sup", "thanks", "thank", "you", "ok", "okay", "k", "bye", "ping"}
        if len(words) <= 3 and all(re.sub(r'[^a-z]', '', w) in trivial_greetings for w in words if re.sub(r'[^a-z]', '', w)):
            return []

        # For all substantive queries, provide full autonomous access across all enabled categories
        result = []
        for cat in ["vault", "coding", "files", "web", "memory", "system"]:
            if cat in self._enabled_categories:
                for tool_name in self._categories.get(cat, []):
                    if tool_name in self._tools:
                        result.append(self._tools[tool_name]["schema"])
        return result

    async def execute_tool(self, name: str, arguments: Dict[str, Any]) -> str:
        if name not in self._handlers:
            return json.dumps({"error": f"Tool '{name}' not found."})
        fn = self._handlers[name]
        try:
            if inspect.iscoroutinefunction(fn):
                res = await fn(**arguments)
            else:
                res = fn(**arguments)
            if isinstance(res, (dict, list)):
                return json.dumps(res, ensure_ascii=False)
            return str(res)
        except Exception as e:
            return json.dumps({"error": f"Tool execution failed: {str(e)}"})


    def classify_task_tier(self, prompt: str) -> str:
        """
        Intelligently determines the ideal model tier based on query complexity.
        - 'coding': Complex software engineering, programming, scripting, debugging -> auto/best-coding
        - 'reasoning': Deep analysis, logic puzzles, multi-step research, architecture -> auto/best-reasoning
        - 'chat': High-quality conversational, creative writing -> auto/best-chat
        - 'fast': Quick questions, greetings, everyday conversation -> auto/best-fast
        """
        p = prompt.lower()
        
        # Coding & Debugging
        if any(w in p for w in ["def ", "class ", "function", "import ", "sql", "html", "css", "javascript", "python", "dockerfile", "refactor", "bug", "traceback", "syntaxerror", "write a script", "code"]):
            return "coding"
            
        # Deep Reasoning & Research
        if any(w in p for w in ["research", "investigate", "compare and contrast", "architect", "deep dive", "prove", "step-by-step reasoning", "analyze tradeoffs", "strategy", "algorithm", "full report", "detailed report"]):
            return "reasoning"

        # General High Quality
        if len(prompt.split()) > 40:
            return "chat"
            
        # Fast Everyday Interaction
        return "fast"

registry = ToolRegistry()
