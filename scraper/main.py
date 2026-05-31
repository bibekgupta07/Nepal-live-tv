"""Entry point used by .github/workflows/scrape.yml.

Runs both scrapers in parallel and writes their output to data/*.json. The
Action commits the resulting diff back to main; the Android app reads the JSON
straight from raw.githubusercontent.com.

Run locally:
    pip install -r scraper/requirements.txt
    python -m scraper.main
"""

from __future__ import annotations

import asyncio
import json
import os
import sys
from pathlib import Path

from scraper.lookmovie_home import scrape_home
from scraper.techjail_channels import scrape_channels

REPO_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = REPO_ROOT / "data"


async def run() -> int:
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    lookmovie_base = os.environ.get("LOOKMOVIE_BASE_URL", "https://lookmovie2.to")
    techjail_meta = os.environ.get(
        "TECHJAIL_METADATA_URL", "http://tv.techjail.net/huritv9/channels.php"
    )
    techjail_stream = os.environ.get(
        "TECHJAIL_STREAM_API_URL",
        "http://tv.techjail.net/huritv9/getlink.php?vv=1&CHID=",
    )
    logo_base = os.environ.get("TECHJAIL_LOGO_BASE_URL", "https://nettv1.nettv.com.np")

    home_task = asyncio.create_task(scrape_home(base_url=lookmovie_base))
    channels_task = asyncio.create_task(
        scrape_channels(
            metadata_url=techjail_meta,
            stream_api_url=techjail_stream,
            logo_base_url=logo_base,
        )
    )

    home_feed, channels = await asyncio.gather(home_task, channels_task)

    home_path = DATA_DIR / "movies-home.json"
    channels_path = DATA_DIR / "channels.json"

    home_path.write_text(json.dumps(home_feed, ensure_ascii=False, indent=2) + "\n")
    channels_path.write_text(json.dumps(channels, ensure_ascii=False, indent=2) + "\n")

    rows = len(home_feed.get("rows", []))
    hero = len(home_feed.get("hero", []))
    print(f"OK movies-home.json: hero={hero}, rows={rows}")
    print(f"OK channels.json: count={len(channels)}")

    if not channels and not rows:
        print("ERR both scrapers returned empty — refusing to commit")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(run()))
