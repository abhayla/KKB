#!/usr/bin/env python3
"""
Auto-resize screenshots to prevent Claude API 400 errors.

The Claude API rejects images exceeding 2000px in either dimension during
multi-image conversations. This script resizes images to stay within a safe
1800px limit (buffer below the 2000px hard cap).

Usage:
    python .claude/hooks/resize_screenshot.py <file_path>    # Resize single file
    python .claude/hooks/resize_screenshot.py --all          # Resize all in screenshots dir
    python .claude/hooks/resize_screenshot.py --recent       # Resize files modified in last 10s
"""

import os
import sys
import time

MAX_DIM = 1800
SCREENSHOTS_DIR = "docs/testing/screenshots"


def resize_if_needed(path):
    """Resize image at path if either dimension exceeds MAX_DIM."""
    try:
        from PIL import Image

        img = Image.open(path)
        w, h = img.size
        if w <= MAX_DIM and h <= MAX_DIM:
            return False
        scale = min(MAX_DIM / w, MAX_DIM / h)
        new_size = (int(w * scale), int(h * scale))
        img = img.resize(new_size, Image.LANCZOS)
        img.save(path)
        return True
    except Exception:
        return False


def process_directory(recent_only=False):
    """Resize all (or recently modified) screenshots in the directory."""
    if not os.path.isdir(SCREENSHOTS_DIR):
        return
    now = time.time()
    for fname in os.listdir(SCREENSHOTS_DIR):
        fpath = os.path.join(SCREENSHOTS_DIR, fname)
        if not os.path.isfile(fpath):
            continue
        if not fname.lower().endswith((".png", ".jpg", ".jpeg")):
            continue
        if recent_only and (now - os.path.getmtime(fpath)) > 10:
            continue
        resize_if_needed(fpath)


def main():
    if len(sys.argv) < 2:
        print("Usage: resize_screenshot.py <file_path> | --all | --recent")
        sys.exit(1)

    arg = sys.argv[1]

    if arg == "--all":
        process_directory(recent_only=False)
    elif arg == "--recent":
        process_directory(recent_only=True)
    else:
        if os.path.isfile(arg):
            resize_if_needed(arg)


if __name__ == "__main__":
    main()
