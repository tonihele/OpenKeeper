#!/usr/bin/env python3
#
# Copyright (C) 2014-2026 OpenKeeper
#
# OpenKeeper is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# OpenKeeper is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
#
"""Makes HD versions of the converted DK II textures with Real-ESRGAN.

Reads the PNGs that OpenKeeper converted from your own copy of the game
(assets/Converted/Textures), upscales them 4x with Real-ESRGAN and writes them
under the same names to assets/Textures. Files under assets/ are on the
classpath, which jME searches before assets/Converted, so the game picks the
HD textures up without any other change. Deleting them brings the originals
back. No textures are part of this script; everything runs locally.

    python tools/hd-textures/hd_textures.py setup
    python tools/hd-textures/hd_textures.py upscale --test-batch
    python tools/hd-textures/hd_textures.py upscale
    python tools/hd-textures/hd_textures.py clean

See README.md next to this script for the details.
"""

import argparse
import hashlib
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
import venv
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = REPO_ROOT / "assets" / "Converted" / "Textures"
DEFAULT_DEST = REPO_ROOT / "assets" / "Textures"
DEFAULT_TOOLS_DIR = Path.home() / ".OpenKeeper" / "hd-textures"
MANIFEST_NAME = ".hd-textures.json"
MANIFEST_RETRIES = 40
SCALE = 4

# Real-ESRGAN's portable Vulkan build, so no Python ML stack or CUDA is needed
RELEASE_URL = "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/"
DOWNLOADS = {
    "Windows": ("realesrgan-ncnn-vulkan-20220424-windows.zip",
                "abc02804e17982a3be33675e4d471e91ea374e65b70167abc09e31acb412802d"),
    "Linux": ("realesrgan-ncnn-vulkan-20220424-ubuntu.zip",
              "e5aa6eb131234b87c0c51f82b89390f5e3e642b7b70f2b9bbe95b6a285a40c96"),
    "Darwin": ("realesrgan-ncnn-vulkan-20220424-macos.zip",
               "e0ad05580abfeb25f8d8fb55aaf7bedf552c375b5b4d9bd3c8d59764d2cc333a"),
}
PYTHON_PACKAGES = ["pillow>=11", "numpy>=2"]
MODELS = ["realesrgan-x4plus", "realesrgan-x4plus-anime", "realesr-animevideov3-x4"]

# Left out of a full run unless --all is given: a path ending in / is a
# folder, anything else one file. The interface (GUI/) is in: the game draws
# an HD interface image at its original's size.
DEFAULT_EXCLUDES = {
    "GUI/Map/MapColours.png": "the minimap's colour table, read as data",
    "GUI/Map/Map-BG.png": "the minimap tiles the converted original as its rock, so an HD copy "
                          "would only make the panel round the minimap differ from the rock in it",
    "cursor/": "not used: the mouse cursors come from Interface/Cursors, and cursors have their "
               "hot spots in pixels",
    "Thumbnails/": "MapColours.png is the colour table of the map thumbnails, and OpenKeeper "
                   "draws its own thumbnails",
    "Maps/": "not used",
}

# The test batch: the terrain (T_*) and the other wall and floor textures
TEST_BATCH = r"(?i)^(T_[^/]*|[^/]*(wall|floor)[^/]*)\.png$"

# The interface test batch: the HUD's windows and bars, the minimap frame,
# the tabs and the room icons
GUI_TEST_BATCH = r"(?i)^GUI/(Windows|Map|Tabs|Rooms)/[^/]*\.png$"

# Source pixels added round each texture before upscaling and cut off after,
# so the edges of tiling textures are upscaled with their neighbours in view
DEFAULT_PAD = 16

