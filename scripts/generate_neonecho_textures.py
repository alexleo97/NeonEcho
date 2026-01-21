#!/usr/bin/env python3
import math
import os
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

BLOCK_TEX_DIR = ROOT / "src/main/resources/Common/BlockTextures/NeonEcho"
BLOCK_MODEL_DIR = ROOT / "src/main/resources/Common/Blocks/NeonEcho"
ICON_DIR = ROOT / "src/main/resources/Common/Icons/ItemsGenerated"
INGOT_DIR = ROOT / "src/main/resources/Common/Resources/Materials/Ingot_Textures"

NEON_CYAN = (30, 245, 255, 255)
NEON_MAGENTA = (255, 63, 179, 255)
NEON_PURPLE = (177, 75, 255, 255)
NEON_BLUE = (40, 140, 255, 255)
DARK = (10, 12, 18, 255)
MID = (22, 26, 38, 255)
LIGHT = (130, 140, 160, 255)
CHROME = (180, 186, 198, 255)


def clamp(value, low=0, high=255):
    return max(low, min(high, int(value)))


def blend(dst, src):
    dr, dg, db, da = dst
    sr, sg, sb, sa = src
    if sa <= 0:
        return dst
    if sa >= 255:
        return (sr, sg, sb, max(da, 255))
    inv = 255 - sa
    out_a = clamp(sa + da * inv / 255)
    if out_a == 0:
        return (0, 0, 0, 0)
    out_r = clamp((sr * sa + dr * da * inv / 255) / out_a)
    out_g = clamp((sg * sa + dg * da * inv / 255) / out_a)
    out_b = clamp((sb * sa + db * da * inv / 255) / out_a)
    return (out_r, out_g, out_b, out_a)


def new_image(width, height, color):
    return [[color for _ in range(width)] for _ in range(height)]


def set_pixel(img, x, y, color, blend_mode=False):
    if 0 <= y < len(img) and 0 <= x < len(img[0]):
        if blend_mode:
            img[y][x] = blend(img[y][x], color)
        else:
            img[y][x] = color


def fill_rect(img, x0, y0, x1, y1, color, blend_mode=False):
    for y in range(y0, y1):
        for x in range(x0, x1):
            set_pixel(img, x, y, color, blend_mode)


def draw_rect(img, x0, y0, x1, y1, color):
    for x in range(x0, x1):
        set_pixel(img, x, y0, color, True)
        set_pixel(img, x, y1 - 1, color, True)
    for y in range(y0, y1):
        set_pixel(img, x0, y, color, True)
        set_pixel(img, x1 - 1, y, color, True)


