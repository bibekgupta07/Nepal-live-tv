"""Scrapes the techjail upstream into a list of provably-live channels.

Two-stage liveness check: getlink must return an http(s) m3u8 URL, and that
URL must serve a valid HLS playlist (#EXTM3U + at least one variant/segment
marker). This catches expired tokens and "File not found"-with-200 responses.

Output is the JSON shape the Android app's ChannelDto expects:
    [{name, encodedUrl, logo, category}]
"""

from __future__ import annotations

import asyncio
from typing import List, Optional

import httpx

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"


def _is_valid_m3u8_body(body: str) -> bool:
    if not body:
        return False
    stripped = body.lstrip()
    if not stripped.startswith("#EXTM3U"):
        return False
    return (
        "#EXT-X-STREAM-INF" in stripped
        or "#EXTINF" in stripped
        or "#EXT-X-ENDLIST" in stripped
    )


def _normalize_logo(logo: str, logo_base_url: str) -> str:
    if logo and logo.startswith("/"):
        return logo_base_url + logo
    return logo or ""


async def _probe(
    client: httpx.AsyncClient,
    sem: asyncio.Semaphore,
    stream_api_url: str,
    channel_id: str,
    probe_timeout: float,
) -> Optional[str]:
    async with sem:
        try:
            resp = await client.get(
                stream_api_url + str(channel_id), timeout=probe_timeout
            )
        except Exception:
            return None

        stream_url = resp.text.strip() if resp.status_code == 200 else ""
        if not stream_url.startswith("http") or ".m3u8" not in stream_url.lower():
            return None

        try:
            playlist = await client.get(stream_url, timeout=probe_timeout)
        except Exception:
            return None

        if playlist.status_code != 200:
            return None

        return channel_id if _is_valid_m3u8_body(playlist.text) else None


async def scrape_channels(
    metadata_url: str = "http://tv.techjail.net/huritv9/channels.php",
    stream_api_url: str = "http://tv.techjail.net/huritv9/getlink.php?vv=1&CHID=",
    logo_base_url: str = "https://nettv1.nettv.com.np",
    probe_concurrency: int = 16,
    probe_timeout: float = 6.0,
) -> List[dict]:
    """Returns a list of ChannelDto-shaped dicts."""
    async with httpx.AsyncClient(
        headers={"User-Agent": USER_AGENT}, follow_redirects=True
    ) as client:
        try:
            meta_resp = await client.get(metadata_url, timeout=10.0)
            meta_resp.raise_for_status()
            metadata = meta_resp.json()
        except Exception as e:
            print(f"ERR fetching channels metadata: {e}")
            return []

        sem = asyncio.Semaphore(probe_concurrency)
        tasks = [
            _probe(client, sem, stream_api_url, ch["channel_id"], probe_timeout)
            for ch in metadata
            if ch.get("channel_id")
        ]
        results = await asyncio.gather(*tasks)
        live_ids = {cid for cid in results if cid}

    return [
        {
            "name": (ch.get("channel_name") or "").strip(),
            "encodedUrl": ch.get("channel_id", ""),
            "logo": _normalize_logo(ch.get("channel_logo", ""), logo_base_url),
            "category": ch.get("category_title", "All"),
        }
        for ch in metadata
        if ch.get("channel_id") in live_ids
    ]
