#!/usr/bin/env python3
"""
Normalize Pokemon sprite images so that every file has uniform padding.

Algorithm:
  1. Crop to the non-transparent content bounding box
  2. Compute padding = round(max(content_w, content_h) * (1 - fill) / 2)
  3. Add that same padding on all 4 sides → canvas is content + 2*padding each axis

No resampling, no forced square, no power-of-2. Because ContentScale.Fit scales
an image so its largest dimension fills the dp box, uniform absolute padding
guarantees every Pokemon's content occupies `fill` of the box on its dominant
axis — regardless of whether the sprite is portrait, landscape, or square.

Usage:
  python3 normalize_sprites.py [--dry-run] [--fill 0.90] [folder]

Options:
  --dry-run     Print what would change without writing any files.
  --fill FLOAT  Fraction of the dominant axis the content should occupy.
                Default: 0.90
  folder        Path to the sprite directory.
                Default: composeApp/src/wasmJsMain/resources/images/pokemons/sprite
"""

import argparse
import math
from pathlib import Path

import numpy as np
from PIL import Image


def visual_bbox(img: Image.Image, alpha_threshold: int = 30, min_pixels: int = 3) -> tuple | None:
    """
    Returns (left, top, right, bottom) of the visual content bounding box.
    A row or column only counts as content if it contains at least `min_pixels`
    pixels with alpha >= `alpha_threshold`. This avoids sparse edge features
    (single wing tips, thin tails) pulling the bbox too far out.
    """
    alpha = np.array(img)[:, :, 3]
    mask = alpha >= alpha_threshold  # True where pixel is opaque enough

    row_counts = mask.sum(axis=1)   # opaque pixels per row
    col_counts = mask.sum(axis=0)   # opaque pixels per column

    rows = np.where(row_counts >= min_pixels)[0]
    cols = np.where(col_counts >= min_pixels)[0]

    if rows.size == 0 or cols.size == 0:
        return None

    return int(cols[0]), int(rows[0]), int(cols[-1]) + 1, int(rows[-1]) + 1


def normalize_sprite(path: Path, dry_run: bool = False, fill: float = 0.90) -> dict | None:
    img = Image.open(path).convert("RGBA")
    orig_w, orig_h = img.size
    bbox = visual_bbox(img)

    if bbox is None:
        return None  # fully transparent – skip

    left, top, right, bottom = bbox
    cw, ch = right - left, bottom - top

    padding = round(max(cw, ch) * (1 - fill) / 2)
    new_w = cw + 2 * padding
    new_h = ch + 2 * padding

    # Already normalized?
    if orig_w == new_w and orig_h == new_h and left == padding and top == padding:
        return None

    info = {
        "name": path.name,
        "orig": (orig_w, orig_h),
        "content": (cw, ch),
        "padding": padding,
        "new_size": (new_w, new_h),
    }

    if not dry_run:
        content = img.crop(bbox)
        out = Image.new("RGBA", (new_w, new_h), (0, 0, 0, 0))
        out.paste(content, (padding, padding))
        out.save(path, "PNG", optimize=True)

    return info


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Normalize Pokemon sprite padding",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "folder",
        nargs="?",
        default="composeApp/src/wasmJsMain/resources/images/pokemons/sprite",
        help="Path to sprite folder (default: %(default)s)",
    )
    parser.add_argument("--dry-run", action="store_true", help="Preview without writing files")
    parser.add_argument(
        "--fill",
        type=float,
        default=0.90,
        metavar="FLOAT",
        help="Content fill ratio on dominant axis (default: 0.90)",
    )
    args = parser.parse_args()

    folder = Path(args.folder)
    if not folder.is_dir():
        print(f"Error: '{folder}' is not a directory.")
        return 1

    files = sorted(p for p in folder.iterdir() if p.suffix.lower() == ".png")
    if not files:
        print(f"No PNG files found in '{folder}'.")
        return 0

    changed, skipped = [], 0
    for path in files:
        result = normalize_sprite(path, dry_run=args.dry_run, fill=args.fill)
        if result:
            changed.append(result)
        else:
            skipped += 1

    prefix = "[DRY RUN] " if args.dry_run else ""
    for info in changed:
        ow, oh = info["orig"]
        nw, nh = info["new_size"]
        cw, ch = info["content"]
        p = info["padding"]
        print(f"{prefix}{info['name']}: {ow}x{oh} → {nw}x{nh}  (content {cw}x{ch}, padding {p}px)")

    verb = "would be updated" if args.dry_run else "updated"
    print(f"\n{prefix}{len(changed)} image(s) {verb}, {skipped} already normalized.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
