# HD textures

`HdTextures` (`./gradlew hdTextures`) upscales the textures OpenKeeper converted
from your copy of Dungeon Keeper II 4x with
[Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN), on your own machine. No
textures come with it, and it needs no Python.

It reads `assets/Converted/Textures` and writes PNGs with the same names to
`assets/Textures`. The game finds files under `assets/` on the classpath before
the converted ones, so it uses the HD textures with no other change.

## In the asset conversion

The asset conversion can make them too, as an optional step after the
textures. It is off by default. Switch it on with "Make HD textures" in the
first run's folder window, or in Options, Graphics (it then runs at the next
start). It needs a GPU with Vulkan and takes about 6 minutes.

- It downloads Real-ESRGAN the same way as `setup`, then upscales the 3D
  textures (`upscale --3d`). The interface is left out, because a game that
  doesn't draw HD interface images at their original's size draws them 4x too
  big.
- It runs again when the converted textures change.
- Switching it off deletes the HD textures it made, like `clean`.
- If it can't make them (no Vulkan GPU, no network, a failed download) it
  logs why, deletes what it made and switches itself off. The conversion
  carries on and the game uses the converted textures.

## Use

Needs Java and a GPU with Vulkan (or `--gpu -1` for the CPU, which is slow).
Works on Windows, Linux and macOS.

```
./gradlew hdTextures --args="setup"
./gradlew hdTextures --args="upscale --test-batch"
./gradlew run
./gradlew hdTextures --args="upscale"
./gradlew hdTextures --args="clean"
```

1. `setup` downloads the official `realesrgan-ncnn-vulkan` release for your
   system (checked against a SHA-256) to `~/.OpenKeeper/hd-textures` (change it
   with `--tools-dir`).
2. `upscale --test-batch` does only the terrain (`T_*`) and the wall and floor
   textures, so you can compare in game before the full run.
   `--gui-test-batch` does the same for the interface: the HUD's windows and
   bars, the minimap frame, the tabs and the room icons. `--3d` does all but
   the interface.
3. `upscale` does all of them. It skips textures that are already up to date,
   so it can be run again after a new conversion (`--force` redoes them).
   `--small` writes smaller PNGs (about 40% less disk, slower to write).
4. `clean` deletes only the files the tool wrote, and the game uses the
   converted textures again.

Other options: `--model`, `--tile`, `--gpu`, `--source` and `--dest`.

## What it does to each texture

- Pads the texture with its own opposite edges before upscaling and cuts the
  padding off after, so tiling textures stay seamless.
- Upscales the alpha channel separately. Cut-out alpha (only fully
  transparent or opaque pixels) stays cut-out.
- Spreads the visible colours into the fully transparent pixels first, so no
  dark fringes appear round cut-outs.
- Resizes soft textures (smoke, flames, glows, explosions) smoothly instead:
  Real-ESRGAN draws cracks, hairs and hard rims on them. Each texture's fine
  detail is measured; soft ones get a bicubic resize, detailed ones
  Real-ESRGAN, and those in between a blend of the two.
- Keeps the original's local brightness, colour and opacity: Real-ESRGAN tends
  to darken grainy textures and shift semi-transparent alpha.

## The interface

The interface (`GUI/`, and the briefing, movie and other images it shows from
elsewhere) is laid out by the size of its images in pixels. OpenKeeper's
screens give their images the original's size, so the HD interface keeps its
layout and gets the detail where it is drawn bigger than the original.

An `imageMode` that cuts an image in pixels (the HUD bars, the minimap frame,
the hint and message boxes, the scroll bars) is in the original's pixels, and
Nifty draws each piece at that size on screen. For the images listed in
`.hd-textures.properties`, OpenKeeper takes those pieces from the HD copy at
4x the coordinates, so they keep their layout too. An older OpenKeeper that
doesn't set the sizes draws the HD interface 4x too big; `clean` puts that
right.

## Left out

These are never upscaled:

- `GUI/Map/MapColours.png`: the minimap's colour table, read as data.
- `GUI/Map/Map-BG.png`: the minimap tiles the converted original as its rock,
  so an HD copy would only make the panel round the minimap differ from the
  rock in it.
- `cursor/`: not used. The mouse cursors come from `Interface/Cursors`, which
  this tool doesn't touch, and cursors have their hot spots in pixels.
- `Thumbnails/`: `MapColours.png` is the colour table of the map thumbnails,
  and OpenKeeper draws its own thumbnails.
- `Maps/`: not used.

## Notes

- The destination is added to the repository's `.git/info/exclude`, so the HD
  textures never show up in `git status` and are never committed.
- A record of what was written, with the settings used, is kept in
  `assets/Textures/.hd-textures.properties`. Changing `--model` redoes the
  textures on the next run. Files listed by the old Python script's record
  (`.hd-textures.json`) are taken over as this tool's own.
- Real-ESRGAN is BSD-3-Clause licensed. The upscaled textures are derived from
  the game's, so keep them to yourself.
