/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.view.lighting.LightRules;
import toniarts.openkeeper.view.lighting.SceneLights.MaterialBinding;

/**
 * Render-thread-owned material lifecycle. Registrations, queues and material
 * references belong to the view, never to cached/exported model assets.
 * Call update at the owner's normal lighting phase, and flush before submission
 * to include material replacements made by scene controls later in the frame.
 */
public final class EntityMaterialSetup implements AutoCloseable {
    private final MaterialProvider provider;
    private final boolean cached;
    private final IdentityHashMap<Spatial, Entry> entries = new IdentityHashMap<>();
    private final List<Entry> active = new ArrayList<>();
    private final ArrayDeque<Entry> pending = new ArrayDeque<>();
    private final List<Entry> waiting = new ArrayList<>();
    private MaterialBinding binding;
    private boolean closed;
    private Thread ownerThread;

    public EntityMaterialSetup(AssetManager assets) {
        this(new JmeMaterialProvider(assets),
                Boolean.parseBoolean(System.getProperty("openkeeper.material.cacheVertexLightingSetup", "true")));
    }

    public EntityMaterialSetup(MaterialProvider provider, boolean cached) {
        this.provider = Objects.requireNonNull(provider);
        this.cached = cached;
    }

    /** Registers once; changing an existing registration's policy schedules preparation. */
    public void register(Spatial root, MaterialSetupPolicy policy) {
        checkThread(false);
        if (closed) {
            throw new IllegalStateException("Material setup is closed");
        }
        Objects.requireNonNull(root);
        Objects.requireNonNull(policy);
        Entry entry = entries.get(root);
        if (entry == null) {
            InvalidationControl control = root.getControl(InvalidationControl.class);
            if (control == null) {
                control = new InvalidationControl();
                root.addControl(control);
            } else if (control.entry != null) {
                throw new IllegalStateException("Model already belongs to a material setup owner");
            }
            entry = new Entry(root, policy, control);
            control.entry = entry;
            entries.put(root, entry);
            active.add(entry);
            queue(entry);
            materialsChanged(root.getParent());
        } else if (!entry.policy.equals(policy)) {
            entry.policy = policy;
            waiting.remove(entry);
            entry.waiting = false;
            queue(entry);
        }
    }

    /** Cancels pending work and releases this model's materials immediately. */
    public void unregister(Spatial root) {
        checkThread(false);
        Entry entry = entries.remove(root);
        if (entry != null) {
            active.remove(entry);
            pending.remove(entry);
            waiting.remove(entry);
            entry.control.entry = null;
            root.removeControl(entry.control);
            entry.materials = List.of();
            entry.bound = null;
            materialsChanged(root.getParent());
        }
    }

    /** Reports a mutation to the nearest registered owner, including mutations of child meshes. */
    public static void materialsChanged(Spatial spatial) {
        for (Spatial current = spatial; current != null; current = current.getParent()) {
            InvalidationControl control = current.getControl(InvalidationControl.class);
            if (control != null && control.entry != null) {
                control.entry.owner.queue(control.entry);
                return;
            }
        }
    }

    /** Preparation must not cross another live registration's ownership boundary. */
    static boolean isRegisteredRoot(Spatial spatial) {
        InvalidationControl control = spatial.getControl(InvalidationControl.class);
        return control != null && control.entry != null;
    }

    /** Preserves the existing MeshCenter sampling phase without recurring geometry walks. */
    public void update(LightRules rules) {
        checkThread(true);
        if (closed) {
            return;
        }
        if (!cached) {
            for (int i = 0; i < active.size(); i++) {
                queue(active.get(i));
            }
        }
        flush(rules);
        for (int i = 0; i < active.size(); i++) {
            updateCenter(active.get(i));
        }
    }

