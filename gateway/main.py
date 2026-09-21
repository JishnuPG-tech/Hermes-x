import os
import glob
import time
import json
from pathlib import Path
from fastapi import FastAPI, Request
from fastapi.responses import FileResponse, JSONResponse, HTMLResponse, RedirectResponse, Response

from gateway.anthropic_bridge import router as anthropic_router
from gateway.auth_api import router as auth_router
from gateway.v1_sessions import router as v1_sessions_router
from gateway.hermes_proxy import router as hermes_proxy_router
from gateway.omniroute import router as omniroute_router
from gateway.ignis import router as ignis_router
from gateway.claude_rest_api import router as claude_rest_router
from gateway.telemetry import router as telemetry_router
from gateway.webui_api import router as webui_router
from gateway.hermes_dashboard_api import router as dashboard_api_router
from gateway.harness_api import router as harness_router
from gateway.knowledge_api import router as knowledge_router
from gateway.computer_api import router as computer_router
from gateway.voice_api import router as voice_router
from gateway.sessions_api import router as sessions_api_router
from gateway.autonomous_chat import router as autonomous_chat_router



app = FastAPI(
    title="Hermes Agent Space Gateway",
    description="OmniRoute + Ignis + Hermes unified gateway.",
    docs_url=None,
    redoc_url=None,
)

DASHBOARD_ROOT = Path(os.getenv("HERMES_DASHBOARD_ROOT", "/app/web"))
HERMES_WEBUI_PREFIX = "/hermes-webui"


def _hermes_webui_enabled() -> bool:
    return os.getenv("HERMEX_ENABLE_HERMES_WEBUI", "").strip().lower() in {
        "1",
        "true",
        "yes",
        "on",
    }


def _hermes_webui_root() -> Path:
    configured = os.getenv("HERMES_WEBUI_STATIC_ROOT", "").strip()
    candidates = [
        Path(configured) if configured else None,
        Path("/app/hermes-webui-static"),
        Path(__file__).resolve().parents[1] / "third_party" / "hermes-webui" / "static",
    ]
    for candidate in candidates:
        if candidate is not None and candidate.is_dir():
            return candidate
    return candidates[1]


def _hermes_webui_disabled() -> JSONResponse:
    return JSONResponse(
        {
            "error": "Hermes WebUI migration route is disabled",
            "enable_with": "HERMEX_ENABLE_HERMES_WEBUI=true",
        },
        status_code=404,
    )

def _dashboard_index_response(index: Path, request: Request | None) -> HTMLResponse:
    """Inject the dashboard's runtime base/auth settings without rebuilding it."""
    html = index.read_text(encoding="utf-8")
    token = ""
    auth_required = False
    if request is not None:
        from gateway.webui_api import _auth_enabled, _request_token, _valid_session_token

        candidate = _request_token(request)
        if candidate and _valid_session_token(candidate):
            token = candidate
        auth_required = _auth_enabled()
    # The gateway deliberately keeps the API at /api while the SPA is served
    # from /dashboard, so the browser client must use the host root for API
    # and WebSocket URLs.
    base_path = ""
    injected = (
        f'<scr' + f'ipt>window.__HERMES_BASE_PATH__={json.dumps(base_path)};'
        f'window.__HERMES_SESSION_TOKEN__={json.dumps(token)};'
        f'window.__HERMES_AUTH_REQUIRED__={str(auth_required).lower()};</scr' + 'ipt>'
    )
    html = html.replace("</head>", f"{injected}</head>", 1)
    return HTMLResponse(
        html,
        headers={"Cache-Control": "no-store, max-age=0"},
    )

