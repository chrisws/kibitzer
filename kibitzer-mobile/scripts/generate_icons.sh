#!/usr/bin/env bash
# Generate mipmap PNGs from the master SVG
# Requires: rsvg-convert (librsvg2-bin) or ImageMagick (convert)
set -euo pipefail

SVG="app/src/main/res/drawable/ic_launcher.svg"
RES="app/src/main/res"

# Detect converter
if command -v rsvg-convert &>/dev/null; then
    convert() { rsvg-convert -w "$2" -h "$2" "$1" -o "$3"; }
elif command -v convert &>/dev/null; then
    convert() { convert "$1" -resize "${2}x${2}" "$3"; }
else
    echo "ERROR: Need rsvg-convert or ImageMagick. Install with:"
    echo "  sudo apt install librsvg2-bin   # or"
    echo "  sudo apt install imagemagick"
    exit 1
fi

# Mipmap densities
declare -A SIZES=(
    [mdpi]=48
    [hdpi]=72
    [xhdpi]=96
    [xxhdpi]=144
    [xxxhdpi]=192
)

for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    size=${SIZES[$density]}
    out="$RES/mipmap-$density/ic_launcher.png"
    echo "Generating $out (${size}x${size})"
    convert "$SVG" "$size" "$out"
done

# Store listing icon (512x512)
out="$RES/drawable/ic_launcher_512.png"
echo "Generating $out (512x512)"
convert "$SVG" 512 "$out"

echo "Done! All icons generated."