    /** Completes late material mutations before any renderer pass consumes the geometry. */
    public void flush(LightRules rules) {
        checkThread(true);
        if (closed) {
            return;
        }
        MaterialBinding next = rules == null ? null : rules.getMaterialBinding();
        boolean bindingsChanged = next != binding;
        binding = next;
        if (bindingsChanged && binding != null && binding.isAmbientReady()) {
            for (int i = 0; i < waiting.size(); i++) {
                Entry current = waiting.get(i);
                current.waiting = false;
                queue(current);
            }
            waiting.clear();
        }
        Entry entry;
        while ((entry = pending.pollFirst()) != null) {
            entry.queued = false;
            if (!entry.policy.fullBright() && (binding == null || !binding.isAmbientReady())) {
                entry.waiting = true;
                waiting.add(entry);
                continue;
            }
            entry.materials = provider.prepare(entry.root, entry.policy, entry.materials);
            entry.bound = null;
            entry.centerValid = false;
            updateCenter(entry);
            bind(entry);
        }
        // Only a resource transition or freshly prepared entry needs binding.
        // No traversal or allocation is involved while ambient resources are pending.
        if (bindingsChanged) {
            for (int i = 0; i < active.size(); i++) {
                bind(active.get(i));
            }
        }
    }

    private void bind(Entry entry) {
        if (!entry.policy.fullBright() && entry.bound != binding) {
            if (binding == null) {
                for (int i = 0; i < entry.materials.size(); i++) {
                    Material material = entry.materials.get(i);
                    material.clearParam("LightData");
                    material.clearParam("AmbientMap");
                    material.clearParam("AmbientMapSize");
                }
            } else {
                binding.bindMaterials(entry.materials);
            }
            entry.bound = binding;
        }
    }

    private void queue(Entry entry) {
        checkThread(false);
        if (!closed && !entry.queued && !entry.waiting) {
            entry.queued = true;
            pending.addLast(entry);
        }
    }

    private static void updateCenter(Entry entry) {
        if (entry.policy.fullBright() || entry.materials.isEmpty()) {
            return;
        }
        var bound = entry.root.getWorldBound();
        Vector3f center = bound == null ? entry.root.getWorldTranslation() : bound.getCenter();
        if (!entry.centerValid || !entry.center.equals(center)) {
            entry.center.set(center);
            entry.centerValid = true;
            for (int i = 0; i < entry.materials.size(); i++) {
                entry.materials.get(i).setVector3("MeshCenter", entry.center);
            }
        }
    }

    @Override
    public void close() {
        checkThread(false);
        closed = true;
        for (int i = 0; i < active.size(); i++) {
            Entry entry = active.get(i);
            entry.control.entry = null;
            entry.root.removeControl(entry.control);
            materialsChanged(entry.root.getParent());
        }
        entries.clear();
        active.clear();
        pending.clear();
        waiting.clear();
        binding = null;
    }

    private void checkThread(boolean claim) {
        Thread current = Thread.currentThread();
        if (ownerThread == null && claim) {
            ownerThread = current;
        } else if (ownerThread != null && ownerThread != current) {
            throw new IllegalStateException("Material setup must be used on its owning render thread");
        }
    }

    int registeredModels() {
        return active.size();
    }

    private final class Entry {
        private final EntityMaterialSetup owner = EntityMaterialSetup.this;
        private final Spatial root;
        private final InvalidationControl control;
        private final Vector3f center = new Vector3f();
        private MaterialSetupPolicy policy;
        private List<Material> materials = List.of();
        private MaterialBinding bound;
        private boolean queued;
        private boolean waiting;
        private boolean centerValid;

        private Entry(Spatial root, MaterialSetupPolicy policy, InvalidationControl control) {
            this.root = root;
            this.policy = policy;
            this.control = control;
        }
    }

    /**
     * Mutation routing only. Cloning/exporting cannot transfer a registration,
     * cache, callback, lighting context or material list to a new instance.
     */
    public static final class InvalidationControl extends AbstractControl {
        private transient Entry entry;

        public InvalidationControl() {
            setEnabled(false);
        }

        @Override
        public Object jmeClone() {
            return new InvalidationControl();
        }

        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager manager, ViewPort view) {
        }
    }
}
