/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.control;

import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import java.util.ArrayList;
import java.util.List;

/** Per-instance opacity; shared cached materials and authored blend modes stay intact. */
public final class SpatialOpacity implements AutoCloseable {
    private final Spatial spatial;
    private final MatParamOverride opacity = new MatParamOverride(VarType.Float, "Opacity", 1f);
    private final List<Surface> surfaces = new ArrayList<>();
    private float value = 1;

    private record Surface(Geometry geometry, Material original, Material fading,
            RenderQueue.Bucket bucket) { }

    public SpatialOpacity(Spatial spatial) {
        this.spatial = spatial;
        spatial.addMatParamOverride(opacity);
        spatial.depthFirstTraversal(child -> {
            if (child instanceof Geometry geometry && geometry.getMaterial() != null) {
                Material original = geometry.getMaterial();
                if (original.getMaterialDef().getMaterialParam("Opacity") == null) {
                    return;
                }
                Material fading = original.clone();
                if (fading.getAdditionalRenderState().getBlendMode() == RenderState.BlendMode.Off) {
                    fading.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                    fading.getAdditionalRenderState().setDepthWrite(false);
                }
                surfaces.add(new Surface(geometry, original, fading, geometry.getLocalQueueBucket()));
            }
        });
    }

    public void set(float value) {
        value = Math.clamp(value, 0, 1);
        if (this.value == value) {
            return;
        }
        boolean fading = value < 1;
        if (fading != (this.value < 1)) {
            for (Surface surface : surfaces) {
                surface.geometry.setMaterial(fading ? surface.fading : surface.original);
                surface.geometry.setQueueBucket(fading ? RenderQueue.Bucket.Transparent : surface.bucket);
            }
        }
        opacity.setValue(value);
        this.value = value;
    }

    @Override
    public void close() {
        set(1);
        spatial.removeMatParamOverride(opacity);
    }
}
