/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import java.util.List;

/** Scene-retirement operations; never called per creature or during render submission. */
public final class RenderQueueLifecycle {
    private static final RenderQueue.Bucket[] BUCKETS = {RenderQueue.Bucket.Opaque, RenderQueue.Bucket.Transparent,
        RenderQueue.Bucket.Translucent, RenderQueue.Bucket.Sky, RenderQueue.Bucket.Gui};

    private RenderQueueLifecycle() { }

    /**
     * Release empty queues' retained sorting scratch after scene roots retire, before culling.
     * jME 3.9 GeometryList.clear() leaves ListSort's merge scratch referencing old geometries.
     * The public comparator setter creates fresh list storage while retaining sorting policy.
     * Nonempty buckets belong to pending work and must not be discarded. Processor-private
     * GeometryLists remain the processor's lifecycle responsibility.
     */
    public static void releaseEmptySortStorage(RenderManager renderer) {
        if (renderer == null) return; // Detach before renderer/application initialization.
        release(renderer.getPreViews());
        release(renderer.getMainViews());
        release(renderer.getPostViews());
    }

    private static void release(List<ViewPort> views) {
        for (ViewPort view : views) {
            RenderQueue queue = view.getQueue();
            for (RenderQueue.Bucket bucket : BUCKETS) if (queue.isQueueEmpty(bucket))
                queue.setGeometryComparator(bucket, queue.getGeometryComparator(bucket));
        }
    }
}