# Real-ESRGAN invents cracks, hairs and hard rims on soft images: smoke, flames,
# glows, explosions. How much fine detail a texture has is measured as its mean
# absolute Laplacian over its mean absolute gradient. Below SOFT_BELOW it is
# resized smoothly (bicubic), above DETAILED_ABOVE upscaled with Real-ESRGAN,
# and in between the two are blended.
SOFT_BELOW = 0.75
DETAILED_ABOVE = 1.05


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--tools-dir", type=Path, default=DEFAULT_TOOLS_DIR,
                        help="where Real-ESRGAN and the Python packages are installed (default: %(default)s)")
    commands = parser.add_subparsers(dest="command", required=True)

    commands.add_parser("setup", help="download Real-ESRGAN and install Pillow and NumPy")

    upscale = commands.add_parser("upscale", help="upscale the textures")
    upscale.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    upscale.add_argument("--dest", type=Path, default=DEFAULT_DEST)
    which = upscale.add_mutually_exclusive_group()
    which.add_argument("--test-batch", action="store_true",
                       help="only the terrain, wall and floor textures in the top folder")
    which.add_argument("--gui-test-batch", action="store_true",
                       help="only some of the interface: the HUD windows, the minimap frame, the tabs and the room icons")
    which.add_argument("--match", metavar="REGEX",
                       help="only the textures whose path relative to the source matches, e.g. '(?i)lava'")
    upscale.add_argument("--all", action="store_true",
                         help="also what is left out by default: " + ", ".join(DEFAULT_EXCLUDES))
    upscale.add_argument("--model", default=MODELS[0], choices=MODELS)
    upscale.add_argument("--pad", type=int, default=DEFAULT_PAD)
    upscale.add_argument("--pad-mode", default="wrap", choices=["wrap", "edge", "reflect"],
                         help="how the padding is filled: wrap suits tiling textures (default)")
    upscale.add_argument("--no-soft-blend", action="store_true",
                         help="use Real-ESRGAN for soft textures (smoke, flames, glows) too")
    upscale.add_argument("--no-level-match", action="store_true",
                         help="keep Real-ESRGAN's brightness and alpha levels instead of the original's")
    upscale.add_argument("--gpu", default="auto", help="Real-ESRGAN's -g: a GPU id, or -1 for the CPU")
    upscale.add_argument("--tile", type=int, default=0, help="Real-ESRGAN's -t tile size, 0 = auto")
    upscale.add_argument("--force", action="store_true", help="redo textures that are already up to date")
    upscale.add_argument("--overwrite-unmanaged", action="store_true",
                         help="allow overwriting files in the destination that this script didn't write")
    upscale.add_argument("--dry-run", action="store_true", help="only list what would be upscaled")
    upscale.add_argument("--no-git-exclude", action="store_true",
                         help="don't add the HD textures to the repository's .git/info/exclude")

    clean = commands.add_parser("clean", help="delete the HD textures this script wrote")
    clean.add_argument("--dest", type=Path, default=DEFAULT_DEST)

    args = parser.parse_args()
    if args.command == "setup":
        setup(args.tools_dir)
    elif args.command == "upscale":
        upscale_textures(args)
    else:
        clean_textures(args.dest)


# --- Setup ----------------------------------------------------------------------------------------

def setup(tools_dir):
    tools_dir.mkdir(parents=True, exist_ok=True)

    system = platform.system()
    if system not in DOWNLOADS:
        sys.exit(f"No Real-ESRGAN build for {system}")
    name, sha256 = DOWNLOADS[system]
    target = tools_dir / Path(name).stem
    if find_executable(tools_dir) is None:
        archive = tools_dir / name
        print(f"Downloading {RELEASE_URL}{name}")
        urllib.request.urlretrieve(RELEASE_URL + name, archive)
        digest = hashlib.sha256(archive.read_bytes()).hexdigest()
        if digest != sha256:
            archive.unlink()
            sys.exit(f"Checksum mismatch for {name}: got {digest}, expected {sha256}")
        with zipfile.ZipFile(archive) as zip_file:
            zip_file.extractall(target)
        archive.unlink()
        executable = find_executable(tools_dir)
        if executable is None:
            sys.exit(f"realesrgan-ncnn-vulkan not found in {target}")
        if system != "Windows":
            executable.chmod(0o755)
    print(f"Real-ESRGAN: {find_executable(tools_dir)}")

    venv_dir = tools_dir / "venv"
    python = venv_python(tools_dir)
    if not python.exists():
        print(f"Creating a virtual environment in {venv_dir}")
        venv.EnvBuilder(with_pip=True).create(venv_dir)
    subprocess.run([str(python), "-m", "pip", "install", "--quiet", "--disable-pip-version-check", "--upgrade", *PYTHON_PACKAGES], check=True)
    print(f"Python packages installed in {venv_dir}")
    print("Setup done. Next: python tools/hd-textures/hd_textures.py upscale --test-batch")


