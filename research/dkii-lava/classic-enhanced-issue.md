DKII provides ordinary and enhanced liquid rendering for **both water and lava**. Add a graphics option to choose Classic or Enhanced, using the original assets and preserving the existing lighting and fog behaviour.

Related: #22 proposes improving lava appearance/bloom. The native enhanced mode is environment-mapped bump mapping (EMBM), which is a separate effect from bloom.

### Native findings

Research against the original executable (SHA-256 `2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163`) established:

- Ordinary lava uses CPU-generated heights **and changing normals**, with a 3×3 vertex grid per tile. Its height uses two added sine waves, each with amplitude 0.04. Across eight live frames, all 72 sampled heights and normal vectors matched the reconstructed calculation within 1e-7.
- Enhanced rendering uses three fixed-function Direct3D stages: base texture × diffuse lighting; bump-map distortion; additive environment-map highlight. The luminance variant changes the bump operation. Six isolated original-code setup, restoration and caching cases passed against a recording device.
- Lava has one base/bump/environment-map texture triple. Water has its own triple, including 32 bump-map frames.
- Launching the native reference with `-enablebumpmapping` enabled mode 1, passed its capability check and loaded all three enhanced lava resources. The saved registry value alone did not activate the feature in the tested build. Enhanced GPU pixel output has not yet been verified.
- OpenKeeper's current lava control uses a different wave formula, one quad per tile, and leaves the normals vertical. Its liquid material does not currently implement the native enhanced stages.

The native patch notes document EMBM for water and lava on the Matrox G400. Modern shaders can implement the effect without requiring that hardware.

### Proposed scope / acceptance criteria

- [ ] Add a persisted **Classic / Enhanced liquids** graphics option, defaulting to Classic.
- [ ] Support both water and lava, retaining their distinct textures and animation.
- [ ] Reproduce the verified native lava mesh density, displacement and changing normals.
- [ ] Implement the enhanced texture-stage combination in a dedicated liquid shader.
- [ ] Load the original enhanced assets through the asset pipeline; fall back to Classic with a useful diagnostic if required assets are missing.
- [ ] Preserve terrain lighting, fog-of-war visibility, transparency and tile boundaries in both modes.
- [ ] Allow comparisons in the model viewer and validate shader compilation/rendering for both liquids and modes.
- [ ] Check bump-channel encoding, environment-map coordinates and animation against native behaviour before claiming pixel parity.

Bloom remains a separate enhancement. Research captures, a controlled map and reproducible original-code harnesses are prepared locally under `research/dkii-lava/`; they are not yet in an upstream commit. No original executable or commercial texture data is included here.
