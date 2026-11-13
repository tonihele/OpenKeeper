# Vulkan rendering

## Backend design

OpenKeeper keeps its existing jMonkeyEngine `LWJGL_OPENGL*` backends. The new
Vulkan choice uses jME's ANGLE GLES3 renderer and asks ANGLE to select its
Vulkan renderer before SDL creates the context. Game rendering still uses the
jME `GLRenderer` API; ANGLE translates the GLES calls and shaders to Vulkan.
After the context is initialized, OpenKeeper queries the active GLES or desktop
OpenGL renderer and version strings. The window title and startup log identify
the API reported by that context, independently of the saved renderer choice;
a usable OpenGL context is therefore labeled OpenGL even if Vulkan was selected.

OpenKeeper's flat and vertex-lit terrain material definitions, unit flower,
progress indicator, and movie material now provide GLSL ES 3.00/3.10 variants
while retaining the existing GLSL 1.50 path for desktop OpenGL. These shaders
use jME's GLSL compatibility include to keep shared source working across the
desktop and GLES shader versions. The smoke scene compiles and renders each
material on both backends.

jME 3.10 moved its LWJGL window system to SDL3. Display mode enumeration and
borderless fullscreen therefore use SDL3 too. OpenKeeper explicitly keeps
LWJGL OpenGL 3 as the default because jME 3.10 otherwise defaults to ANGLE
GLES3. Existing stored renderer identifiers are preserved, and the settings
menus expose Vulkan on supported platforms.

## Runtime support

The bundled Vulkan compatibility runtime currently targets Windows x86_64.
Its `libEGL.dll` and `libGLESv2.dll` are in
`src/main/resources/native/angle/windows/x86_64/`; ANGLE and Vulkan Loader
license notices are beside them. OpenKeeper offers the Vulkan option only on
this platform. A saved Vulkan selection moved to another platform falls back
to the jME OpenGL default.

The runtime was built from ANGLE commit
`6f0b04ba184b1a0a1411356e738c0ed752071c26` and its Vulkan Loader checkout at
`506124c4ac349c7a515ab71fdd460f285fae71bd`. It statically links the Vulkan
loader. `tools/angle/vulkan-loader-dllmain.patch` merges the loader's Windows
DLL initialization with ANGLE's existing `DllMain`; without this glue, both
libraries define the entry point. The relevant GN configuration is:

```text
libEGL.dll    SHA-256 9f866f5bb937e0241134b6fcaba2bd638b83f960fbeb3fa98fa38cb355bc0a32
libGLESv2.dll SHA-256 36079cbb929bb529458d392c35a19cd0852575b818954dd580a996f34c146cf6
```

```text
is_component_build = false
target_cpu = "x64"
is_debug = false
angle_assert_always_on = false
clang_use_chrome_plugins = false
angle_enable_null = false
angle_enable_d3d11 = true
angle_enable_d3d9 = false
angle_enable_gl = false
angle_enable_vulkan = true
angle_shared_libvulkan = false
angle_enable_wgpu = false
```

To reproduce the pair, use the pinned ANGLE checkout with Chromium `depot_tools`,
Visual Studio C++ tools, and a Windows SDK. Sync ANGLE dependencies at the
pinned commit, apply the patch above from the ANGLE root, generate the
`release-windows-x64` GN directory with the listed arguments, then build
`libEGL` and `libGLESv2` with Ninja. Copy those two outputs to the resource
directory above. The system Vulkan loader and a Vulkan-capable graphics driver
must be installed on the target machine.

## Validation

Run the deterministic backend check on Windows x86_64:

```powershell
.\gradlew.bat test rendererCompare
```

`rendererCompare` renders a red cube and OpenKeeper's flat terrain, vertex-lit
terrain, creature lighting, unit flower, progress indicator, and movie
materials into an explicit RGBA8 offscreen target. It checks the center pixel,
checks ANGLE's reported renderer, and requires exact parity for that capture.
The same application also runs the game's SSAO and translucent-bucket
processors on the visible main viewport. The offscreen comparison is taken
before those filters because jME routes the filter output to the window
surface.

On the validated RTX 2060 SUPER with NVIDIA driver 617.42, both backends
reported the same capture SHA-256
`52164a111cb504dd33b8f213e3bb9dbeb7110f3d52445cd394e2af9e556a0fb6`; the
comparison found 0 of 307,200 pixels different. In the same run, host frame
intervals averaged 6.944 ms on both backends; p95 was 7.110 ms for OpenGL and
7.125 ms for Vulkan. These intervals include driver scheduling and are not GPU
timestamp measurements, so they do not establish a relative GPU performance
result.

## Remaining validation

- The complete game could not be launched because this environment has no
  original Dungeon Keeper II installation. Full-scene material, gamma
  correction, actual video playback, and gameplay parity remain to be checked
  with those assets. Shader compilation and material rendering for the terrain,
  creature lighting, unit flower, progress indicator, and movie materials, plus
  SSAO/translucency processing, are covered by the smoke scene.
- Vulkan has been packaged and exercised only on Windows x86_64. Linux, macOS,
  Windows ARM64, other GPU vendors, and other driver versions are not yet
  validated and do not expose the Vulkan menu option.
- Performance work still needs GPU timestamp measurements and representative
  gameplay captures against OpenGL.
