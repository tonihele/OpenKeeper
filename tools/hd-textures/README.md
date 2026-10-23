# HD textures

`hd_textures.py` upscales the textures OpenKeeper converted from your copy of
Dungeon Keeper II 4x with [Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN),
on your own machine. No textures come with it.

It reads `assets/Converted/Textures` and writes PNGs with the same names to
`assets/Textures`. The game finds files under `assets/` on the classpath before
the converted ones, so it uses the HD textures with no other change.

## Use

Needs Python 3.10 or later and a GPU with Vulkan (or `--gpu -1` for the CPU, which is slow).

```
python tools/hd-textures/hd_textures.py setup
python tools/hd-textures/hd_textures.py upscale --test-batch
./gradlew run
python tools/hd-textures/hd_textures.py upscale
python tools/hd-textures/hd_textures.py clean
```

1. `setup` downloads Real-ESRGAN's portable Vulkan build (checked against a
   SHA-256) and installs Pillow and NumPy into a virtual environment, both under
   `~/.OpenKeeper/hd-textures` (change it with `--tools-dir`).
2. `upscale --test-batch` does only the terrain (`T_*`) and the wall and floor
   textures, so you can compare in game before the full run. `--match REGEX`
   picks other textures by their path, e.g. `--match "(?i)^lair"`.
3. `upscale` does all of them. It skips textures that are already up to date,
   so it can be run again after a new conversion.
4. `clean` deletes only the files the script wrote, and the game uses the
   converted textures again.

## What it does to each texture

- Pads the texture with its own opposite edges before upscaling and cuts the
  padding off after, so tiling textures stay seamless (`--pad`, `--pad-mode`).
- Upscales the alpha channel separately. Cut-out alpha (only fully
  transparent or opaque pixels) stays cut-out.
- Spreads the visible colours into the fully transparent pixels first, so no
  dark fringes appear round cut-outs.
- Keeps the original's local brightness, colour and opacity: Real-ESRGAN tends
  to darken grainy textures and shift semi-transparent alpha
  (`--no-level-match` turns this off).

## Left out of the full run

`GUI/`, `cursor/`, `Thumbnails/` and `Maps/` are skipped unless `--all` is
given. The interface is laid out by image size in pixels, some of those images
are read as data (the minimap palette `MapColours.png`, the panel background),
and cursors have their hot spots in pixels.

## Notes

- The HD textures are added to the repository's `.git/info/exclude`, so they
  don't show up in `git status` (`--no-git-exclude` to skip that).
- A record of what was written, with the settings used, is kept in
  `assets/Textures/.hd-textures.json`. Changing `--model` or the padding redoes
  the textures on the next run.
- Real-ESRGAN is BSD-3-Clause licensed. The upscaled textures are derived from
  the game's, so keep them to yourself.