@app.middleware("http")
async def normalize_hermes_paths(request: Request, call_next):
    # 1. Normalize duplicate slashes (e.g. /hermes//api -> /hermes/api)
    path = request.scope.get("path", "")
    import re
    cleaned_path = re.sub(r"/+", "/", path)

    # The upstream nesquena/hermes-webui assets are mounted under a temporary
    # migration prefix, but their browser client still requests the normal
    # root-relative /api and /health contracts. Rewrite only those requests;
    # static assets and SPA paths remain under /hermes-webui/.
    if cleaned_path == HERMES_WEBUI_PREFIX or cleaned_path.startswith(f"{HERMES_WEBUI_PREFIX}/"):
        nested_path = cleaned_path[len(HERMES_WEBUI_PREFIX):] or "/"
        if nested_path == "/health" or nested_path == "/health/":
            cleaned_path = "/health"
        elif nested_path == "/api" or nested_path.startswith("/api/"):
            cleaned_path = nested_path

    # 2. If request starts with /hermes/api, /hermes/bootstrap, /hermes/account, /hermes/organizations
    # strip the leading /hermes prefix so it routes to the Claude REST API router
    # (Do NOT strip /hermes/v1/messages or /hermes/v1/models which belong to anthropic_bridge)
    if cleaned_path.startswith("/hermes/api/") or cleaned_path == "/hermes/api":
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/bootstrap"):
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/account"):
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/organizations"):
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/mobile"):
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/v1/code") or cleaned_path.startswith("/hermes/v1/sessions") or cleaned_path.startswith("/hermes/v1/b"):
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/code"):
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/telemetry") or cleaned_path.startswith("/hermes/live-logs") or cleaned_path.startswith("/hermes/ws"):
        cleaned_path = cleaned_path[len("/hermes"):]
    elif cleaned_path.startswith("/hermes/artifacts"):
        cleaned_path = cleaned_path[len("/hermes"):]

    request.scope["path"] = cleaned_path
    return await call_next(request)

@app.on_event("startup")
async def on_startup():
    try:
        from gateway.background_agent import start_all_saved_jobs
        start_all_saved_jobs()
    except Exception as e:
        print(f"Error starting background agent tasks: {e}")


# ── Root & Health ───────────────────────────────────────────────
@app.api_route("/", methods=["GET", "HEAD"])
async def root():
    upstream_omniroute = os.getenv("UPSTREAM_OMNIROUTE_URL", "https://jishnupg-opencode-cli.hf.space/v1").rstrip("/")
    return JSONResponse({
        "status": "ok",
        "service": "Hermes Agent Space",
        "authority": "Hermes is the King. OmniRoute powers the king.",
        "components": {
            "hermes_agent": "http://127.0.0.1:8642",
            "omniroute": upstream_omniroute,
            "ignis_obsidian": "http://127.0.0.1:8080",
        },
        "endpoints": {
            "anthropic_messages": "/hermes/v1/messages",
            "anthropic_models": "/hermes/v1/models",
            "openai_chat": "/v1/chat/completions",
            "openai_models": "/v1/models",
            "tasks": "/v1/tasks",
            "approvals": "/v1/approvals",
            "harness_health": "/v1/harness/health",
            "dashboard": "/dashboard/",
            "obsidian": "/obsidian",
            "logs": "/logs",
            "health": "/health",
            "persistence": "/debug/persistence",
        },
    })


@app.api_route("/health/live", methods=["GET", "HEAD"])
async def health_live():
    return JSONResponse({"status": "alive"})


@app.api_route("/health", methods=["GET", "HEAD"])
async def health_check():
    services = {}
    import httpx
    client = httpx.AsyncClient(timeout=4.0)
    upstream_omniroute = os.getenv("UPSTREAM_OMNIROUTE_URL", "https://jishnupg-opencode-cli.hf.space/v1").rstrip("/")
    omniroute_health = f"{upstream_omniroute[:-3] if upstream_omniroute.endswith('/v1') else upstream_omniroute}/health"
    checks = {
        "hermes": "http://127.0.0.1:8642/health",
        "omniroute": omniroute_health,
        "ignis": "http://127.0.0.1:8080/obsidian/health",
    }
    for name, url in checks.items():
        try:
            r = await client.get(url)
            services[name] = {"status": "ok", "code": r.status_code}
        except Exception as e:
            services[name] = {"status": "starting", "message": str(e)}
    await client.aclose()
    return JSONResponse({"status": "ok", "gateway": "healthy", "upstreams": services})


