"""Render the Play Store icon (512x512) and feature graphic (1024x500).

Same look as the launcher icon (orange "V" on navy). Needs Pillow:
    python3 tools/store_graphics.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

NAVY = (11, 27, 51)
ORANGE = (255, 122, 26)
WHITE = (255, 255, 255)
GRID = (24, 44, 76)
OUT = Path(__file__).resolve().parent.parent / "store" / "graphics"

# The "V" from ic_launcher_foreground.xml, in its 108x108 viewport.
V = [(36, 36), (46, 36), (54, 62), (62, 36), (72, 36), (59, 74), (49, 74)]


def v_polygon(cx, cy, size):
    s = size / 38  # the V is 36 wide x 38 tall
    return [(cx + (x - 54) * s, cy + (y - 55) * s) for x, y in V]


def font(size, bold=True):
    names = ["DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"]
    for n in names:
        for d in ["/usr/share/fonts/truetype/dejavu", "/usr/share/fonts/dejavu"]:
            p = Path(d) / n
            if p.exists():
                return ImageFont.truetype(str(p), size)
    return ImageFont.load_default(size)


def grid(draw, w, h, step):
    for x in range(0, w, step):
        draw.line([(x, 0), (x, h)], fill=GRID, width=1)
    for y in range(0, h, step):
        draw.line([(0, y), (w, y)], fill=GRID, width=1)


def icon():
    img = Image.new("RGB", (512, 512), NAVY)
    d = ImageDraw.Draw(img)
    d.polygon(v_polygon(256, 256, 300), fill=ORANGE)
    img.save(OUT / "icon-512.png")


def feature():
    w, h = 1024, 500
    img = Image.new("RGB", (w, h), NAVY)
    d = ImageDraw.Draw(img)
    grid(d, w, h, 32)
    d.polygon(v_polygon(200, 250, 220), fill=ORANGE)
    d.text((370, 150), "// PORTFOLIO + DEMO", font=font(26, bold=False), fill=ORANGE)
    d.text((370, 190), "Vani Santos", font=font(64), fill=WHITE)
    d.text((370, 275), "Angular · Micro Frontends", font=font(30, bold=False), fill=WHITE)
    d.text((370, 315), "Java · Kubernetes · AWS · AI", font=font(30, bold=False), fill=WHITE)
    img.save(OUT / "feature-graphic-1024x500.png")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    feature()
    print("written to", OUT)
