/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.export.*;
import com.jme3.material.Material;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.function.DoubleSupplier;

/** Selects an owned material's atlas frame on the pause-aware presentation clock. */
public final class TextureAnimationControl extends AbstractControl {
    private static final DoubleSupplier NO_CLOCK = () -> Double.NaN;
    private transient DoubleSupplier clock = NO_CLOCK;
    private transient Material previousMaterial;
    private transient int previousFrame = -1;
    private double framesPerSecond = 30;
    private double elapsed;

    public static void install(Spatial model, DoubleSupplier clock, double fps) {
        model.depthFirstTraversal(node -> {
            if (!(node instanceof Geometry geometry)) return;
            Material material = geometry.getMaterial();
            var tiles = material.getParam("NumberOfTiles");
            if (tiles == null || ((Number)tiles.getValue()).intValue() <= 1
                    || material.getMaterialDef().getMaterialParam("UseAnimationFrame") == null) return;
            var control = geometry.getControl(TextureAnimationControl.class);
            if (control == null) { control = new TextureAnimationControl(); geometry.addControl(control); }
            control.clock = clock;
            control.framesPerSecond = Double.isFinite(fps) && fps > 0 ? fps : 30;
            control.controlUpdate(0);
        });
    }

    @Override protected void controlUpdate(float tpf) {
        if (!(spatial instanceof Geometry geometry) || AnimationLoader.isPaused(spatial)) return;
        Material material = geometry.getMaterial();
        var tilesParam = material.getParam("NumberOfTiles");
        if (tilesParam == null || material.getMaterialDef().getMaterialParam("UseAnimationFrame") == null) return;
        int tiles = ((Number)tilesParam.getValue()).intValue();
        if (tiles <= 0) return;
        if (material != previousMaterial) { previousMaterial = material; previousFrame = -1; material.setBoolean("UseAnimationFrame",true); }
        double time = clock.getAsDouble();
        if (!Double.isFinite(time)) {
            if (Float.isFinite(tpf) && tpf > 0) elapsed += tpf;
            time = elapsed;
        } else elapsed = Math.max(0,time);
        int frame = Math.floorMod((long)Math.floor(Math.max(0,time)*framesPerSecond+1e-6),tiles);
        if (frame != previousFrame) { material.setInt("AnimationFrame",frame); previousFrame = frame; }
    }

    @Override protected void controlRender(RenderManager rm, ViewPort vp) {}
    @Override public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner,original); clock = NO_CLOCK; previousMaterial = null; previousFrame = -1;
    }
    @Override public void write(JmeExporter exporter) throws IOException {
        super.write(exporter); var out = exporter.getCapsule(this);
        out.write(framesPerSecond,"framesPerSecond",30); out.write(elapsed,"elapsed",0);
    }
    @Override public void read(JmeImporter importer) throws IOException {
        super.read(importer); var in = importer.getCapsule(this);
        framesPerSecond = in.readDouble("framesPerSecond",30); elapsed = in.readDouble("elapsed",0);
        if (!Double.isFinite(framesPerSecond) || framesPerSecond <= 0) framesPerSecond = 30;
        if (!Double.isFinite(elapsed) || elapsed < 0) elapsed = 0;
    }
}