@app.get("/debug/persistence")
async def debug_persistence():
    data_dir = Path("/data")
    memory_db = data_dir / "hermes" / "memory.sqlite"
    conv_file = data_dir / "conversations" / "history.json"
    vault_notes = list((data_dir / "obsidian" / "vault").glob("**/*.md")) if (data_dir / "obsidian" / "vault").exists() else []

    return JSONResponse({
        "status": "ok",
        "memory_db_exists": memory_db.exists(),
        "memory_db_size": memory_db.stat().st_size if memory_db.exists() else 0,
        "conversations_exists": conv_file.exists(),
        "conversations_size": conv_file.stat().st_size if conv_file.exists() else 0,
        "obsidian_notes_count": len(vault_notes),
        "cloud_vault_repo": os.getenv("HERMES_VAULT_REPO", "Jishnupg/hermes-storage-vault"),
        "hf_token_set": bool(os.getenv("HF_TOKEN") or os.getenv("HUGGINGFACE_API_KEY")),
    })


@app.post("/debug/vault/backup")
@app.get("/debug/vault/backup")
async def trigger_vault_backup():
    try:
        from vault_sync import backup_to_vault
        ok, msg = backup_to_vault()
        return JSONResponse({"status": "ok" if ok else "error", "message": msg})
    except Exception as e:
        return JSONResponse({"status": "error", "message": str(e)}, status_code=500)


# ── PWA Manifest & Static Assets ───────────────────────────────
@app.api_route("/manifest.json", methods=["GET", "HEAD"])
@app.api_route("/manifest.webmanifest", methods=["GET", "HEAD"])
@app.api_route("/site.webmanifest", methods=["GET", "HEAD"])
async def webmanifest():
    return JSONResponse({
        "name": "Hermes Agent Space",
        "short_name": "Hermes",
        "start_url": "/",
        "display": "standalone",
        "background_color": "#0f172a",
        "theme_color": "#0f172a",
        "icons": [{"src": "/static/favicon.png", "sizes": "192x192", "type": "image/png"}],
    })


@app.api_route("/dashboard", methods=["GET", "HEAD"])
@app.api_route("/dashboard/", methods=["GET", "HEAD"])
async def official_dashboard(request: Request):
    index = DASHBOARD_ROOT / "index.html"
    if not index.exists():
        return JSONResponse(
            {"error": "Official Hermes dashboard assets are not installed"},
            status_code=503,
        )
    return _dashboard_index_response(index, request)


@app.api_route("/dashboard/{asset_path:path}", methods=["GET", "HEAD"])
async def official_dashboard_asset(request: Request, asset_path: str):
    """Serve official Hermes assets and fall back only for SPA deep links."""
    candidate = (DASHBOARD_ROOT / asset_path).resolve()
    root = DASHBOARD_ROOT.resolve()
    if candidate.is_file() and (candidate == root or root in candidate.parents):
        return FileResponse(candidate)
    index = DASHBOARD_ROOT / "index.html"
    if index.exists():
        return _dashboard_index_response(index, request)
    return JSONResponse(
        {"error": "Official Hermes dashboard assets are not installed"},
        status_code=503,
    )


@app.api_route("/hermes-webui", methods=["GET", "HEAD"])
async def hermes_webui_root():
    if not _hermes_webui_enabled():
        return _hermes_webui_disabled()
    return RedirectResponse(f"{HERMES_WEBUI_PREFIX}/", status_code=307)


@app.api_route("/hermes-webui/", methods=["GET", "HEAD"])
async def hermes_webui_index():
    if not _hermes_webui_enabled():
        return _hermes_webui_disabled()
    index = _hermes_webui_root() / "index.html"
    if not index.is_file():
        return JSONResponse(
            {"error": "Upstream Hermes WebUI assets are not installed"},
            status_code=503,
        )
    html = index.read_text(encoding="utf-8").replace(
        "__WEBUI_VERSION__", os.getenv("HERMES_WEBUI_VERSION", "adapter-1")
    )
    return HTMLResponse(
        html,
        headers={"Cache-Control": "no-store, max-age=0"},
    )


