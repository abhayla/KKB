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


def strip_adb_warnings(path):
    """Strip ADB warning text prepended to PNG data.

    When multiple displays exist, `adb exec-out screencap -p` prepends
    warning text like '[Warning] Multiple displays were found...' before
    the actual PNG binary. This corrupts the file. Fix by finding the
    PNG magic bytes (\\x89PNG) and stripping everything before them.
    """
    try:
        with open(path, "rb") as f:
            data = f.read(512)  # Read header to check
        png_magic = b"\x89PNG"
        if data.startswith(png_magic):
            return False  # Already valid PNG
        idx = data.find(png_magic)
        if idx <= 0:
            return False  # Not a PNG at all, skip
        # Re-read full file and strip prefix
        with open(path, "rb") as f:
            full_data = f.read()
        idx = full_data.find(png_magic)
        with open(path, "wb") as f:
            f.write(full_data[idx:])
        return True
    except Exception:
        return False


def resize_if_needed(path):
    """Resize image at path if either dimension exceeds MAX_DIM.

    Also repairs files corrupted by ADB warning text prepended to PNG data.
    """
    # First, fix ADB warning corruption if present
    strip_adb_warnings(path)

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
