"""First-Class Browser Automation Subsystem.

Provides controlled, headless browser automation:
- Playwright Chromium driver for full DOM automation (click, type, screenshot, JS evaluation).
- Session isolation with dedicated page and cookie contexts.
- Robust HTTP + BeautifulSoup fallback mode when browser binaries are not installed.
- Safe navigation with status code inspection and content isolation.
- Enforced under the TrustHierarchy policy engine.
"""

from __future__ import annotations

import asyncio
import base64
import logging
import os
import re
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import httpx

logger = logging.getLogger(__name__)


@dataclass
class BrowserNavigationResult:
    url: str
    status_code: int
    title: str
    text_content: str
    links: List[Dict[str, str]] = field(default_factory=list)
    latency_ms: float = 0.0


class BrowserManager:
    """Manages browser sessions, web requests, DOM interaction, and JavaScript execution."""

    def __init__(self, default_timeout: float = 20.0) -> None:
        self.default_timeout = default_timeout
        self._sessions: Dict[str, Dict[str, Any]] = {}
        self._playwright = None
        self._browser = None
        self._contexts: Dict[str, Any] = {}
        self._pages: Dict[str, Any] = {}
        self._playwright_available: Optional[bool] = None

    async def _init_playwright(self) -> bool:
        """Attempt to initialize Playwright and launch headless Chromium."""
        if self._playwright_available is False:
            return False
        if self._browser is not None:
            return True

        try:
            from playwright.async_api import async_playwright
            self._playwright = await async_playwright().start()
            self._browser = await self._playwright.chromium.launch(
                headless=True,
                args=["--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"]
            )
            self._playwright_available = True
            logger.info("Playwright headless Chromium launched successfully.")
            return True
        except Exception as e:
            logger.warning("Playwright Chromium unavailable (%s); operating in HTTP fallback mode.", e)
            self._playwright_available = False
            return False

    async def _get_page(self, session_id: str = "default") -> Optional[Any]:
        """Retrieve or create an active Playwright page for the session."""
        if not await self._init_playwright():
            return None
        if session_id in self._pages:
            return self._pages[session_id]

        context = await self._browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 HermesAgent/2.0",
            viewport={"width": 1280, "height": 800}
        )
        page = await context.new_page()
        page.set_default_timeout(int(self.default_timeout * 1000))
        self._contexts[session_id] = context
        self._pages[session_id] = page
        return page

    async def navigate(self, url: str, session_id: str = "default") -> BrowserNavigationResult:
        """Navigates to URL using Playwright when available, or HTTP client fallback."""
        start_t = time.perf_counter()
        
        # Handle special/pseudo URLs
        if url.strip().lower() in ("about:blank", "about:"):
            return BrowserNavigationResult(
                url=url,
                status_code=200,
                title="Blank Page",
                text_content="",
                links=[],
                latency_ms=0.0,
            )

        if not (url.startswith("http://") or url.startswith("https://")):
            url = "https://" + url

        page = await self._get_page(session_id)
        if page:
            try:
                resp = await page.goto(url, wait_until="domcontentloaded")
                elapsed_ms = round((time.perf_counter() - start_t) * 1000.0, 2)
                status_code = resp.status if resp else 200
                title = await page.title()
                
                # Extract text content
                text_content = await page.evaluate("() => document.body ? document.body.innerText : ''")
                
                # Extract links
                links = await page.evaluate("""() => {
                    const anchors = Array.from(document.querySelectorAll('a[href]'));
                    return anchors.slice(0, 20).map(a => ({
                        text: a.innerText.trim().slice(0, 60),
                        href: a.href
                    })).filter(l => l.href && !l.href.startsWith('javascript:'));
                }""")
                
                self._sessions[session_id] = {
                    "current_url": page.url,
                    "title": title,
                    "status_code": status_code,
                    "last_visited": time.time(),
                    "mode": "playwright",
                }

                return BrowserNavigationResult(
                    url=page.url,
                    status_code=status_code,
                    title=title or url,
                    text_content=(text_content or "")[:15000],
                    links=links or [],
                    latency_ms=elapsed_ms,
                )
            except Exception as e:
                logger.warning("Playwright navigation error (%s); falling back to HTTP fetch", e)

        # Fallback HTTP mode
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 HermesAgent/2.0",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        }

        async with httpx.AsyncClient(timeout=self.default_timeout, follow_redirects=True) as client:
            resp = await client.get(url, headers=headers)
            elapsed_ms = round((time.perf_counter() - start_t) * 1000.0, 2)
            html_text = resp.text

            title_match = re.search(r"<title[^>]*>(.*?)</title>", html_text, re.IGNORECASE | re.DOTALL)
            title = title_match.group(1).strip() if title_match else url

            clean = re.sub(r"<(script|style)[^>]*>.*?</\1>", " ", html_text, flags=re.DOTALL | re.IGNORECASE)
            clean = re.sub(r"<!--.*?-->", " ", clean, flags=re.DOTALL)
            clean = re.sub(r"<[^>]+>", " ", clean)
            clean = re.sub(r"\s+", " ", clean).strip()

            links = []
            for m in re.finditer(r'<a\s+(?:[^>]*?\s+)?href="([^"]*)"[^>]*>(.*?)</a>', html_text, re.IGNORECASE):
                href, anchor = m.group(1).strip(), re.sub(r"<[^>]+>", "", m.group(2)).strip()
                if href and anchor and not href.startswith("#") and not href.startswith("javascript:"):
                    links.append({"text": anchor[:60], "href": href})
                if len(links) >= 15:
                    break

            self._sessions[session_id] = {
                "current_url": str(resp.url),
                "title": title,
                "status_code": resp.status_code,
                "last_visited": time.time(),
                "mode": "http_fallback",
            }

            return BrowserNavigationResult(
                url=str(resp.url),
                status_code=resp.status_code,
                title=title,
                text_content=clean[:15000],
                links=links,
                latency_ms=elapsed_ms,
            )

    async def click(self, selector: str, session_id: str = "default") -> Dict[str, Any]:
        """Click on an element matching a CSS or text selector in the active session."""
        page = await self._get_page(session_id)
        if not page:
            return {"status": "error", "message": "Click requires interactive browser engine (Playwright). Not available in HTTP fallback mode."}

        try:
            await page.click(selector, timeout=int(self.default_timeout * 1000))
            await page.wait_for_load_state("domcontentloaded")
            return {
                "status": "success",
                "message": f"Clicked element matching '{selector}'",
                "current_url": page.url,
                "title": await page.title(),
            }
        except Exception as e:
            return {"status": "error", "message": f"Failed to click '{selector}': {e}"}

    async def type(self, selector: str, text: str, session_id: str = "default") -> Dict[str, Any]:
        """Type text into an input or textarea element matching a selector."""
        page = await self._get_page(session_id)
        if not page:
            return {"status": "error", "message": "Type requires interactive browser engine (Playwright)."}

        try:
            await page.fill(selector, text, timeout=int(self.default_timeout * 1000))
            return {"status": "success", "message": f"Typed text into '{selector}'"}
        except Exception as e:
            return {"status": "error", "message": f"Failed to type into '{selector}': {e}"}

    async def screenshot(self, session_id: str = "default", output_path: Optional[str] = None) -> Dict[str, Any]:
        """Capture screenshot of the active page."""
        page = await self._get_page(session_id)
        if not page:
            return {"status": "error", "message": "Screenshot requires interactive browser engine (Playwright)."}

        try:
            dest = output_path or f"/tmp/browser_screenshot_{int(time.time())}.png"
            await page.screenshot(path=dest, full_page=False)
            return {"status": "success", "file_path": dest, "current_url": page.url}
        except Exception as e:
            return {"status": "error", "message": f"Screenshot failed: {e}"}

    async def evaluate_js(self, expression: str, session_id: str = "default") -> Any:
        """Evaluate arbitrary JavaScript expression within page context."""
        page = await self._get_page(session_id)
        if not page:
            return {"status": "error", "message": "JavaScript evaluation requires Playwright."}

        try:
            result = await page.evaluate(expression)
            return {"status": "success", "result": result}
        except Exception as e:
            return {"status": "error", "message": f"JS execution error: {e}"}

    async def extract_selector(self, url: str, selector: str, session_id: str = "default") -> str:
        """Extract text content from elements matching a CSS selector."""
        page = await self._get_page(session_id)
        if page:
            try:
                if page.url != url:
                    await page.goto(url, wait_until="domcontentloaded")
                elements = await page.query_selector_all(selector)
                texts = []
                for el in elements[:20]:
                    t = await el.inner_text()
                    if t and t.strip():
                        texts.append(t.strip())
                if texts:
                    return "\n---\n".join(texts)[:5000]
            except Exception as e:
                logger.warning("Playwright selector extraction failed: %s", e)

        # Fallback using BeautifulSoup if available
        res = await self.navigate(url, session_id=session_id)
        try:
            from bs4 import BeautifulSoup
            async with httpx.AsyncClient(timeout=self.default_timeout) as client:
                r = await client.get(url)
                soup = BeautifulSoup(r.text, "html.parser")
                matches = soup.select(selector)
                if matches:
                    return "\n---\n".join(m.get_text(strip=True) for m in matches[:20])[:5000]
        except Exception:
            pass

        return res.text_content[:5000]

    async def close(self) -> None:
        """Clean up active pages and browser contexts."""
        for p in self._pages.values():
            try:
                await p.close()
            except Exception:
                pass
        for c in self._contexts.values():
            try:
                await c.close()
            except Exception:
                pass
        if self._browser:
            try:
                await self._browser.close()
            except Exception:
                pass
        if self._playwright:
            try:
                await self._playwright.stop()
            except Exception:
                pass
        self._pages.clear()
        self._contexts.clear()
        self._browser = None
        self._playwright = None

    def get_session_info(self, session_id: str = "default") -> Optional[Dict[str, Any]]:
        return self._sessions.get(session_id)


_BROWSER_INSTANCE: Optional[BrowserManager] = None


def get_browser_manager() -> BrowserManager:
    global _BROWSER_INSTANCE
    if _BROWSER_INSTANCE is None:
        _BROWSER_INSTANCE = BrowserManager()
    return _BROWSER_INSTANCE
