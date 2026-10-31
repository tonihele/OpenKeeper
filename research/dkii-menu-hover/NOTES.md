# DK II in-game menu icon hover rendering

Reference: installed DKII.exe, SHA-256 `2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163`. Findings are from static inspection of the reference executable, following the existing IDA names/vtables, with a pixel comparison of the user's native screenshots. No runtime blend-state capture was performed for this investigation.

## Result

The icon is not made more opaque on hover. It uses the same graphic at full white RGBA modulation, then adds a second pass of that graphic when hovered. The extra pass uses additive ONE/ONE blending. This explains the approximately doubled RGB and white clipping in the screenshots.

## Native call path

- `0x00528430` performs the button rectangle hit test and sets button `+0x49` to 1/0. The GUI update/draw dispatchers are `0x0052BC50` and `0x0052BBD0`.
- `0x0042B7C0` draws the menu icon. It selects the same graphic through button `+0x34` and interface graphic bank `+0x12B6` (record stride 0x1B). At `0x0042B8C0–0x0042B8E2`, its modulation is set to RGBA 255/255/255/255.
- At `0x0042B8E9–0x0042B900`, an enabled button (state `+0x61 == 1`) with hover `+0x49 != 0` and its highlight option `+0x38 != 0` adds descriptor flag 0x08 at descriptor `+0x4C`. No alternate hover asset or alpha reduction is selected here.
- The draw goes through CBridge vtable slot +0x5C (`0x00440FF0`), forwarding to CPCEngineInterface slot +0x5C (`0x0059B0C0`). RTTI/vtable identities were checked against the existing IDA symbol catalogue and executable RTTI.
- `0x0059B0C0` maps descriptor flag 0x08 to sprite-submission flag 0x40 and preserves the same descriptor alpha (`+0x4B`, 255).
- `0x00579180` submits the sprite normally. With flag 0x40, it loops for one additional submission: it clears 0x40, sets flag 0x02, and slightly adjusts depth. It does not change texture or RGBA between the two submissions.
- `0x0057F090` maps flag 0x02 to render flags 0x201, clearing bits 0xE0. In render-state setup `0x00589250`, the bit-0 branch at `0x005893F4` sets D3DRS_SRCBLEND (19) = 2 (ONE), D3DRS_DESTBLEND (20) = 2 (ONE), and D3DRS_ALPHABLENDENABLE (27) = 1. Thus the added pass adds source RGB to destination RGB.

## Screenshot cross-check

Compare the native Load icon in `Schermafdruk van 2026-10-29 01-20-15.png` (rest) with `01-20-12.png` (hover). Example RGB transitions inside the icon:

- (16, 32, 16) -> (33, 65, 33)
- (0, 16, 0) -> (0, 32, 0)
- (173, 170, 173) -> (255, 255, 255)
- (0, 0, 0) -> (0, 0, 0)

The small deviations from exact doubling are consistent with framebuffer quantization. All 528 pixels of the tested interior region (9,209)-(33,231) stay identical between two resting screenshots while the nearby terrain changes, independently arguing against variable map transparency.

## OpenKeeper implication

The existing `iconText` hover effect in MyControls.xml overlays `Textures/GUI/Icons/hilight.png` and makes the lettering white. That frame overlay does not itself reproduce DK II's second pass of the actual icon image. Matching the reference requires brightening the original icon contents with additive drawing, leaving alpha unchanged; do not implement this as reduced resting opacity.


## OpenKeeper implementation and GPU verification

`MenuIconHighlightEffect` sets the icon's own NiftyImage color to 2x RGB,
with its existing alpha, and restores the original image color on deactivation.
For the opaque RGB menu icons, this is equivalent to DK II's normal draw plus
ONE/ONE pass. It is deliberately not a general additive effect for transparent
images. The old unrelated frame-overlay texture is no longer used for iconText.

The public Nifty BlendMode enum has only BLEND and MULIPLY; jME 3.9's backend maps
both to Alpha. Changing that enum would not request native ONE/ONE blending.
Also, NiftyRenderEngineImpl.renderImage reads RGB from NiftyImage.getColor and
only takes the inherited alpha from render state: changing the renderer's RGB
alone had no visible effect. The first GPU probe caught that failure before the
image-level correction.

Run the production-control integration probe with Java 25 after installDist:

```
/home/wietse/.jdks/ms-25.0.4/bin/java -cp 'build/install/OpenKeeper/lib/*' research/dkii-menu-hover/MenuIconHoverRenderProbe.java
```

The probe uses the actual MyControls.xml iconText, controller startHover/endHover,
production effect, jME Nifty batch renderer with the production atlas settings,
and an offscreen OpenGL surface. Gamma correction is disabled as in production
Settings. Framebuffer assertions passed for 2x RGB, saturation, opaque black,
an unchanged neighboring control sharing the same atlas image, and exact
restoration after ending hover. It also renders the original converted Load icon.
Screenshots are written to /tmp/openkeeper-menu-hover-gpu/{normal,hover,restored}.png.

The probe also asserts the 2x RGB rule for every pixel/channel of the actual
64x64 Load icon, not just the calibration colors. This passed.

Application compileJava/installDist and git diff --check passed. The full test
suite was attempted but compileTestJava is blocked by the pre-existing
PortalSpawnCooldownTest.java:107 fixture calling CreatureSpawnSystem with the
old constructor (missing EntityData and GameEventService). That unrelated
fixture was left unchanged.
