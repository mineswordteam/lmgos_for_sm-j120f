#!/usr/bin/env bash
# Skeleton: unpack stock system.img, debloat, apply overlay, repack.
# Steps marked TODO depend on the real firmware layout (check after first run).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/work"; OUT="$ROOT/out"
rm -rf "$WORK" "$OUT"; mkdir -p "$WORK/fw" "$WORK/mnt" "$OUT"

FW="${1:-$ROOT/fw.zip}"
echo "[1/5] extract firmware"
cd "$WORK/fw"
case "$FW" in
  *.zip) unzip -o "$FW" ;;
  *.tar|*.tar.md5) tar -xf "$FW" ;;
  *) echo "unknown firmware format"; exit 1 ;;
esac
ls -la

echo "[2/5] locate system image"
SYS=$(find . -maxdepth 2 -name 'system.img*' | head -n1)
[ -n "$SYS" ] || { echo "system.img not found (TODO: adjust for this firmware)"; exit 1; }
case "$SYS" in *.lz4) lz4 -d "$SYS" system.img; SYS=system.img ;; esac
if file "$SYS" | grep -qi 'sparse'; then simg2img "$SYS" system.raw.img; else cp "$SYS" system.raw.img; fi

echo "[3/5] mount and modify (needs root)"
sudo mount -o loop,rw system.raw.img "$WORK/mnt"
grep -vE '^\s*(#|$)' "$ROOT/rom/debloat.txt" | while read -r p; do sudo rm -rf "$WORK/mnt/$p"; done
sudo cp -a "$ROOT/rom/overlay/." "$WORK/mnt/"
sudo umount "$WORK/mnt"

echo "[4/5] repack"
# TODO: confirm whether Odin expects sparse system.img inside a tar.md5
cp system.raw.img "$OUT/system.img"

echo "[5/5] done"
ls -la "$OUT"
