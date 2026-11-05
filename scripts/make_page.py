"""Builds the app page android/assets/index.html from web/template.html and the song data.

Usage: python3 scripts/make_page.py web/template.html build/data.json android/assets/index.html android/apktool.yml
- Swaps the Google Fonts link for the bundled fonts in android/assets/fonts, so the app works offline.
- Puts the song data where the template says /*DATA*/.
- Sets the version shown on the About screen from versionName in apktool.yml.
"""
import re, sys

template, data_json, out, apktool_yml = sys.argv[1:5]
t = open(template, encoding='utf-8').read()
d = open(data_json, encoding='utf-8').read()

m = re.search(r'versionName:\s*([^\s]+)', open(apktool_yml, encoding='utf-8').read())
version = m.group(1).strip("'\"") if m else 'dev'
t, n = re.subn(r"const APP_VERSION='[^']*';", f"const APP_VERSION='{version}';", t)
assert n == 1, 'APP_VERSION not found in template'

faces = [('Bricolage Grotesque', 600, 'bricolage-grotesque-latin-600-normal'),
         ('Bricolage Grotesque', 800, 'bricolage-grotesque-latin-800-normal'),
         ('Atkinson Hyperlegible', 400, 'atkinson-hyperlegible-latin-400-normal'),
         ('Atkinson Hyperlegible', 700, 'atkinson-hyperlegible-latin-700-normal'),
         ('JetBrains Mono', 500, 'jetbrains-mono-latin-500-normal'),
         ('JetBrains Mono', 700, 'jetbrains-mono-latin-700-normal')]
css = '<style>' + ''.join(
    f"@font-face{{font-family:'{f}';font-weight:{w};font-style:normal;font-display:swap;src:url(fonts/{n}.woff2) format('woff2')}}"
    for f, w, n in faces) + '</style>'
t = re.sub(r'<link rel="preconnect"[^>]*>\n', '', t)
t = re.sub(r'<link rel="stylesheet" href="https://fonts.googleapis.com[^>]*>', css, t)
assert 'googleapis' not in t

head = ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
        '<style>:root{padding-top:env(safe-area-inset-top);padding-bottom:env(safe-area-inset-bottom)}'
        'body{margin:0;font-size:14px}img{max-width:100%}</style></head><body>')
open(out, 'w', encoding='utf-8').write(head + t.replace('/*DATA*/', d) + '</body></html>')
print(f'page built: {out} (version {version})')