def find_executable(tools_dir):
    name = "realesrgan-ncnn-vulkan.exe" if platform.system() == "Windows" else "realesrgan-ncnn-vulkan"
    found = sorted(tools_dir.glob(f"*/{name}"))
    return found[0] if found else None


def venv_python(tools_dir):
    if platform.system() == "Windows":
        return tools_dir / "venv" / "Scripts" / "python.exe"
    return tools_dir / "venv" / "bin" / "python"


def ensure_packages(tools_dir):
    """Runs this script again in the tools' virtual environment if Pillow or NumPy is missing"""
    try:
        import numpy  # noqa: F401
        import PIL  # noqa: F401
        return
    except ImportError:
        pass
    python = venv_python(tools_dir)
    if not python.exists() or Path(sys.executable).resolve() == python.resolve():
        sys.exit("Pillow and NumPy are missing. Run: python tools/hd-textures/hd_textures.py setup")
    sys.exit(subprocess.run([str(python), *sys.argv]).returncode)


# --- Upscaling ------------------------------------------------------------------------------------

def upscale_textures(args):
    ensure_packages(args.tools_dir)
    executable = find_executable(args.tools_dir)
    if executable is None:
        sys.exit("Real-ESRGAN is not installed. Run: python tools/hd-textures/hd_textures.py setup")
    source = args.source.resolve()
    dest = args.dest.resolve()
    if not source.is_dir():
        sys.exit(f"No converted textures in {source}. Run the game once to convert DK II's assets.")

    manifest = load_manifest(dest)
    settings = {"model": args.model, "pad": args.pad, "pad_mode": args.pad_mode,
                "level_match": not args.no_level_match}
    textures = select_textures(source, args)
    if args.no_soft_blend:
        weights = dict.fromkeys(textures, 1.0)
    else:
        print("Measuring detail")
        weights = detail_weights(source, textures)
    todo = []
    for rel in textures:
        out = dest / rel
        entry = manifest["files"].get(rel)
        if out.exists() and entry is None and not args.overwrite_unmanaged:
            print(f"Skipping {rel}: the destination has a file this script didn't write")
            continue
        up_to_date = (entry is not None and out.exists() and entry.get("settings") == settings
                      and entry.get("source_mtime") == (source / rel).stat().st_mtime_ns
                      and entry.get("esrgan_weight", 1.0) == weights[rel])
        if args.force or not up_to_date:
            todo.append(rel)

    print(f"{len(textures)} textures selected, {len(todo)} to upscale, {len(textures) - len(todo)} up to date")
    if args.dry_run:
        for rel in todo:
            print("  " + rel)
        if not args.all:
            print("Left out (--all to include them):")
            for path, reason in DEFAULT_EXCLUDES.items():
                print(f"  {path}: {reason}")
        return
    if not todo:
        return

    started = time.monotonic()
    with tempfile.TemporaryDirectory(prefix="hd-textures-") as work:
        work_in = Path(work) / "in"
        work_out = Path(work) / "out"
        work_in.mkdir()
        work_out.mkdir()

        print("Preparing")
        jobs = [prepare(source / rel, work_in, index, args.pad, args.pad_mode, weights[rel])
                for index, rel in enumerate(todo)]

        if any(work_in.iterdir()):
            print(f"Upscaling with {args.model}")
            command = [str(executable), "-i", str(work_in), "-o", str(work_out), "-n", args.model, "-s", str(SCALE),
                       "-f", "png", "-m", str(executable.parent / "models"), "-t", str(args.tile)]
            if args.gpu != "auto":
                command += ["-g", args.gpu]
            result = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True,
                                    errors="replace")
            if result.returncode != 0:
                sys.exit(f"Real-ESRGAN failed ({result.returncode}):\n{result.stderr[-2000:]}")

        print("Writing")
        failed = []
        for rel, job in zip(todo, jobs):
            try:
                finish(job, work_out, dest / rel, args.pad, not args.no_level_match)
            except (OSError, ValueError) as error:
                failed.append(f"{rel}: {error}")
                continue
            manifest["files"][rel] = {"source_mtime": (source / rel).stat().st_mtime_ns, "settings": settings,
                                      "esrgan_weight": weights[rel]}
            save_manifest(dest, manifest)

    if not args.no_git_exclude:
        add_git_exclude(dest)
    print(f"Done: {len(todo) - len(failed)} textures in {time.monotonic() - started:.0f} s, written to {dest}")
    if failed:
        print(f"{len(failed)} failed:")
        for line in failed:
            print("  " + line)
        sys.exit(1)