@app.api_route("/hermes-webui/login", methods=["GET", "HEAD"])
async def hermes_webui_login_page():
    if not _hermes_webui_enabled():
        return _hermes_webui_disabled()
    if not os.getenv("HERMES_WEBUI_PASSWORD", "").strip():
        return RedirectResponse(f"{HERMES_WEBUI_PREFIX}/", status_code=303)
    script_open = "<scr" + "ipt>"
    script_close = "</scr" + "ipt>"
    return HTMLResponse(
        """<!doctype html>
<html><head><meta charset="utf-8"><title>Hermes WebUI login</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{font:16px system-ui;background:#0d1117;color:#f0f6fc;display:grid;place-items:center;min-height:100vh}
form{display:grid;gap:14px;width:min(360px,90vw);padding:28px;border:1px solid #30363d;border-radius:12px}
input,button{font:inherit;padding:10px;border-radius:8px;border:1px solid #484f58}button{cursor:pointer}</style>
</head><body><form id="login">
<h1>Hermes WebUI</h1><label>Password<input name="password" type="password" autofocus required></label>
<button type="submit">Sign in</button><p id="error" role="alert"></p></form>
__SCRIPT_OPEN__document.querySelector("#login").addEventListener("submit",async(e)=>{e.preventDefault();
const password=new FormData(e.currentTarget).get("password");const r=await fetch("/api/auth/login",
{method:"POST",headers:{"content-type":"application/json"},credentials:"include",body:JSON.stringify({password})});
if(r.ok) location.assign("/hermes-webui/"); else document.querySelector("#error").textContent="Invalid password";});__SCRIPT_CLOSE__
</body></html>""".replace("__SCRIPT_OPEN__", script_open).replace("__SCRIPT_CLOSE__", script_close),
        headers={"Cache-Control": "no-store, max-age=0"},
    )


@app.api_route("/hermes-webui/{asset_path:path}", methods=["GET", "HEAD"])
async def hermes_webui_asset(asset_path: str):
    if not _hermes_webui_enabled():
        return _hermes_webui_disabled()
    root = _hermes_webui_root().resolve()
    candidate = (root / asset_path).resolve()
    # The upstream index is served from its static/ directory, so its
    # relative references remain static/style.css, static/boot.js, etc.
    # Docker packages that directory's contents directly at the configured
    # root. Resolve both layouts without weakening the path traversal guard.
    if not candidate.is_file() and asset_path.startswith("static/"):
        candidate = (root / asset_path[len("static/"):]).resolve()
    if candidate.is_file() and (candidate == root or root in candidate.parents):
        return FileResponse(candidate)
    index = root / "index.html"
    if index.is_file():
        html = index.read_text(encoding="utf-8").replace(
            "__WEBUI_VERSION__", os.getenv("HERMES_WEBUI_VERSION", "adapter-1")
        )
        return HTMLResponse(
            html,
            headers={"Cache-Control": "no-store, max-age=0"},
        )
    return JSONResponse(
        {"error": "Upstream Hermes WebUI assets are not installed"},
        status_code=503,
    )


@app.api_route("/login", methods=["GET", "HEAD"])
async def dashboard_login_page():
    """Small server-side login bridge used by the official dashboard auth flow."""
    if not os.getenv("HERMES_WEBUI_PASSWORD", "").strip():
        return RedirectResponse("/dashboard/", status_code=303)
    script_open = "<scr" + "ipt>"
    script_close = "</scr" + "ipt>"
    return HTMLResponse(
        """<!doctype html>
<html><head><meta charset="utf-8"><title>Hermes login</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{font:16px system-ui;background:#0d1117;color:#f0f6fc;display:grid;place-items:center;min-height:100vh}
form{display:grid;gap:14px;width:min(360px,90vw);padding:28px;border:1px solid #30363d;border-radius:12px}
input,button{font:inherit;padding:10px;border-radius:8px;border:1px solid #484f58}button{cursor:pointer}</style>
</head><body><form id="login">
<h1>Hermes dashboard</h1><label>Password<input name="password" type="password" autofocus required></label>
<button type="submit">Sign in</button><p id="error"></p></form>
__SCRIPT_OPEN__document.querySelector("#login").addEventListener("submit",async(e)=>{e.preventDefault();
const password=new FormData(e.currentTarget).get("password");const r=await fetch("/api/auth/login",
{method:"POST",headers:{"content-type":"application/json"},credentials:"include",body:JSON.stringify({password})});
if(r.ok) location.assign("/dashboard/"); else document.querySelector("#error").textContent="Invalid password";});__SCRIPT_CLOSE__
</body></html>""".replace("__SCRIPT_OPEN__", script_open).replace("__SCRIPT_CLOSE__", script_close),
        headers={"Cache-Control": "no-store, max-age=0"},
    )


@app.api_route("/favicon.ico", methods=["GET", "HEAD"])
@app.api_route("/favicon.png", methods=["GET", "HEAD"])
@app.api_route("/static/favicon.png", methods=["GET", "HEAD"])
@app.api_route("/static/favicon.ico", methods=["GET", "HEAD"])
async def favicon():
    return Response(content=b"", status_code=204)


