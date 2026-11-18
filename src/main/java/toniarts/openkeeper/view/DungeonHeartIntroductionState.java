/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioKey;
import com.jme3.app.state.AbstractAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.DungeonHeartIntroduction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.sound.AudioBackend;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.SoundVoice;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.control.SpatialOpacity;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.EffectPlayback;

/** Render-thread owner of each assembly; formation cues belong to the local keeper. */
public final class DungeonHeartIntroductionState extends AbstractAppState {
    private final EntitySet rooms;
    private final Map<EntityId, Presentation> presentations = new HashMap<>();
    private final Function<EntityId, Spatial> models;
    private final Predicate<Point> visible;
    private final LongSupplier clock;
    private final Node root;
    private final EffectManagerState effects;
    private final short player;
    private boolean started;
    private boolean paused;
    private float formationDuration;

    public DungeonHeartIntroductionState(EntityData entities, short player, Node root,
            EffectManagerState effects, Function<EntityId, Spatial> models,
            Predicate<Point> visible, LongSupplier clock) {
        this.player = player;
        this.root = root;
        this.effects = effects;
        this.models = models;
        this.visible = visible;
        this.clock = clock;
        rooms = entities.getEntities(Owner.class, RoomComponent.class, DungeonHeartIntroduction.class);
    }

    public void start() {
        started = true;
    }

