/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view;

import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderQueueLifecycleTest {
    private static GeometryList opaque(RenderQueue queue) throws Exception {
        var field = RenderQueue.class.getDeclaredField("opaqueList"); field.setAccessible(true);
        return (GeometryList) field.get(queue);
    }
    private static GeometryComparator comparator() {
        return new GeometryComparator() {
            @Override public void setCamera(Camera camera) { }
            @Override public int compare(Geometry a, Geometry b) { return a.getName().compareTo(b.getName()); }
        };
    }

    @Test void emptyPreMainAndPostQueuesDropPinnedEnginesRetainedWorldWithoutChangingSortPolicy() throws Exception {
        var renderer = new RenderManager(null);
        var views = List.of(renderer.createPreView("pre", new Camera(32, 32)),
                renderer.createMainView("main", new Camera(32, 32)), renderer.createPostView("post", new Camera(32, 32)));
        for (var view : views) {
            var queue = view.getQueue(); var policy = comparator();
            queue.setGeometryComparator(RenderQueue.Bucket.Opaque, policy);
            var old = opaque(queue); var world = new Node("retired-world");
            for (int i = 0; i < 128; i++) {
                var geometry = new Geometry(String.format("%03d", i * 73 % 128));
                world.attachChild(geometry); queue.addToQueue(geometry, RenderQueue.Bucket.Opaque);
            }
            old.sort(); old.clear();
            // Pin-specific regression evidence. Production uses only Renderer/RenderQueue public APIs.
            var sorterField = GeometryList.class.getDeclaredField("listSort"); sorterField.setAccessible(true);
            var sorter = sorterField.get(old);
            var scratchField = sorter.getClass().getDeclaredField("tmpArray"); scratchField.setAccessible(true);
            var scratch = (Object[]) scratchField.get(sorter);
            assertTrue(Arrays.stream(scratch).anyMatch(g -> g instanceof Geometry geometry && geometry.getParent() == world),
                    "Requalify this lifecycle operation if the pinned engine no longer retains sorting scratch");
            RenderQueueLifecycle.releaseEmptySortStorage(renderer);
            assertNotSame(old, opaque(queue));
            assertSame(policy, queue.getGeometryComparator(RenderQueue.Bucket.Opaque));
            assertTrue(queue.isQueueEmpty(RenderQueue.Bucket.Opaque));
        }
    }

    @Test void pendingDrawWorkIsPreservedAndAnUninitializedRendererNeedsNoCleanup() throws Exception {
        var renderer = new RenderManager(null);
        var view = renderer.createMainView("pending", new Camera(32, 32));
        var queue = view.getQueue(); var old = opaque(queue);
        var geometry = new Geometry("pending"); queue.addToQueue(geometry, RenderQueue.Bucket.Opaque);
        RenderQueueLifecycle.releaseEmptySortStorage(renderer);
        assertSame(old, opaque(queue)); assertSame(geometry, old.get(0));
        RenderQueueLifecycle.releaseEmptySortStorage(null);
    }
}