# ── Live Runtime Log Inspector ──────────────────────────────────
LOG_FILES = {
    "gateway": "/data/cache/gateway.log",
    "hermes": "/data/cache/hermes.log",
    "omniroute": "/data/cache/omniroute.log",
    "ignis": "/data/cache/ignis.log",
}


@app.api_route("/logs", methods=["GET", "HEAD"])
async def logs_viewer():
    html = """<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>Live Logs</title>
<meta http-equiv="refresh" content="5">
<style>
body{font-family:monospace;background:#0d1117;color:#c9d1d9;margin:0;padding:16px}
h1{color:#58a6ff;font-size:1.2rem;margin:0 0 12px}
.log{background:#161b22;border:1px solid #30363d;border-radius:8px;padding:12px;margin:8px 0;max-height:400px;overflow-y:auto;white-space:pre-wrap;font-size:0.8rem;line-height:1.4}
.label{color:#8b949e;font-size:0.75rem;margin-bottom:4px}
.ok{color:#3fb950}.warn{color:#d29922}.err{color:#f85149}
</style></head><body>
<h1>Live Container Logs (auto-refresh 5s)</h1>
"""
    for name, path in LOG_FILES.items():
        content = ""
        status = "ok"
        try:
            if os.path.exists(path):
                with open(path, "r", encoding="utf-8", errors="replace") as f:
                    lines = f.readlines()
                    import html as html_lib; content = html_lib.escape("".join(lines[-80:]))
            else:
                content = "(log file not yet created)"
                status = "warn"
        except Exception as e:
            content = f"(error reading: {e})"
            status = "err"
        html += f'<div class="label">{name.upper()} — {path} <span class="{status}">[{status}]</span></div>'
        html += f'<div class="log">{content}</div>\n'
    html += "</body></html>"
    return HTMLResponse(content=html)


@app.api_route("/logs/{service}", methods=["GET", "HEAD"])
async def logs_service(service: str):
    path = LOG_FILES.get(service)
    if not path:
        return JSONResponse({"error": f"Unknown service: {service}. Valid: {list(LOG_FILES.keys())}"}, status_code=404)
    try:
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8", errors="replace") as f:
                lines = f.readlines()
                content = "".join(lines[-200:])
        else:
            content = "(log file not yet created)"
    except Exception as e:
        content = f"(error: {e})"
    return HTMLResponse(f"<pre style='font-family:monospace;background:#0d1117;color:#c9d1d9;padding:16px'>{content}</pre>")


# Register routers after the gateway's exact health/static/log routes so the
# legacy /health catch-all cannot shadow them. Keep WebUI before the legacy
# /api/models aliases and Hermes proxy last.
app.include_router(autonomous_chat_router)
app.include_router(sessions_api_router)
app.include_router(telemetry_router)
app.include_router(anthropic_router)
app.include_router(v1_sessions_router)
app.include_router(harness_router)
app.include_router(knowledge_router)
app.include_router(computer_router)
app.include_router(voice_router)
app.include_router(auth_router)
app.include_router(webui_router)
app.include_router(dashboard_api_router)
app.include_router(claude_rest_router)
app.include_router(hermes_proxy_router)
app.include_router(omniroute_router)
app.include_router(ignis_router)

@app.on_event("startup")
async def on_startup():
    try:
        from harness.voice import get_voice_gateway
        vg = get_voice_gateway()
        await vg.initialize()
    except Exception as e:
        print(f"Error initializing voice gateway: {e}")
    try:
        from gateway import channels_manager
        await channels_manager.start_all_channels()
    except Exception as e:
        print(f"Error starting channels manager: {e}")

    try:
        from gateway.harness_api import get_harness_engine
        from harness.watchdog import WatchdogDaemon
        import asyncio
        engine = get_harness_engine()
        watchdog = WatchdogDaemon(engine)
        asyncio.create_task(watchdog.start())
    except Exception as e:
        print(f"Error starting harness watchdog: {e}")

@app.on_event("shutdown")
async def on_shutdown():
    try:
        from gateway import channels_manager
        await channels_manager.stop_all_channels()
    except Exception as e:
        pass

