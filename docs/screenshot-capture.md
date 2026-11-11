# Headless map screenshot capture

Render repeatable map views without starting an interactive game or opening a visible window. The standalone capture app loads the map through the same 3D map-view code used by the model viewer, applies configured camera poses, and writes PNG files. It does not load game HUD screens, play audio, or use gameplay controls.

```powershell
.\gradlew.bat captureScreenshots `
  -PmapFile="C:/path/to/level.kwd" `
  -PcaptureConfig="D:/path/to/screenshot-capture.properties" `
  -PdkFolder="C:/GOG Games/Dungeon Keeper 2"
```

The renderer uses an offscreen OpenGL context, so a compatible graphics driver is still required; no interactive game process or visible window is needed. The Gradle task uses `build/headless-screenshot-capture` as its working directory so generated settings and converted assets stay out of the source tree.

`dkFolder` points to the original DK II install and supplies the map's companion KLD data. If converted assets are missing, the task converts them into its isolated working directory. For repeated captures, also pass `-PassetsDir="D:/path/to/Converted"` to reuse an existing converted asset folder. For maps stored outside the usual `Data/editor/maps` tree, pass `-PmapRoot="D:/path/to/data-root"` so linked KLD paths resolve correctly.

Copy [screenshot-capture.properties.example](screenshot-capture.properties.example) and add one `shot.<id>.*` group for each view. `position` and `look-at` use OpenKeeper world coordinates. Set either `look-at` or `rotation-degrees` (yaw, pitch, roll). Each shot can set vertical field of view, settle frames, and `ceiling-visible=true|false`; use `false` for keeper-map views and `true` for creature-perspective views. Relative output directories resolve from the properties file. `startup-frames` lets a large map finish loading before the first shot. The tool refuses to overwrite any configured PNG. All shots in one run share one map; use another invocation and manifest for a different map.

`ceiling-subdivisions` controls ceiling grid cells per tile edge. The supported range is 2 to 16: use 4 for normal detail, 8 for high detail, and 16 for an extreme close-up. The game graphics setting `CEILING_SUBDIVISIONS` controls normal map rendering; every patch shares the selected count so adjoining tile edges stay aligned. `near-clip` defaults to the possession camera's 0.01 world units, keeping close models from disappearing at the camera plane.
