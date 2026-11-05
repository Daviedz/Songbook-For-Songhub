"""Optional: redraws the launcher icon into android/res/mipmap-*/ic_launcher.png (needs Pillow)."""
import sys
from PIL import Image, ImageDraw
res = sys.argv[1] if len(sys.argv) > 1 else 'android/res'
def icon(sz):
    W = sz * 4; im = Image.new('RGBA', (W, W), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * .22), fill=(42, 31, 77, 255))
    c = W / 2; r = W * .33; d.ellipse([c - r, c - r, c + r, c + r], fill=(226, 51, 122, 255))
    w = (255, 255, 255, 255); hx, hy, hr = W * .44, W * .62, W * .085
    d.ellipse([hx - hr * 1.25, hy - hr, hx + hr * 1.25, hy + hr], fill=w)
    sx = hx + hr * 1.05; d.rectangle([sx - W * .03, W * .30, sx, hy], fill=w)
    d.polygon([(sx - W * .01, W * .30), (sx + W * .14, W * .36), (sx + W * .14, W * .43), (sx - W * .01, W * .37)], fill=w)
    return im.resize((sz, sz), Image.LANCZOS)
for name, sz in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
    icon(sz).save(f'{res}/mipmap-{name}/ic_launcher.png')
print('icons written')