def select_textures(source, args):
    textures = sorted(path.relative_to(source).as_posix() for path in source.rglob("*")
                      if path.is_file() and path.suffix.lower() == ".png")
    if not args.all:
        textures = [rel for rel in textures if not is_excluded(rel)]
    pattern = TEST_BATCH if args.test_batch else GUI_TEST_BATCH if args.gui_test_batch else args.match
    if pattern:
        regex = re.compile(pattern)
        textures = [rel for rel in textures if regex.search(rel)]
    return textures


def is_excluded(rel):
    rel = rel.lower()
    for path in DEFAULT_EXCLUDES:
        path = path.lower()
        if rel == path or (path.endswith("/") and rel.startswith(path)):
            return True
    return False


def detail_weights(source, textures):
    """How much of each texture comes from Real-ESRGAN, the rest from a bicubic resize.

    The frames of an animation (names that differ only in their trailing number)
    share one weight, so an animation doesn't change style from frame to frame.
    """
    import statistics

    groups = {}
    for path in source.rglob("*"):
        if path.is_file() and path.suffix.lower() == ".png":
            rel = path.relative_to(source).as_posix()
            groups.setdefault(animation_key(rel), []).append(rel)

    scores = {}

    def score(rel):
        if rel not in scores:
            scores[rel] = detail_score(source / rel)
        return scores[rel]

    weights = {}
    for rel in textures:
        members = [score(member) for member in groups.get(animation_key(rel), [rel])]
        members = [value for value in members if value is not None]
        if not members:
            weights[rel] = 0.0  # A flat colour, nothing to upscale
            continue
        value = statistics.median(members)
        weight = (value - SOFT_BELOW) / (DETAILED_ABOVE - SOFT_BELOW)
        weights[rel] = round(min(1.0, max(0.0, weight)), 2)
    return weights


def animation_key(rel):
    path = Path(rel)
    return (path.parent / re.sub(r"\d+$", "", path.stem)).as_posix().lower()


def detail_score(path):
    """Mean absolute Laplacian over mean absolute gradient, the higher of colour and alpha.

    Crisp, detailed textures score above 1, soft ones (blurred glows, smoke)
    well below. None for a flat image.
    """
    import numpy as np
    from PIL import Image

    with Image.open(path) as image:
        has_alpha = image.mode in ("RGBA", "LA", "PA") or "transparency" in image.info
        pixels = np.asarray(image.convert("RGBA")).astype(np.float32)
    channels = [pixels[..., :3].mean(axis=2)] + ([pixels[..., 3]] if has_alpha else [])
    values = []
    for channel in channels:
        if channel.std() <= 1 or min(channel.shape) < 3:
            continue
        gradient = np.abs(np.diff(channel, axis=0)).mean() + np.abs(np.diff(channel, axis=1)).mean()
        laplacian = np.abs(4 * channel[1:-1, 1:-1] - channel[:-2, 1:-1] - channel[2:, 1:-1]
                           - channel[1:-1, :-2] - channel[1:-1, 2:]).mean()
        values.append(float(laplacian / max(gradient, 1e-6)))
    return max(values) if values else None