    /** Resolve audio duration before ready, so a late view never starts an expired cue. */
    public void prewarmAudio(AssetManager assets) {
        String path = SoundManager.getSoundAssetPath(ThingSounds.CATEGORY_DUNGEON_HEART, ThingSounds.OBJECT_CREATE);
        if (path != null) {
            try {
                formationDuration = assets.loadAudio(new AudioKey(path, false)).getDuration();
            } catch (RuntimeException ex) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,
                        "Unable to prewarm Heart formation sound", ex);
            }
        }
    }

    public void setPaused(boolean paused) {
        if (this.paused == paused) {
            return;
        }
        this.paused = paused;
        for (Presentation presentation : presentations.values()) {
            presentation.setPaused(paused);
        }
    }

    @Override
    public void update(float tpf) {
        rooms.applyChanges();
        for (Entity removed : rooms.getRemovedEntities()) {
            Presentation presentation = presentations.remove(removed.getId());
            if (presentation != null) {
                presentation.close();
            }
        }
        long now = clock.getAsLong();
        for (Entity entity : rooms) {
            Presentation presentation = presentations.computeIfAbsent(entity.getId(),
                    id -> new Presentation(id, entity.get(Owner.class).ownerId));
            presentation.update(entity.get(DungeonHeartIntroduction.class), entity.get(RoomComponent.class), now);
        }
    }

    @Override
    public void cleanup() {
        presentations.values().forEach(Presentation::close);
        presentations.clear();
        rooms.release();
        super.cleanup();
    }

    private final class Presentation implements AutoCloseable {
        private final Node node = new Node("Dungeon Heart introduction");
        private final EffectPlayback playback;
        private final Map<EntityId, Part> parts = new HashMap<>();
        private final Vector3f position = new Vector3f();
        private Spatial heart;
        private AnimComposer composer;
        private double clipLength;
        private double cursor = Double.NaN;
        private double nextStatic;
        private boolean formed;
        private boolean revealed;
        private int sound = SoundEngine.NO_HANDLE;
        private boolean soundSought;
        private long observedStartTime;
        private long pausedStartTime;
        private boolean resumePending;
        private final short owner;
        private DungeonHeartIntroduction assembly;

        private record Part(Spatial model, int revision, SpatialOpacity opacity) { }

        Presentation(EntityId room, short owner) {
            this.owner = owner;
            root.attachChild(node);
            playback = new EffectPlayback(effects, node, owner, room.getId());
        }

        void update(DungeonHeartIntroduction intro, RoomComponent room, long now) {
            if (room.destroyed) {
                close();
                return;
            }
            observedStartTime = intro.startTime;
            if (resumePending && intro.pausedAt == 0 && intro.startTime != pausedStartTime) {
                resumePending = false;
            }
            if (assembly == null || !Objects.equals(assembly.arches, intro.arches)
                    || !Objects.equals(assembly.step0, intro.step0)
                    || !Objects.equals(assembly.step1, intro.step1)
                    || !Objects.equals(assembly.step2, intro.step2)) {
                parts.values().forEach(part -> part.opacity.close());
                parts.clear();
                assembly = intro;
            }
            bindHeart(intro.heart);
            // Ethereal returns zero until the first server time sample. Treat
            // that as pending, not a delay measured from the server's uptime.
            double age = started && !paused && !resumePending && now != 0 ? intro.age(now) : cursor;
            if (!Double.isFinite(age)) {
                age = Double.isFinite(cursor) ? cursor : Double.NEGATIVE_INFINITY;
            }
            // Network clock corrections must never replay an already consumed cue.
            if (Double.isFinite(cursor)) {
                age = Math.max(cursor, age);
            }
            float opacity = intro.complete ? 1 : !Double.isFinite(age) ? 0 : (float) Math.clamp(
                    (age - DungeonHeartIntroduction.REVEAL_START) * intro.ticksPerSecond * 32 / 255, 0, 1);
            if (age >= DungeonHeartIntroduction.COMPLETE_TIME) {
                opacity = 1;
            }
            fade(intro.arches, opacity);
            fade(intro.step0, opacity);
            fade(intro.step1, opacity);
            fade(intro.step2, opacity);
            node.setCullHint(visible.test(room.location) ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
            if (composer != null && composer.getCurrentAction() != null) {
                composer.setGlobalSpeed(0);
                composer.setTime(Math.max(0, Double.isFinite(age) ? age : 0) % clipLength);
            }
            if (!Double.isFinite(age) || paused || resumePending) {
                pauseSound();
                return;
            }
            if (!Double.isFinite(cursor)) {
                // Replay the authoritative waiting interval too: its live tail
                // must not depend on when the first rendered frame arrives.
                cursor = intro.waitingStartTime != 0
                        ? (intro.waitingStartTime - intro.startTime) / 1_000_000_000.0
                        : age < 0 ? Math.floor(age * Math.max(1, intro.ticksPerSecond))
                                / Math.max(1, intro.ticksPerSecond) : 0;
                nextStatic = cursor;
            }
            position.set(room.location.x, WorldUtils.FLOOR_HEIGHT, room.location.y);
            while (nextStatic < 0 && nextStatic <= age) {
                playback.advance(nextStatic - cursor);
                cursor = nextStatic;
                playback.spawn(DungeonHeartIntroduction.WAIT_EFFECT, position,
                        Math.round(nextStatic * Math.max(1, intro.ticksPerSecond)));
                nextStatic += 1.0 / Math.max(1, intro.ticksPerSecond);
            }
            if (!formed && age >= 0) {
                playback.advance(-cursor);
                cursor = 0;
                if (owner == player) {
                    playback.spawn(DungeonHeartIntroduction.START_EFFECT, position);
                }
                formed = true;
                if (owner == player && visible.test(room.location)
                        && (formationDuration <= 0 || age < formationDuration)) {
                    sound = ThingSounds.play(ThingSounds.CATEGORY_DUNGEON_HEART, ThingSounds.OBJECT_CREATE, room.location);
                }
            }
            if (!revealed && age >= DungeonHeartIntroduction.REVEAL_START) {
                playback.advance(DungeonHeartIntroduction.REVEAL_START - cursor);
                cursor = DungeonHeartIntroduction.REVEAL_START;
                if (owner == player) {
                    playback.spawn(DungeonHeartIntroduction.REVEAL_EFFECT, position);
                }
                revealed = true;
            }
            playback.advance(age - cursor);
            cursor = age;
            seekSound(age);
        }

        private void bindHeart(EntityId id) {
            Spatial model = models.apply(id);
            if (model == heart && composer != null) {
                for (Spatial current = composer.getSpatial(); current != null; current = current.getParent()) {
                    if (current == model) {
                        return;
                    }
                }
            }
            if (composer != null) {
                composer.setGlobalSpeed(1);
            }
            heart = model;
            composer = null;
            if (model != null) {
                model.depthFirstTraversal(spatial -> {
                    AnimComposer candidate = spatial.getControl(AnimComposer.class);
                    if (candidate != null && candidate.hasAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME)) {
                        composer = candidate;
                        clipLength = candidate.getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME).getLength();
                    }
                });
                if (composer != null && clipLength > 0) {
                    // The existing loader selected the normal WRAP resource. Only
                    // its time is owned here; no model reload or idle transition.
                    composer.setGlobalSpeed(0);
                } else {
                    composer = null;
                }
            }
        }

        private void fade(EntityId id, float value) {
            Spatial model = models.apply(id);
            if (model == null) {
                return;
            }
            Part part = parts.get(id);
            int revision = AnimationLoader.getModelRevision(model);
            if (part == null || part.model != model || part.revision != revision) {
                if (part != null) {
                    part.opacity.close();
                }
                part = new Part(model, revision, new SpatialOpacity(model));
                parts.put(id, part);
            }
            part.opacity.set(value);
        }

        private AudioBackend soundBackend() {
            SoundEngine engine = SoundManager.getEngine();
            SoundVoice voice = engine == null ? null : engine.getVoice(sound);
            return voice == null ? null : voice.getBackend();
        }

        private void seekSound(double age) {
            AudioBackend backend = soundBackend();
            if (backend != null && !soundSought) {
                soundSought = true;
                float duration = backend.getDuration();
                if (duration > 0 && age >= duration) {
                    SoundManager.stopSound(sound);
                    return;
                } else if (age > EffectPlayback.STEP) {
                    backend.setTimeOffset((float) age);
                }
                if (backend.getStatus() == AudioBackend.Status.PAUSED) {
                    backend.play();
                }
            }
        }

        void setPaused(boolean paused) {
            if (paused) {
                pausedStartTime = observedStartTime;
                pauseSound();
            } else {
                // Resume notification and ECS deltas use different network paths.
                // Hold phase/audio until the rebased server epoch has arrived.
                resumePending = observedStartTime != 0;
                soundSought = false;
            }
        }

        private void pauseSound() {
            AudioBackend backend = soundBackend();
            if (backend != null && backend.getStatus() == AudioBackend.Status.PLAYING) {
                backend.pause();
            }
        }

        @Override
        public void close() {
            playback.close();
            SoundManager.stopSound(sound);
            if (composer != null) {
                composer.setGlobalSpeed(1);
            }
            parts.values().forEach(part -> part.opacity.close());
            parts.clear();
            node.removeFromParent();
        }
    }
}
