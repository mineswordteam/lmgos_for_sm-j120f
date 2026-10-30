# LMG OS for SM-J120F

Custom ROM for Samsung Galaxy J1 (2016) SM-J120F, based on stock Android 6.0.1.
Built with GitHub Actions from the stock firmware (debloat + overlay + theme).

## Layout
- `rom/build.sh`    unpack stock system.img, apply changes, repack
- `rom/debloat.txt` apps to remove (one path per line)
- `rom/overlay/`    files copied into /system (boot animation, apks, fonts)
- `.github/workflows/build.yml` manual build (workflow_dispatch)

## Setup
Add repo secret `STOCK_FW_URL` (direct link to the stock AP firmware zip/tar.md5).
Always keep a copy of the full stock firmware for recovery.
