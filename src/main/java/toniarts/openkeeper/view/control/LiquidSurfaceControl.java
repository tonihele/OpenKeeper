/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;
import java.util.Optional;
import java.util.Set;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.view.map.Dk2EmbmResources;

/**
 * Applies a graphics-mode change to one liquid material without rebuilding
 * the map mesh or duplicating its lighting registration.
 */
public final class LiquidSurfaceControl extends AbstractControl {

    private static final System.Logger LOG = System.getLogger(LiquidSurfaceControl.class.getName());

    private final boolean water;
    private final boolean allowEnhanced;
    private final AssetManager assetManager;
    private final Mesh enhancedWaterMesh;
    private Material material;
    private Mesh classicMesh;
    private Texture classicDiffuse;
    private Integer classicFrameCount;
    private Integer classicFrameRate;
    private Boolean classicWavesEnabled;
    private Boolean lastRequest;
    private Boolean textureArraysSupported;
    private boolean enhancedActive;

    public LiquidSurfaceControl(boolean water, boolean allowEnhanced, AssetManager assetManager) {
        this(water, allowEnhanced, assetManager, null);
    }

    public LiquidSurfaceControl(boolean water, boolean allowEnhanced, AssetManager assetManager,
            Mesh enhancedWaterMesh) {
        this.water = water;
        this.allowEnhanced = allowEnhanced;
        this.assetManager = assetManager;
        this.enhancedWaterMesh = enhancedWaterMesh;
    }

    @Override
    @SuppressWarnings("deprecation") // Required by jME's Control contract.
    public Control cloneForSpatial(Spatial spatial) {
        return new LiquidSurfaceControl(water, allowEnhanced, assetManager, enhancedWaterMesh);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!(spatial instanceof Geometry geometry)) {
            return;
        }
        Material current = geometry.getMaterial();
        if (current == null) {
            return;
        }
        if (material != current) {
            captureClassicMaterial(current, geometry);
        }

        boolean settingEnabled = allowEnhanced
                && Settings.getInstance().getBoolean(Settings.Setting.ENHANCED_LIQUIDS);
        // Renderer capabilities are first available on the render thread. Let
        // one Classic frame through rather than risk compiling an unsupported
        // sampler2DArray shader before we know the active backend.
        if (settingEnabled && textureArraysSupported == null) {
            return;
        }
        boolean requested = settingEnabled && Boolean.TRUE.equals(textureArraysSupported);
        if (lastRequest != null && lastRequest == requested) {
            return;
        }
        lastRequest = requested;
        if (requested && enableEnhanced(current, geometry)) {
            enhancedActive = true;
            return;
        }

        disableEnhanced(current, geometry);
        enhancedActive = false;
    }

    private void captureClassicMaterial(Material current, Geometry geometry) {
        material = current;
        if (!enhancedActive) {
            classicMesh = geometry.getMesh();
        }
        classicDiffuse = current.getTextureParam("DiffuseMap") == null
                ? null : current.getTextureParam("DiffuseMap").getTextureValue();
        classicFrameCount = integerParameter(current, "NumberOfTiles");
        classicFrameRate = integerParameter(current, "Speed");
        classicWavesEnabled = current.getParam("LiquidWavesEnabled") == null
                ? null : (Boolean) current.getParam("LiquidWavesEnabled").getValue();
        lastRequest = null;
        enhancedActive = false;
    }

    private static Integer integerParameter(Material material, String name) {
        return material.getParam(name) == null ? null : (Integer) material.getParam(name).getValue();
    }

    private boolean enableEnhanced(Material current, Geometry geometry) {
        Optional<Dk2EmbmResources> resources = Dk2EmbmResources.load(assetManager,
                water ? Dk2EmbmResources.Liquid.WATER : Dk2EmbmResources.Liquid.LAVA);
        if (resources.isEmpty()) {
            return false;
        }
        try {
            resources.get().apply(current);
            if (water && enhancedWaterMesh != null) {
                current.setBoolean("LiquidWavesEnabled", true);
                geometry.setMesh(enhancedWaterMesh);
            }
            return true;
        } catch (RuntimeException ex) {
            LOG.log(System.Logger.Level.WARNING,
                    "Enhanced DKII liquid material could not be enabled; keeping Classic rendering.", ex);
            return false;
        }
    }

    private void disableEnhanced(Material current, Geometry geometry) {
        if (classicDiffuse != null) {
            current.setTexture("DiffuseMap", classicDiffuse);
        }
        if (classicFrameCount != null) {
            current.setInt("NumberOfTiles", classicFrameCount);
        }
        if (classicFrameRate != null) {
            current.setInt("Speed", classicFrameRate);
        }
        if (classicWavesEnabled == null) {
            current.clearParam("LiquidWavesEnabled");
        } else {
            current.setBoolean("LiquidWavesEnabled", classicWavesEnabled);
        }
        if (water && enhancedWaterMesh != null && classicMesh != null) {
            geometry.setMesh(classicMesh);
        }
        current.clearParam("EmbmEnabled");
        current.clearParam("EmbmBaseMap");
        current.clearParam("EmbmBumpMap");
        current.clearParam("EmbmBumpFrames");
        current.clearParam("EmbmEnvironmentMap");
        current.clearParam("EmbmBumpMatrix");
        current.clearParam("EmbmReflectionIntensity");
        current.clearParam("EmbmBumpAnimationSpeed");
        current.clearParam("EmbmBumpFrameCount");
        current.clearParam("EmbmLuminanceScale");
        current.clearParam("EmbmLuminanceOffset");
        current.clearParam("EmbmBaseAlpha");
        current.clearParam("EmbmBaseColorMix");
        current.clearParam("EmbmUseLuminance");
        current.clearParam("EmbmUseSphereEnvironmentUv");
    }

    public boolean isEnhancedActive() {
        return enhancedActive;
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager renderManager,
            com.jme3.renderer.ViewPort viewPort) {
        captureRendererCapabilities(renderManager.getRenderer().getCaps());
    }

    void captureRendererCapabilities(Set<Caps> rendererCaps) {
        if (textureArraysSupported == null) {
            textureArraysSupported = rendererCaps.contains(Caps.TextureArray);
            if (!textureArraysSupported) {
                LOG.log(System.Logger.Level.WARNING,
                        "Enhanced DKII liquids require texture-array support; using Classic rendering.");
            }
        }
    }
}
