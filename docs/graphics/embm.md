# DKII liquid environment mapping

OpenKeeper's **Enhanced liquids (EMBM)** graphics option is enabled for new settings and can be changed from the main menu or pause-menu graphics settings. The pause-menu change is applied immediately and saved for later sessions.

The effect is limited to water and lava terrain surfaces. It reuses DKII's liquid base, bump, and environment textures when those resources are available. Water animates through its original bump-frame sequence; lava scrolls its bump field and retains the existing small sine-wave motion, evaluated in the vertex shader. The fragment shader uses the bump field to perturb the environment-map lookup, following the G400-era EMBM approach.

This is an environment-map effect, not a live planar reflection of the dungeon. It does not change creatures, rooms, walls, or other solid materials. If required DKII liquid textures are missing, OpenKeeper keeps the existing classic liquid material. Turning the option off also uses classic materials.
