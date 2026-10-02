import os
from PIL import Image, ImageDraw

src = r'c:\Users\JISHNU PG\Music\Hermes Agent\Hermes-x\android\app_logo.png'
base_res = r'c:\Users\JISHNU PG\Music\Hermes Agent\Hermes-x\android\app\src\main\res'

sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

im = Image.open(src).convert('RGBA')

# Delete old drawable/ic_launcher_foreground.xml if present
old_fg = os.path.join(base_res, 'drawable', 'ic_launcher_foreground.xml')
if os.path.exists(old_fg):
    os.remove(old_fg)
    print('Removed old vector drawable/ic_launcher_foreground.xml')

for folder, sz in sizes.items():
    target_dir = os.path.join(base_res, folder)
    os.makedirs(target_dir, exist_ok=True)
    
    # 1. Square launcher icon
    sq = im.resize((sz, sz), Image.Resampling.LANCZOS)
    sq.save(os.path.join(target_dir, 'ic_launcher.png'), 'PNG')
    
    # 2. Round launcher icon
    mask = Image.new('L', (sz, sz), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, sz, sz), fill=255)
    round_im = Image.new('RGBA', (sz, sz), (255, 255, 255, 255))
    round_im.paste(sq, (0, 0))
    round_im.putalpha(mask)
    round_im.save(os.path.join(target_dir, 'ic_launcher_round.png'), 'PNG')
    
    # 3. Adaptive foreground (108dp viewport)
    fg_sz = int(sz * 108 / 48)
    safe_sz = int(fg_sz * 0.72)
    fg_canvas = Image.new('RGBA', (fg_sz, fg_sz), (255, 255, 255, 0))
    artwork = im.resize((safe_sz, safe_sz), Image.Resampling.LANCZOS)
    offset = (fg_sz - safe_sz) // 2
    fg_canvas.paste(artwork, (offset, offset))
    fg_canvas.save(os.path.join(target_dir, 'ic_launcher_foreground.png'), 'PNG')

anydpi_dir = os.path.join(base_res, 'mipmap-anydpi-v26')
os.makedirs(anydpi_dir, exist_ok=True)

xml_content = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""

for name in ['ic_launcher.xml', 'ic_launcher_round.xml']:
    with open(os.path.join(anydpi_dir, name), 'w', encoding='utf-8') as f:
        f.write(xml_content)

print('Done updating icons and XML!')
