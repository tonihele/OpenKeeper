/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.util.clone.Cloner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.ArtResource;

/** Owner-local LRU. Never shares composers, callbacks or mutable meshes between owners. */
public final class PreparedAnimationCache extends AbstractControl {
    private record Key(String name, ArtResource.ArtResourceType type, Set<ArtResource.ArtResourceFlag> flags, Map<String,Number> data) {
        static Key of(ArtResource resource) {
            Map<String,Number> data = new java.util.HashMap<>();
            for (String key : resource.getDataKeys()) data.put(key,resource.getData(key));
            return new Key(resource.getName(),resource.getType(),Set.copyOf(resource.getFlags()),Map.copyOf(data));
        }
    }
    private record Entry(List<Spatial> children, long bytes) {}
    private final int capacity;
    private final long budget;
    private Map<Key,Entry> entries = new LinkedHashMap<>(4,.75f,true);
    private long residentBytes;

    /** Serialization imports an empty, disabled cache; runtime instances are never persisted. */
    public PreparedAnimationCache() { this(0,0); }

    public PreparedAnimationCache(int capacity, long budget) {
        if (capacity < 0 || capacity > 32 || budget < 0) throw new IllegalArgumentException("Invalid animation cache budget");
        this.capacity = capacity; this.budget = budget;
    }

    List<Spatial> get(ArtResource resource) {
        if (capacity == 0) return null;
        Entry entry = entries.get(Key.of(resource));
        return entry == null ? null : entry.children;
    }

    void put(ArtResource resource, List<Spatial> children) {
        if (capacity == 0) return;
        long[] bytes = {0};
        for (Spatial child : children) child.depthFirstTraversal(node -> {
            if (node instanceof Geometry geometry) {
                var buffer = geometry.getMesh().getBuffer(VertexBuffer.Type.Position);
                if (buffer != null) bytes[0] += buffer.getData().limit() * 12L;
            }
        });
        if (bytes[0] > budget) return;
        Key key = Key.of(resource); Entry old = entries.remove(key);
        if (old != null) residentBytes -= old.bytes;
        entries.put(key,new Entry(List.copyOf(children),bytes[0])); residentBytes += bytes[0];
        while(entries.size() > capacity || residentBytes > budget) {
            var iterator = entries.entrySet().iterator();
            residentBytes -= iterator.next().getValue().bytes; iterator.remove();
        }
    }

    public int size() { return entries.size(); }
    void remove(ArtResource resource) {
        Entry removed = entries.remove(Key.of(resource));
        if (removed != null) residentBytes -= removed.bytes;
    }
    /** Owned position output plus two decoded arrays; other objects are bounded by clip count. */
    public long getResidentPositionBytes() { return residentBytes; }
    public void clear() { entries.clear(); residentBytes = 0; }
    @Override protected void controlUpdate(float tpf) {}
    @Override protected void controlRender(RenderManager rm, ViewPort vp) {}
    @Override public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner,original);
        entries = new LinkedHashMap<>(4,.75f,true); residentBytes = 0;
    }
}