def draw_line(img, x0, y0, x1, y1, color):
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        set_pixel(img, x0, y0, color, True)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def draw_circle(img, cx, cy, radius, color):
    for y in range(cy - radius, cy + radius + 1):
        for x in range(cx - radius, cx + radius + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= radius ** 2:
                set_pixel(img, x, y, color, True)


def write_png(path, width, height, pixels):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            r, g, b, a = pixels[y][x]
            raw.extend([r, g, b, a])
    compressed = zlib.compress(bytes(raw), 9)

    def chunk(tag, data):
        return (
            struct.pack("!I", len(data))
            + tag
            + data
            + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    header = struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)


def gradient_base(width, height, top_color, bottom_color):
    img = new_image(width, height, top_color)
    for y in range(height):
        t = y / max(1, height - 1)
        r = clamp(top_color[0] * (1 - t) + bottom_color[0] * t)
        g = clamp(top_color[1] * (1 - t) + bottom_color[1] * t)
        b = clamp(top_color[2] * (1 - t) + bottom_color[2] * t)
        for x in range(width):
            img[y][x] = (r, g, b, 255)
    return img


def add_scanlines(img, intensity=18):
    height = len(img)
    width = len(img[0])
    for y in range(0, height, 4):
        for x in range(width):
            r, g, b, a = img[y][x]
            img[y][x] = (clamp(r + intensity), clamp(g + intensity), clamp(b + intensity), a)


def make_neon_panel(width, height, accent):
    img = gradient_base(width, height, DARK, MID)
    for y in range(height):
        for x in range(width):
            if (x + y) % 7 == 0:
                set_pixel(img, x, y, (12, 18, 26, 255))
    for x in range(0, width, 6):
        draw_line(img, x, 0, x, height - 1, (accent[0], accent[1], accent[2], 180))
    for y in range(0, height, 6):
        draw_line(img, 0, y, width - 1, y, (accent[0], accent[1], accent[2], 160))
    draw_rect(img, 1, 1, width - 1, height - 1, (accent[0], accent[1], accent[2], 200))
    add_scanlines(img)
    return img


def make_chrome_panel(width, height):
    img = gradient_base(width, height, (32, 38, 46, 255), (16, 20, 28, 255))
    for y in range(height):
        for x in range(width):
            glow = int(20 * math.sin((x + y) / 6))
            r, g, b, a = img[y][x]
            img[y][x] = (clamp(r + glow), clamp(g + glow), clamp(b + glow), a)
    draw_line(img, 0, height // 3, width - 1, height // 3 - 2, (CHROME[0], CHROME[1], CHROME[2], 200))
    draw_line(img, 0, height // 3 + 2, width - 1, height // 3, (140, 150, 168, 160))
    draw_rect(img, 1, 1, width - 1, height - 1, (120, 130, 150, 180))
    return img


def make_circuit_tile(width, height, accent):
    img = gradient_base(width, height, (10, 14, 20, 255), (20, 24, 36, 255))
    for x in range(4, width, 8):
        draw_line(img, x, 2, x, height - 3, (accent[0], accent[1], accent[2], 180))
        draw_circle(img, x, 4, 2, (accent[0], accent[1], accent[2], 220))
    for y in range(6, height, 8):
        draw_line(img, 2, y, width - 3, y, (NEON_BLUE[0], NEON_BLUE[1], NEON_BLUE[2], 180))
        draw_circle(img, width - 5, y, 2, (NEON_BLUE[0], NEON_BLUE[1], NEON_BLUE[2], 220))
    draw_rect(img, 1, 1, width - 1, height - 1, (accent[0], accent[1], accent[2], 160))
    return img


def make_neon_glass(width, height, accent):
    img = gradient_base(width, height, (30, 10, 40, 255), (10, 5, 20, 255))
    for y in range(height):
        for x in range(width):
            if (x + y) % 5 == 0:
                set_pixel(img, x, y, (accent[0], accent[1], accent[2], 60), True)
    draw_rect(img, 1, 1, width - 1, height - 1, (accent[0], accent[1], accent[2], 200))
    return img


def make_sign_texture(width, height, accent):
    img = gradient_base(width, height, (20, 8, 18, 255), (10, 5, 14, 255))
    draw_rect(img, 2, 2, width - 2, height - 2, (accent[0], accent[1], accent[2], 220))
    for y in range(6, height - 6, 6):
        draw_line(img, 6, y, width - 7, y, (accent[0], accent[1], accent[2], 140))
    draw_rect(img, 8, height // 2 - 6, width - 8, height // 2 + 6, (accent[0], accent[1], accent[2], 180))
    return img


def make_lamp_texture(width, height, accent):
    img = gradient_base(width, height, (12, 16, 24, 255), (8, 10, 16, 255))
    draw_line(img, width // 2, 4, width // 2, height - 6, (90, 100, 120, 220))
    draw_circle(img, width // 2, 6, 5, (accent[0], accent[1], accent[2], 220))
    draw_circle(img, width // 2, 6, 9, (accent[0], accent[1], accent[2], 80))
    return img


def make_potion_texture(width, height, accent):
    img = gradient_base(width, height, (8, 12, 18, 255), (16, 20, 30, 255))
    draw_circle(img, width // 2, height // 2, min(width, height) // 3, (accent[0], accent[1], accent[2], 200))
    draw_circle(img, width // 2, height // 2, min(width, height) // 2, (accent[0], accent[1], accent[2], 80))
    return img


def make_datachip_texture(width, height):
    img = gradient_base(width, height, (14, 18, 24, 255), (6, 8, 12, 255))
    draw_rect(img, 4, 6, width - 4, height - 6, (NEON_CYAN[0], NEON_CYAN[1], NEON_CYAN[2], 200))
    for x in range(6, width - 6, 4):
        draw_line(img, x, 4, x, 6, (NEON_CYAN[0], NEON_CYAN[1], NEON_CYAN[2], 220))
        draw_line(img, x, height - 6, x, height - 4, (NEON_CYAN[0], NEON_CYAN[1], NEON_CYAN[2], 220))
    return img


def make_icon_base(width, height, accent):
    img = gradient_base(width, height, (8, 10, 16, 255), (18, 20, 28, 255))
    draw_rect(img, 2, 2, width - 2, height - 2, (accent[0], accent[1], accent[2], 200))
    return img


def make_icon(name, accent):
    width = height = 64
    img = make_icon_base(width, height, accent)
    if name == "panel":
        for x in range(8, width - 8, 8):
            draw_line(img, x, 8, x, height - 9, (accent[0], accent[1], accent[2], 180))
        for y in range(8, height - 8, 8):
            draw_line(img, 8, y, width - 9, y, (accent[0], accent[1], accent[2], 140))
    elif name == "chrome":
        draw_line(img, 8, height // 2, width - 9, height // 2 - 6, (CHROME[0], CHROME[1], CHROME[2], 220))
        draw_line(img, 8, height // 2 + 4, width - 9, height // 2 - 2, (150, 160, 180, 180))
    elif name == "circuit":
        draw_line(img, 12, 18, 52, 18, (NEON_BLUE[0], NEON_BLUE[1], NEON_BLUE[2], 200))
        draw_line(img, 12, 32, 44, 32, (accent[0], accent[1], accent[2], 200))
        draw_line(img, 20, 46, 52, 46, (accent[0], accent[1], accent[2], 180))
        draw_circle(img, 12, 18, 3, (NEON_BLUE[0], NEON_BLUE[1], NEON_BLUE[2], 220))
        draw_circle(img, 52, 46, 3, (accent[0], accent[1], accent[2], 220))
    elif name == "glass":
        draw_rect(img, 10, 10, width - 10, height - 10, (accent[0], accent[1], accent[2], 160))
        for y in range(12, height - 12, 4):
            draw_line(img, 12, y, width - 13, y, (accent[0], accent[1], accent[2], 80))
    elif name == "lamp":
        draw_line(img, width // 2, 12, width // 2, height - 16, (120, 130, 150, 220))
        draw_circle(img, width // 2, 16, 6, (accent[0], accent[1], accent[2], 220))
        draw_circle(img, width // 2, 16, 11, (accent[0], accent[1], accent[2], 80))
    elif name == "danger":
        draw_line(img, 32, 12, 12, 52, (accent[0], accent[1], accent[2], 200))
        draw_line(img, 32, 12, 52, 52, (accent[0], accent[1], accent[2], 200))
        draw_line(img, 12, 52, 52, 52, (accent[0], accent[1], accent[2], 200))
    elif name == "billboard":
        draw_rect(img, 12, 18, width - 12, height - 18, (accent[0], accent[1], accent[2], 200))
        for y in range(24, height - 22, 6):
            draw_line(img, 16, y, width - 17, y, (accent[0], accent[1], accent[2], 140))
    elif name == "arrow":
        draw_line(img, 14, 32, 46, 32, (accent[0], accent[1], accent[2], 220))
        draw_line(img, 36, 22, 46, 32, (accent[0], accent[1], accent[2], 220))
        draw_line(img, 36, 42, 46, 32, (accent[0], accent[1], accent[2], 220))
    elif name == "streetlamp":
        draw_line(img, 32, 14, 32, 50, (120, 130, 150, 220))
        draw_circle(img, 32, 16, 5, (accent[0], accent[1], accent[2], 200))
    elif name == "beacon":
        draw_rect(img, 28, 16, 36, 50, (accent[0], accent[1], accent[2], 200))
        draw_circle(img, 32, 16, 8, (accent[0], accent[1], accent[2], 100))
    elif name == "datachip":
        draw_rect(img, 16, 20, width - 16, height - 20, (accent[0], accent[1], accent[2], 200))
        for x in range(18, width - 18, 6):
            draw_line(img, x, 16, x, 20, (accent[0], accent[1], accent[2], 200))
            draw_line(img, x, height - 20, x, height - 16, (accent[0], accent[1], accent[2], 200))
    return img


def main():
    assets = []

    assets.append((BLOCK_TEX_DIR / "NeonPanel.png", make_neon_panel(32, 32, NEON_CYAN)))
    assets.append((BLOCK_TEX_DIR / "NeonPanel_Top.png", make_neon_panel(32, 32, NEON_CYAN)))
    assets.append((BLOCK_TEX_DIR / "ChromePanel.png", make_chrome_panel(32, 32)))
    assets.append((BLOCK_TEX_DIR / "ChromePanel_Top.png", make_chrome_panel(32, 32)))
    assets.append((BLOCK_TEX_DIR / "CircuitTile.png", make_circuit_tile(32, 32, NEON_CYAN)))
    assets.append((BLOCK_TEX_DIR / "NeonGlass.png", make_neon_glass(32, 32, NEON_PURPLE)))
    assets.append((BLOCK_TEX_DIR / "NeonGlass_Top.png", make_neon_glass(32, 32, NEON_PURPLE)))

    assets.append((BLOCK_MODEL_DIR / "DangerSign_Texture.png", make_sign_texture(64, 64, NEON_MAGENTA)))
    assets.append((BLOCK_MODEL_DIR / "PrototypeSign_Texture.png", make_sign_texture(128, 64, NEON_CYAN)))
    assets.append((BLOCK_MODEL_DIR / "StreetLamp_Texture.png", make_lamp_texture(128, 64, NEON_CYAN)))
    assets.append((BLOCK_MODEL_DIR / "HoloLamp_Texture.png", make_potion_texture(96, 64, NEON_CYAN)))
    assets.append((BLOCK_MODEL_DIR / "Beacon_Texture.png", make_potion_texture(96, 64, NEON_MAGENTA)))

    assets.append((INGOT_DIR / "NeonEcho_Datachip.png", make_datachip_texture(32, 32)))

    icon_specs = {
        "NeonEcho_Datachip.png": ("datachip", NEON_CYAN),
        "NeonEcho_NeonPanel.png": ("panel", NEON_CYAN),
        "NeonEcho_ChromePanel.png": ("chrome", CHROME),
        "NeonEcho_CircuitTile.png": ("circuit", NEON_BLUE),
        "NeonEcho_NeonGlass.png": ("glass", NEON_PURPLE),
        "NeonEcho_HoloLamp.png": ("lamp", NEON_CYAN),
        "NeonEcho_DangerSign.png": ("danger", NEON_MAGENTA),
        "NeonEcho_NeonBillboard.png": ("billboard", NEON_MAGENTA),
        "NeonEcho_NeonArrowSign.png": ("arrow", NEON_CYAN),
        "NeonEcho_NeonStreetLamp.png": ("streetlamp", NEON_CYAN),
        "NeonEcho_NeonBeacon.png": ("beacon", NEON_MAGENTA),
    }

    for filename, (shape, color) in icon_specs.items():
        assets.append((ICON_DIR / filename, make_icon(shape, color)))

    for path, img in assets:
        write_png(path, len(img[0]), len(img), img)

    print("Generated", len(assets), "textures.")


if __name__ == "__main__":
    main()