def prepare(path, work_in, index, pad, pad_mode, weight):
    """Pads the colour and the alpha, and writes them for Real-ESRGAN if it is used"""
    import numpy as np
    from PIL import Image

    with Image.open(path) as image:
        has_alpha = image.mode in ("RGBA", "LA", "PA") or "transparency" in image.info
        pixels = np.asarray(image.convert("RGBA" if has_alpha else "RGB"))

    rgb = pixels[..., :3]
    job = {"name": f"{index:05d}", "source": path, "size": (pixels.shape[1], pixels.shape[0]), "alpha": None,
           "weight": weight}
    if has_alpha:
        alpha = pixels[..., 3]
        values = np.unique(alpha)
        if len(values) == 1:
            job["alpha"] = {"constant": int(values[0])}
        else:
            # Binary alpha (colour keyed cutouts) is kept binary, so the edges stay crisp
            job["alpha"] = {"binary": bool(set(values.tolist()) <= {0, 255})}
            job["a"] = np.pad(np.repeat(alpha[..., None], 3, axis=2), padding(pad), mode=pad_mode)
        rgb = bleed_colour(rgb, alpha)

    job["c"] = np.pad(rgb, padding(pad), mode=pad_mode)
    if weight > 0:
        for suffix in ("c", "a"):
            if suffix in job:
                Image.fromarray(job[suffix]).save(work_in / f"{job['name']}_{suffix}.png")
    return job


def padding(pad):
    return ((pad, pad), (pad, pad), (0, 0))


def bleed_colour(rgb, alpha, steps=8):
    """Spreads the colour of the visible pixels into the invisible ones next to them.

    The colour under fully transparent pixels is often black or garbage, and the
    upscaler would otherwise smear it into the visible edges.
    """
    import numpy as np

    known = alpha > 0
    if known.all() or not known.any():
        return rgb
    colour = rgb.astype(np.float32) * known[..., None]
    for _ in range(steps):
        if known.all():
            break
        weight = np.pad(known.astype(np.float32), 1, mode="edge")
        summed = np.pad(colour, ((1, 1), (1, 1), (0, 0)), mode="edge")
        height, width = known.shape
        total = np.zeros_like(colour)
        count = np.zeros(known.shape, np.float32)
        for dy in (0, 1, 2):
            for dx in (0, 1, 2):
                total += summed[dy:dy + height, dx:dx + width]
                count += weight[dy:dy + height, dx:dx + width]
        grow = ~known & (count > 0)
        colour[grow] = total[grow] / count[grow][..., None]
        known = known | grow
    colour[~known] = colour[known].mean(axis=0)
    return np.clip(np.rint(colour), 0, 255).astype(np.uint8)


def finish(job, work_out, out, pad, match_levels):
    """Crops the padding off, puts the alpha back and writes the texture"""
    import numpy as np
    from PIL import Image

    width, height = job["size"]
    crop = pad * SCALE

    def cropped(pixels):
        pixels = pixels[crop:crop + height * SCALE, crop:crop + width * SCALE]
        if pixels.shape[:2] != (height * SCALE, width * SCALE):
            raise ValueError(f"unexpected output size {pixels.shape[1]}x{pixels.shape[0]}")
        return pixels.astype(np.float32)

    def load(suffix):
        """Real-ESRGAN's output blended with a bicubic resize by the job's weight"""
        weight = job["weight"]
        padded = job[suffix]
        smooth = cropped(np.asarray(Image.fromarray(padded).resize(
            (padded.shape[1] * SCALE, padded.shape[0] * SCALE), Image.BICUBIC)))
        if weight == 0:
            return smooth
        path = work_out / f"{job['name']}_{suffix}.png"
        if not path.exists():
            raise ValueError("Real-ESRGAN wrote no output")
        with Image.open(path) as image:
            detailed = cropped(np.asarray(image.convert("RGB")))
        return detailed * weight + smooth * (1 - weight)

    alpha = job["alpha"]
    with Image.open(job["source"]) as image:
        original = np.asarray(image.convert("RGB" if alpha is None else "RGBA")).astype(np.float32)

    rgb = load("c")
    if match_levels:
        rgb = keep_levels(rgb, original[..., :3])
    rgb = np.clip(np.rint(rgb), 0, 255).astype(np.uint8)
    if alpha is None:
        result = Image.fromarray(rgb, "RGB")
    else:
        if "constant" in alpha:
            channel = np.full(rgb.shape[:2], alpha["constant"], np.uint8)
        else:
            channel = load("a").mean(axis=2)
            if alpha["binary"]:
                channel = np.where(channel >= 128, 255, 0)
            elif match_levels:
                channel = keep_levels(channel[..., None], original[..., 3:])[..., 0]
            channel = np.clip(np.rint(channel), 0, 255).astype(np.uint8)
        result = Image.fromarray(np.dstack([rgb, channel]), "RGBA")

    out.parent.mkdir(parents=True, exist_ok=True)
    temporary = out.with_name(out.name + ".tmp")
    result.save(temporary, format="PNG")
    os.replace(temporary, out)


