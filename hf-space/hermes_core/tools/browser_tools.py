"""
Browser Agent Tools
===================
Exposes autonomous Playwright/headless browser actions to Hermes Agent.
"""
from __future__ import annotations

import logging
from typing import Optional, Dict, Any
from hermes_core.tools.registry import registry

logger = logging.getLogger("hermes.tools.browser")


@registry.register(
    name="browser_navigate",
    description="Navigate the headless browser to an external URL and retrieve page title, clean text content, and extracted links.",
    parameters={
        "type": "object",
        "properties": {
            "url": {"type": "string", "description": "The HTTP or HTTPS URL to load"}
        },
        "required": ["url"],
    },
    category="browser",
    timeout_seconds=60,
)
async def browser_navigate(url: str) -> str:
    url = url.strip()
    if not url.startswith("http://") and not url.startswith("https://"):
        url = "https://" + url
    from harness.browser.browser_manager import get_browser_manager
    b_mgr = get_browser_manager()
    res = await b_mgr.navigate(url)
    from harness.security.prompt_isolation import wrap_untrusted_content
    wrapped_text = wrap_untrusted_content(res.text_content, source_type="web_page", source_id=url)
    lines = [
        f"### Web Browser Navigation: {res.title}",
        f"- **URL**: `{res.url}`",
        f"- **HTTP Status**: `{res.status_code}`",
        f"- **Latency**: `{res.latency_ms}ms`",
        f"\n**Page Content:**\n{wrapped_text[:4000]}",
    ]
    if res.links:
        lines.append("\n**Top Links Found:**")
        for l in res.links[:8]:
            lines.append(f"- [{l['text']}]({l['href']})")
    return "\n".join(lines)


@registry.register(
    name="browser_click",
    description="Click an interactive HTML element matching a CSS or text selector in the active browser page.",
    parameters={
        "type": "object",
        "properties": {
            "selector": {
                "type": "string",
                "description": "CSS selector or text selector to click, e.g. '#submit-btn' or 'button:has-text(\"Login\")'",
            }
        },
        "required": ["selector"],
    },
    category="browser",
    side_effects=True,
    timeout_seconds=30,
)
async def browser_click(selector: str) -> str:
    from harness.browser.browser_manager import get_browser_manager
    b_mgr = get_browser_manager()
    res = await b_mgr.click(selector=selector)
    if res.get("status") == "success":
        return f"Successfully clicked element '{selector}'. Current URL: `{res.get('current_url')}` (Title: {res.get('title')})"
    return f"Browser click failed: {res.get('message')}"


@registry.register(
    name="browser_type",
    description="Fill or type text into a form input or textarea matching a CSS selector.",
    parameters={
        "type": "object",
        "properties": {
            "selector": {
                "type": "string",
                "description": "CSS selector for the input element, e.g. 'input[name=\"email\"]'",
            },
            "text": {
                "type": "string",
                "description": "Text content to type into the field",
            },
        },
        "required": ["selector", "text"],
    },
    category="browser",
    side_effects=True,
    timeout_seconds=30,
)
async def browser_type(selector: str, text: str) -> str:
    from harness.browser.browser_manager import get_browser_manager
    b_mgr = get_browser_manager()
    res = await b_mgr.type(selector=selector, text=text)
    if res.get("status") == "success":
        return f"Successfully typed text into '{selector}'."
    return f"Browser type failed: {res.get('message')}"


@registry.register(
    name="browser_screenshot",
    description="Capture a screenshot of the active browser viewport for visual verification.",
    parameters={
        "type": "object",
        "properties": {
            "output_path": {
                "type": "string",
                "description": "Optional file path to save the screenshot image PNG",
            }
        },
    },
    category="browser",
    timeout_seconds=30,
)
async def browser_screenshot(output_path: Optional[str] = None) -> str:
    from harness.browser.browser_manager import get_browser_manager
    b_mgr = get_browser_manager()
    res = await b_mgr.screenshot(output_path=output_path)
    if res.get("status") == "success":
        return f"Captured viewport screenshot: `{res.get('file_path')}` (Page: {res.get('current_url')})"
    return f"Screenshot capture failed: {res.get('message')}"
