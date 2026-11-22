package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.sound.AudioBackend;
import toniarts.openkeeper.game.sound.AudioSpec;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.sound.SoundArchive;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundEvent;
import toniarts.openkeeper.game.sound.SoundEventGroup;
import toniarts.openkeeper.game.sound.SoundSample;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;

class HaulingSoundStateTest {
    private final DefaultEntityData server = new DefaultEntityData();
    private final List<Backend> backends = new ArrayList<>();
    private final SoundEngine engine = new SoundEngine(spec -> {
        Backend backend = new Backend(spec);
        backends.add(backend);
        return backend;
    });
    private final AtomicInteger categoryRequests = new AtomicInteger();
    private EntityId imp;
    private EntityId load;
    private HaulingSoundState state;
    private boolean visible = true;

    @BeforeEach
    void start() throws Exception {
        SoundCategory sounds = category();
        engine.setThrottleEnabled(false);
        engine.setListener(new Point(3, 4));
        state = new HaulingSoundState(new ClientEntityData(server), 0.25f, () -> engine, id -> {
            assertEquals((short) 1, id.shortValue());
            categoryRequests.incrementAndGet();
            return sounds;
        }, tile -> visible);
        imp = server.createEntity();
        server.setComponents(imp, new Position(0, new Vector3f(3, 1, 4)),
                new CreatureViewState((short) 1, 0, AnimationType.WALK));
        load = server.createEntity();
        server.setComponents(load, new Position(0, new Vector3f(10, 1, 10)), new HauledBy(imp));
    }

    @AfterEach
    void close() {
        if (state != null) {
            state.cleanup();
        }
        engine.stopAll();
        server.close();
    }

    @Test
    void cueUsesTheHaulerAndDoesNotLayerOrDependOnMovement() {
        update(0);
        assertEquals(1, backends.size());
        Backend first = backends.getFirst();
        assertEquals("Sounds/Global/Synthetic/drag.wav", first.spec.assetPath());
        assertTrue(first.spec.positional());
        assertEquals(3, first.spec.x());
        assertEquals(4, first.spec.z());
        server.setComponent(imp, new CreatureViewState((short) 1, 0, AnimationType.STAND_STILL));
        for (int i = 0; i < 20; i++) {
            update(0.25f);
        }
        assertEquals(1, backends.size());
        assertEquals(AudioBackend.Status.PLAYING, first.status);
        server.setComponent(imp, new Position(0, new Vector3f(5, 1, 6)));
        update(0.01f);
        assertEquals(5, first.x);
        assertEquals(6, first.z);
        assertEquals(1, categoryRequests.get());
    }

    @Test
    void completedOneShotsRestartAtMostOncePerGameTick() {
        update(0);
        backends.getFirst().status = AudioBackend.Status.STOPPED;
        engine.tick();
        update(0.1f);
        assertEquals(1, backends.size());
        update(0.15f);
        assertEquals(2, backends.size());
        assertEquals(1, engine.getActiveVoiceCount());
    }

    @Test
    void deliveryAndInterruptionStopTheVoiceAndCanStartANewTrip() {
        update(0);
        Backend first = backends.getFirst();
        server.removeComponent(load, HauledBy.class);
        update(0.25f);
        assertEquals(AudioBackend.Status.STOPPED, first.status);
        update(1);
        assertEquals(1, backends.size());
        server.setComponent(load, new HauledBy(imp));
        update(0.25f);
        assertEquals(2, backends.size());
        server.setComponent(imp, new InHand());
        update(0.25f);
        assertEquals(AudioBackend.Status.STOPPED, backends.getLast().status);
        update(1);
        assertEquals(2, backends.size());
    }

    @Test
    void losingTheHaulerPositionStopsImmediately() {
        update(0);
        server.removeComponent(imp, Position.class);
        update(0.01f);
        assertEquals(AudioBackend.Status.STOPPED, backends.getFirst().status);
        update(1);
        assertEquals(1, backends.size());
    }

    @Test
    void fogAndRemovedLoadsDoNotLeaveSoundsBehind() {
        update(0);
        visible = false;
        update(0.01f);
        assertEquals(AudioBackend.Status.STOPPED, backends.getFirst().status);
        update(1);
        assertEquals(1, backends.size());
        visible = true;
        update(0.25f);
        assertEquals(2, backends.size());
        server.removeEntity(load);
        update(0.01f);
        assertEquals(AudioBackend.Status.STOPPED, backends.getLast().status);
    }

    @Test
    void duplicateRelationsHaveOneVoiceAndChangingHaulersStopsTheOldOne() {
        EntityId otherLoad = server.createEntity();
        server.setComponents(otherLoad, new Position(0, new Vector3f(3, 1, 4)), new HauledBy(imp));
        update(0);
        assertEquals(1, backends.size());
        server.removeComponent(otherLoad, HauledBy.class);
        EntityId otherImp = server.createEntity();
        server.setComponents(otherImp, new Position(0, new Vector3f(6, 1, 4)),
                new CreatureViewState((short) 1, 0, AnimationType.WALK));
        server.setComponent(load, new HauledBy(otherImp));
        update(0.25f);
        assertEquals(2, backends.size());
        assertEquals(AudioBackend.Status.STOPPED, backends.getFirst().status);
        assertEquals(1, engine.getActiveVoiceCount());
    }

    @Test
    void disablingAndCleanupStopActiveVoices() {
        update(0);
        state.setEnabled(false);
        engine.tick();
        assertEquals(AudioBackend.Status.STOPPED, backends.getFirst().status);
        state.setEnabled(true);
        update(0.25f);
        assertEquals(2, backends.size());
        state.cleanup();
        engine.tick();
        assertEquals(AudioBackend.Status.STOPPED, backends.getLast().status);
        state = null;
    }

    private void update(float tpf) {
        state.update(tpf);
        engine.tick();
    }

    private static SoundCategory category() throws Exception {
        SoundArchive archive = new SoundArchive("Global/Synthetic", Set.of(), null, List.of("drag.wav"));
        SoundSample sample = new SoundSample(1, 100, 1000, 0, archive);
        SoundEventGroup group = new SoundEventGroup(100, 100, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0,
                List.of(sample), List.of());
        int eventId = GlobalType.CREATURE_DRAG.getId();
        SoundEvent event = new SoundEvent(eventId, 150, Set.of(), List.of(group));
        var constructor = SoundCategory.class.getDeclaredConstructor(String.class, int.class, Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance("SYNTHETIC_IMP", 1, Map.of(eventId, event));
    }

    private static final class Backend implements AudioBackend {
        final AudioSpec spec;
        Status status = Status.STOPPED;
        float x;
        float z;
        Backend(AudioSpec spec) { this.spec = spec; }
        @Override public Status getStatus() { return status; }
        @Override public void play() { status = Status.PLAYING; }
        @Override public void pause() { status = Status.PAUSED; }
        @Override public void stop() { status = Status.STOPPED; }
        @Override public void setGain(float gain) { }
        @Override public void setPitch(float pitch) { }
        @Override public void setLooping(boolean looping) { assertFalse(looping); }
        @Override public void setTimeOffset(float seconds) { }
        @Override public float getPlaybackTime() { return 0; }
        @Override public float getDuration() { return 1; }
        @Override public void setPosition(float x, float y, float z) { this.x = x; this.z = z; }
    }
}
