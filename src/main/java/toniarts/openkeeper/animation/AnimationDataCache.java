/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.scene.Spatial;
import java.util.WeakHashMap;

/**
 * Optional asset-context-owned immutable packed source data. Owners retain their own mutable output.
 * The limit counts packed primitive payload, not scene/source/direct/GPU memory. Weak entries account
 * for data while templates or live clones retain it; eviction never invalidates a live owner.
 */
public final class AnimationDataCache {
    public static final int MAX_BYTES=256*1024*1024;
    private final int limit;
    private final WeakHashMap<VertexClip,Boolean> data=new WeakHashMap<>();

    public AnimationDataCache(int limit) {
        if (limit<0 || limit>MAX_BYTES) throw new IllegalArgumentException("Invalid packed primitive-data budget");
        this.limit=limit;
    }
    public int getLimit() { return limit; }
    public synchronized long getResidentPrimitiveBytes() {
        return data.keySet().stream().mapToLong(VertexClip::decodedBytes).sum();
    }
    /** Prepare a private raw template before publishing/cloning it; never call on a live scene. */
    public synchronized void prepare(Spatial template) {
        if (limit==0) return;
        template.depthFirstTraversal(node->{
            var composer=node.getControl(AnimComposer.class);
            if (composer==null) return;
            for (var clip : composer.getAnimClips()) for (var track : clip.getTracks()) if (track instanceof PoseTrack pose) {
                if (pose.packedData()!=null) {
                    if (!data.containsKey(pose.packedData()) && pose.packedData().decodedBytes()>limit-getResidentPrimitiveBytes())
                        pose.discardPackedData();
                    else data.put(pose.packedData(),Boolean.TRUE);
                    continue;
                }
                if (pose.getTimes().length>limit/4) continue; // Bound arithmetic before multiplying dimensions.
                long required=4L*pose.getTimes().length*(1+3L*pose.getTarget().getMesh().getVertexCount());
                long available=limit-getResidentPrimitiveBytes();
                if (required>available) continue; // Retain sparse compatibility sampling; no partial allocation.
                pose.preparePackedData((int)available); data.put(pose.packedData(),Boolean.TRUE);
            }
        });
    }
}
