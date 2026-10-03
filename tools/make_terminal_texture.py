"""Generates the terminal panel texture used by both terminal blocks."""

import os

from PIL import Image, ImageDraw

SIZE = 32
BEZEL = 2

OUT = os.path.join("src", "main", "resources", "assets", "rbmkterminals", "textures", "blocks")

image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
draw = ImageDraw.Draw(image)

# brushed metal bezel, lighter towards the outside so the plate still reads as a solid object
for i in range(BEZEL):
    shade = 96 - i * 26
    draw.rectangle([i, i, SIZE - 1 - i, SIZE - 1 - i], outline=(shade, shade, shade + 4, 255))

# screen, a very dark green grey with a slight vertical gradient
for y in range(BEZEL, SIZE - BEZEL):
    t = (y - BEZEL) / float(SIZE - 2 * BEZEL - 1)
    base = int(8 + 10 * (1.0 - t))
    draw.line([(BEZEL, y), (SIZE - 1 - BEZEL, y)], fill=(base, base + 6, base, 255))

# dark inner edge of the screen cutout
draw.rectangle([BEZEL, BEZEL, SIZE - 1 - BEZEL, SIZE - 1 - BEZEL], outline=(24, 30, 24, 255))

# four screws in the corners of the bezel
for cx, cy in ((0, 0), (SIZE - 1, 0), (0, SIZE - 1), (SIZE - 1, SIZE - 1)):
    draw.point((cx, cy), fill=(168, 168, 172, 255))

os.makedirs(OUT, exist_ok=True)
path = os.path.join(OUT, "terminal_panel.png")
image.save(path)
print("wrote", path, image.size)
