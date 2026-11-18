/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.effect;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.tools.convert.map.Effect;

/**
 * A finite, seeded effect graph with the native 20 Hz visual clock. This owns
 * logical updates of its controls; jME still performs their normal rendering.
 * Used when a sequence must be seekable and independent of render frame rate.
 */
public final class EffectPlayback implements AutoCloseable {
    public static final double STEP = 1.0 / 20;
    private final EffectManagerState manager;
    private final Node root = new Node("Timed effects");
    private final List<VisualEffect> effects = new ArrayList<>();
    private final List<ManualControl> controls = new ArrayList<>();
    private final long seed;
    private final short owner;
    private double remainder;
    private boolean closed;

    public EffectPlayback(EffectManagerState manager, Node parent, short owner, long seed) {
        this.manager = manager;
        this.owner = owner;
        this.seed = seed;
        parent.attachChild(root);
    }

    public void spawn(int id, Vector3f position) {
        spawn(id, position, 0);
    }

    /** A cue's random stream is independent of omitted or late-arriving cues. */
    public void spawn(int id, Vector3f position, long occurrence) {
        if (closed) {
            return;
        }
        Effect effect = manager.getKwdFile().getEffect(id);
        if (effect == null) {
            return;
        }
        try {
            RandomGenerator random = new Random(seed ^ (0x9e3779b97f4a7c15L * id)
                    ^ Long.rotateLeft(occurrence, 21));
            effects.add(new VisualEffect(manager, root, position, effect, false, owner, false, this, random));
        } catch (RuntimeException ex) {
            System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,
                    "Unable to play effect " + id, ex);
        }
    }

    /** Register once, after a model/emitter has acquired all its controls. */
    void manage(Spatial spatial) {
        spatial.depthFirstTraversal(child -> {
            for (int i = child.getNumControls() - 1; i >= 0; i--) {
                Control control = child.getControl(i);
                if (!(control instanceof ManualControl)) {
                    child.removeControl(control);
                    ManualControl manual = new ManualControl(control);
                    child.addControl(manual);
                    controls.add(manual);
                }
            }
        });
    }

    public void advance(double seconds) {
        if (closed || seconds <= 0 || !Double.isFinite(seconds)) {
            return;
        }
        remainder += seconds;
        if (effects.isEmpty()) {
            retainFractionalStep();
            controls.clear();
            return;
        }
        while (remainder + 1e-9 >= STEP) {
            remainder -= STEP;
            // World transforms are resolved lazily by the controls. A subtree
            // geometric update is illegal while its parent's lights/material
            // overrides are dirty; the application owns that traversal.
            int count = controls.size();
            for (int i = 0; i < count; i++) {
                ManualControl control = controls.get(i);
                if (belongsToRoot(control.getSpatial())) {
                    control.delegate.update((float) STEP);
                }
            }
            controls.removeIf(control -> !belongsToRoot(control.getSpatial()));
            for (int i = effects.size() - 1; i >= 0; i--) {
                if (!effects.get(i).update((float) STEP)) {
                    effects.remove(i);
                }
            }
            if (effects.isEmpty()) {
                retainFractionalStep();
                controls.clear();
                break;
            }
        }
    }

    private void retainFractionalStep() {
        // Floating remainder can turn an exact multiple (e.g. .25) into
        // almost STEP, incorrectly adding a tick to the next cue.
        remainder = Math.max(0, remainder - Math.floor((remainder + 1e-9) / STEP) * STEP);
    }

    private boolean belongsToRoot(Spatial spatial) {
        for (Spatial current = spatial; current != null; current = current.getParent()) {
            if (current == root) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return effects.isEmpty();
    }

    @Override
    public void close() {
        closed = true;
        effects.forEach(VisualEffect::removeEffect);
        effects.clear();
        controls.clear();
        root.removeFromParent();
    }

    private static final class ManualControl extends AbstractControl {
        private final Control delegate;

        ManualControl(Control delegate) {
            this.delegate = delegate;
        }

        @Override
        public void setSpatial(Spatial spatial) {
            super.setSpatial(spatial);
            delegate.setSpatial(spatial);
        }

        @Override
        protected void controlUpdate(float tpf) {
            // Only EffectPlayback.advance() advances simulation.
        }

        @Override
        protected void controlRender(RenderManager renderer, ViewPort viewPort) {
            delegate.render(renderer, viewPort);
        }
    }
}
