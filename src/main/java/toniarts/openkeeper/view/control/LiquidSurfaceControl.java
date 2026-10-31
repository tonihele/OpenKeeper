/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;
import java.nio.FloatBuffer;
import java.util.function.BooleanSupplier;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.view.map.LiquidTextures;
import toniarts.openkeeper.view.map.LiquidWaves;

/** Switches liquid materials in place, retaining the map's shared light bindings. */
public final class LiquidSurfaceControl extends AbstractControl {
    private final boolean water;
    private final AssetManager assets;
    private final BooleanSupplier enhancedSetting;
    private Mesh mesh;
    private Texture classicTexture;
    private Integer classicFrames;
    private Boolean requested;
    private boolean enhanced;
    private final Vector3f normal = new Vector3f();

    public LiquidSurfaceControl(boolean water, AssetManager assets) {
        this(water, assets, () -> Main.getUserSettings().getBoolean(Settings.Setting.ENHANCED_LIQUIDS));
    }

    /** Allows independent Classic/Enhanced previews without changing the user's setting. */
    public LiquidSurfaceControl(boolean water, AssetManager assets, BooleanSupplier enhancedSetting) {
        this.water = water;
        this.assets = assets;
        this.enhancedSetting = java.util.Objects.requireNonNull(enhancedSetting);
    }

    @Override public Control cloneForSpatial(Spatial spatial) { return new LiquidSurfaceControl(water, assets, enhancedSetting); }

    @Override protected void controlUpdate(float tpf) {
        if (!(spatial instanceof Geometry geometry)) return;
        Material material = geometry.getMaterial();
        if (mesh == null) {
            mesh = geometry.getMesh();
            mesh.getBuffer(VertexBuffer.Type.Position).setUsage(VertexBuffer.Usage.Dynamic);
            mesh.getBuffer(VertexBuffer.Type.Normal).setUsage(VertexBuffer.Usage.Dynamic);
            classicTexture = material.getTextureParam("DiffuseMap").getTextureValue();
            classicFrames = material.getParam("NumberOfTiles") == null ? null : (Integer) material.getParam("NumberOfTiles").getValue();
        }
        boolean enabled = enhancedSetting.getAsBoolean();
        boolean modeChanged = requested == null || requested != enabled;
        if (modeChanged) {
            requested = enabled;
            var maps = enabled ? LiquidTextures.load(assets, water) : java.util.Optional.<LiquidTextures.Maps>empty();
            enhanced = maps.isPresent();
            material.setBoolean("Enhanced", enhanced);
            if (enhanced) {
                var textures = maps.get();
                material.setTexture("DiffuseMap", textures.base());
                material.setTexture("BumpMap", textures.bump());
                material.setTexture("EnvironmentMap", textures.environment());
                material.setInt("BumpFrames", textures.frames());
                material.clearParam("NumberOfTiles");
            } else {
                material.setTexture("DiffuseMap", classicTexture);
                if (classicFrames != null) material.setInt("NumberOfTiles", classicFrames);
                material.clearParam("BumpMap");
                material.clearParam("EnvironmentMap");
            }
        }
        if (water && !enhanced && !modeChanged) return;
        double tick = LiquidWaves.tick();
        FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        FloatBuffer normals = mesh.getFloatBuffer(VertexBuffer.Type.Normal);
        for (int i = 0; i < positions.limit(); i += 3) {
            // Native map corners are integer coordinates; OK centres tiles on integers.
            double x = positions.get(i) + .5, z = positions.get(i + 2) + .5;
            boolean waves = !water || enhanced;
            positions.put(i + 1, waves ? LiquidWaves.height(x, z, tick, water) : LiquidWaves.LEVEL);
            if (waves) LiquidWaves.normal(x, z, tick, water, normal); else normal.set(0, 1, 0);
            normals.put(i, normal.x).put(i + 1, normal.y).put(i + 2, normal.z);
        }
        mesh.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
        mesh.getBuffer(VertexBuffer.Type.Normal).setUpdateNeeded();
        mesh.updateBound();
    }

    @Override protected void controlRender(com.jme3.renderer.RenderManager manager, com.jme3.renderer.ViewPort view) { }
}
