"""Scrapes the lookmovie2.to home/grid pages into the JSON shape the Android
app's HomeFeedDto expects. Run from a GitHub Actions cron; the output replaces
the backend's /api/movies/home endpoint.

This is a faithful port of the FastAPI backend's LookmovieMediaSource._scrape_home,
minus the asyncio.Lock / TTL machinery (cron runs cold each time).
"""

from __future__ import annotations

import asyncio
import re
from typing import Dict, List, Optional, Tuple

import httpx
from bs4 import BeautifulSoup, Tag

_UA = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/124.0.0.0 Safari/537.36"
)

_HOME_GENRE_ROWS: List[Tuple[str, str, str]] = [
    ("Action",  "movie", "action"),
    ("Comedy",  "movie", "comedy"),
    ("Horror",  "movie", "horror"),
    ("Sci-Fi",  "movie", "science-fiction"),
    ("Drama",   "show",  "drama"),
    ("Crime",   "show",  "crime"),
]

_TITLE_YEAR = re.compile(r"^(.*?)\s*\((\d{4})\)\s*$")
_VIEW_HREF_RE = re.compile(r"^/(movies|shows)/view/([^?]+)")
_PATH_KIND = {"movie": "movies", "show": "shows"}


def _absolute(base: str, url: Optional[str]) -> str:
    if not url:
        return ""
    if url.startswith(("http://", "https://")):
        return url
    if url.startswith("//"):
        return "https:" + url
    if url.startswith("/"):
        return base + url
    return base + "/" + url


def _parse_title_year(raw: str) -> Tuple[str, Optional[int]]:
    raw = raw.strip()
    m = _TITLE_YEAR.match(raw)
    if not m:
        return raw, None
    return m.group(1).strip(), int(m.group(2))


def _card_for(anchor: Tag) -> Tag:
    for parent in anchor.parents:
        classes = parent.get("class") or []
        if any(c.startswith(("movie-item", "slide-item")) for c in classes):
            return parent
    node = anchor
    for _ in range(3):
        if node.parent is None:
            break
        node = node.parent
    return node


def _build_item(base: str, anchor: Tag, slug: str, kind: str) -> dict:
    card = _card_for(anchor)

    title_raw: str = ""
    for sel in (".slide-item__title", "h1", "h2", "h3", "h4", "h5", "h6"):
        el = card.select_one(sel)
        if el and el.get_text(strip=True):
            title_raw = el.get_text(strip=True)
            break
    if not title_raw:
        title_raw = (anchor.get("title") or "").strip()
    if not title_raw:
        img = card.find("img")
        if img:
            title_raw = (img.get("alt") or "").strip()
    if not title_raw:
        title_raw = re.sub(r"^\d+-", "", slug).replace("-", " ").title()

    title, year = _parse_title_year(title_raw)

    if year is None:
        year_el = card.select_one(".year")
        if year_el:
            m = re.search(r"\d{4}", year_el.get_text())
            if m:
                year = int(m.group(0))

    img = card.find("img")
    backdrop = ""
    poster = ""
    if img:
        backdrop = (
            img.get("data-src-landscape")
            or img.get("data-src")
            or img.get("src")
            or ""
        )
        poster = img.get("data-src-portrait") or backdrop
        if poster.startswith("data:"):
            poster = backdrop = ""

    return {
        "id": slug,
        "kind": kind,
        "title": title,
        "year": year,
        "poster": _absolute(base, poster),
        "backdrop": _absolute(base, backdrop),
    }


def _parse_grid(base: str, html: str, kind_filter: Optional[str] = None) -> List[dict]:
    soup = BeautifulSoup(html, "html.parser")
    items: List[dict] = []
    seen: set[str] = set()
    for a in soup.select('a[href*="/movies/view/"], a[href*="/shows/view/"]'):
        href = a.get("href", "")
        m = _VIEW_HREF_RE.match(href)
        if not m:
            continue
        kind_path, slug_with_q = m.group(1), m.group(2)
        slug = slug_with_q.split("?", 1)[0]
        kind = "movie" if kind_path == "movies" else "show"
        if kind_filter and kind != kind_filter:
            continue
        if slug in seen:
            continue
        seen.add(slug)
        items.append(_build_item(base, a, slug, kind))
    return items


async def scrape_home(base_url: str = "https://lookmovie2.to", timeout: float = 30.0) -> dict:
    """Returns a HomeFeedDto-shaped dict: {hero: [...], rows: [{title, items: [...]}]}."""
    base = base_url.rstrip("/")
    async with httpx.AsyncClient(
        base_url=base,
        timeout=timeout,
        follow_redirects=True,
        headers={
            "User-Agent": _UA,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9",
        },
    ) as client:
        tasks: List[Tuple[str, asyncio.Future]] = [
            ("__home__", client.get("/")),
            ("__movies__", client.get("/movies")),
            ("__shows__", client.get("/shows")),
        ]
        for label, kind, slug in _HOME_GENRE_ROWS:
            path = f"/{_PATH_KIND[kind]}/genre/{slug}"
            tasks.append((label, client.get(path)))

        results = await asyncio.gather(*[t[1] for t in tasks], return_exceptions=True)

    pages: Dict[str, str] = {}
    for (label, _), r in zip(tasks, results):
        if isinstance(r, Exception):
            print(f"WARN lookmovie home fetch failed for {label}: {r}")
            continue
        if r.status_code != 200:
            print(f"WARN lookmovie {label} returned {r.status_code}")
            continue
        pages[label] = r.text

    home_items = _parse_grid(base, pages.get("__home__", ""))
    hero = home_items[:8]
    trending = home_items[8:28]

    rows: List[dict] = []
    if trending:
        rows.append({"title": "Trending Now", "items": trending})

    new_movies = _parse_grid(base, pages.get("__movies__", ""), kind_filter="movie")[:20]
    if new_movies:
        rows.append({"title": "New Movies", "items": new_movies})

    new_shows = _parse_grid(base, pages.get("__shows__", ""), kind_filter="show")[:20]
    if new_shows:
        rows.append({"title": "New TV Shows", "items": new_shows})

    for label, kind, _slug in _HOME_GENRE_ROWS:
        items = _parse_grid(base, pages.get(label, ""), kind_filter=kind)[:20]
        if items:
            rows.append({"title": label, "items": items})

    return {"hero": hero, "rows": rows}