def keep_levels(upscaled, original, blur_passes=2):
    """Brings the local brightness, colour and opacity back to the original's.

    Real-ESRGAN tends to darken grainy textures and to shift semi-transparent
    alpha. Shrunk back to the original size, the upscaled image is compared
    with the original; the difference, blurred so it carries only the broad
    levels and not the pixel blocks, is spread back over the upscaled image.
    """
    import numpy as np
    from PIL import Image

    height, width, channels = original.shape
    shrunk = upscaled.reshape(height, SCALE, width, SCALE, channels).mean(axis=(1, 3))
    difference = original - shrunk
    for _ in range(blur_passes):
        # 3x3 box blur that wraps round the edges, like the tiling textures do
        difference = sum(np.roll(np.roll(difference, dy, axis=0), dx, axis=1)
                         for dy in (-1, 0, 1) for dx in (-1, 0, 1)) / 9
    correction = np.stack([np.asarray(Image.fromarray(difference[..., channel].astype(np.float32), "F")
                                      .resize((width * SCALE, height * SCALE), Image.BILINEAR))
                           for channel in range(channels)], axis=2)
    return upscaled + correction


# --- Bookkeeping ----------------------------------------------------------------------------------

def load_manifest(dest):
    path = dest / MANIFEST_NAME
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    return {"files": {}}


def save_manifest(dest, manifest):
    dest.mkdir(parents=True, exist_ok=True)
    path = dest / MANIFEST_NAME
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(json.dumps(manifest, indent=1, sort_keys=True), encoding="utf-8")

    # On Windows another program (a build copying the assets, a virus scanner)
    # can keep the manifest open for a moment
    for attempt in range(MANIFEST_RETRIES):
        try:
            os.replace(temporary, path)
            return
        except PermissionError:
            if attempt == MANIFEST_RETRIES - 1:
                raise
            time.sleep(0.25)


def clean_textures(dest):
    dest = dest.resolve()
    manifest = load_manifest(dest)
    removed = 0
    folders = set()
    for rel in manifest["files"]:
        path = dest / rel
        if path.exists():
            path.unlink()
            removed += 1
        folders.update(parent for parent in path.parents if dest in parent.parents)
    for folder in sorted(folders, key=lambda folder: len(folder.parts), reverse=True):
        if folder.is_dir() and not any(folder.iterdir()):
            folder.rmdir()
    (dest / MANIFEST_NAME).unlink(missing_ok=True)
    print(f"Removed {removed} HD textures from {dest}; the game uses the converted ones again")


def add_git_exclude(dest):
    """Keeps the HD textures out of git status, so they don't get committed by accident"""
    try:
        top = subprocess.run(["git", "rev-parse", "--show-toplevel"], cwd=dest, capture_output=True,
                             text=True, check=True).stdout.strip()
        exclude = subprocess.run(["git", "rev-parse", "--git-path", "info/exclude"], cwd=dest,
                                 capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return
    rel = dest.relative_to(Path(top).resolve()).as_posix()
    exclude_path = Path(exclude) if Path(exclude).is_absolute() else dest / exclude
    lines = [f"/{rel}/**/*.png", f"/{rel}/**/*.PNG", f"/{rel}/{MANIFEST_NAME}"]
    existing = exclude_path.read_text(encoding="utf-8").splitlines() if exclude_path.exists() else []
    missing = [line for line in lines if line not in existing]
    if missing:
        exclude_path.parent.mkdir(parents=True, exist_ok=True)
        with exclude_path.open("a", encoding="utf-8") as file:
            file.write("# HD textures from tools/hd-textures/hd_textures.py\n" + "\n".join(missing) + "\n")


if __name__ == "__main__":
    main()
