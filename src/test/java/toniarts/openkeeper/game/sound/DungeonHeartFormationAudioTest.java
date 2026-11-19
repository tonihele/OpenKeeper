/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.sound;

import com.jme3.scene.Node;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DungeonHeartIntroduction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.sound.BankMapFileEntry.BankFlag;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.DungeonHeartIntroductionState;
import toniarts.openkeeper.view.effect.EffectManagerState;
import static org.junit.jupiter.api.Assertions.*;

class DungeonHeartFormationAudioTest {
    private final List<RecordingBackend> backends = new ArrayList<>();
    private Map<String, SoundCategory> categories;
    private SoundCategory previous;
    private DefaultEntityData data;
    private EntityId room;
    private DungeonHeartIntroduction intro;
    private DungeonHeartIntroductionState state;
    private final AtomicLong now = new AtomicLong(1_000_000_000L);
    private static final String KEY = "global:" + ThingSounds.CATEGORY_DUNGEON_HEART;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        var archive = new SoundArchive("HeartFixture", EnumSet.of(BankFlag.RESIDENT), null, List.of("formation.mp2"));
        var sample = new SoundSample(1, 100, 1000, 0, archive);
        var group = new SoundEventGroup(100, 100, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, List.of(sample), List.of());
        var event = new SoundEvent(ThingSounds.OBJECT_CREATE, 100, EnumSet.noneOf(SoundEventFlag.class), List.of(group));
        var category = new SoundCategory(ThingSounds.CATEGORY_DUNGEON_HEART, 1, Map.of(ThingSounds.OBJECT_CREATE, event));
        Field cache = SoundCategory.class.getDeclaredField("CACHE");
        cache.setAccessible(true);
        categories = (Map<String, SoundCategory>) cache.get(null);
        previous = categories.put(KEY, category);
        SoundManager.init(spec -> {
            var backend = new RecordingBackend();
            backends.add(backend);
            return backend;
        });
        data = new DefaultEntityData();
        room = data.createEntity();
        intro = new DungeonHeartIntroduction(null, data.createEntity(), data.createEntity(),
                data.createEntity(), data.createEntity(), data.createEntity());
        intro.startTime = now.get();
        intro.waitingStartTime = intro.startTime;
        intro.ticksPerSecond = 4;
        data.setComponents(room, intro, new Owner((short) 3, (short) 3),
                new RoomComponent((short) 5, false, new Point(0, 0)));
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{IKwdFile.class}, (p, m, a) -> null);
        state = new DungeonHeartIntroductionState(data, (short) 3, new Node(),
                new EffectManagerState(kwd, null), id -> null, point -> true, now::get);
        state.start();
    }

    @AfterEach
    void cleanup() {
        state.cleanup();
        data.close();
        SoundManager.shutdown();
        if (previous == null) categories.remove(KEY);
        else categories.put(KEY, previous);
    }

    private void realiseSound() {
        state.update(0);
        SoundManager.getEngine().tick();
        state.update(0);
        SoundManager.getEngine().tick();
    }

    @Test
    void lateSoundSeeksOnceAndPauseWaitsForTheRebasedEpochBeforeResuming() {
        now.addAndGet(3_000_000_000L);
        realiseSound();
        assertEquals(1, backends.size());
        var backend = backends.getFirst();
        assertEquals(List.of(3f), backend.seeks);
        state.setPaused(true);
        assertEquals(AudioBackend.Status.PAUSED, backend.status);
        now.addAndGet(40_000_000_000L);
        state.setPaused(false); // notification beats the ECS delta
        state.update(0);
        assertEquals(AudioBackend.Status.PAUSED, backend.status);
        assertEquals(1, backend.plays);
        var resumed = new DungeonHeartIntroduction(intro);
        resumed.startTime += 40_000_000_000L;
        resumed.waitingStartTime += 40_000_000_000L;
        data.setComponent(room, resumed);
        state.update(0);
        assertEquals(List.of(3f, 3f), backend.seeks);
        assertEquals(2, backend.plays);
        assertEquals(AudioBackend.Status.PLAYING, backend.status);
        data.removeComponent(room, DungeonHeartIntroduction.class);
        state.update(0);
        SoundManager.getEngine().tick();
        assertEquals(AudioBackend.Status.STOPPED, backend.status);
    }

    @Test
    void anExpiredSoundIsStoppedAndNeverRestarted() {
        now.addAndGet(11_000_000_000L);
        realiseSound();
        var backend = backends.getFirst();
        assertEquals(AudioBackend.Status.STOPPED, backend.status);
        int plays = backend.plays;
        state.setPaused(true);
        now.addAndGet(5_000_000_000L);
        state.setPaused(false);
        var resumed = new DungeonHeartIntroduction(intro);
        resumed.startTime += 5_000_000_000L;
        resumed.waitingStartTime += 5_000_000_000L;
        data.setComponent(room, resumed);
        state.update(0);
        assertEquals(plays, backend.plays);
        assertEquals(AudioBackend.Status.STOPPED, backend.status);
    }

    @Test
    void aBackendRealisedDuringPauseStaysPausedUntilTheEpochArrives() {
        state.update(0); // queue audio; backend is not available yet
        state.setPaused(true);
        SoundManager.getEngine().tick();
        state.update(0);
        assertEquals(AudioBackend.Status.PAUSED, backends.getFirst().status);
        now.addAndGet(5_000_000_000L);
        state.setPaused(false);
        state.update(0);
        assertEquals(AudioBackend.Status.PAUSED, backends.getFirst().status);
        var resumed = new DungeonHeartIntroduction(intro);
        resumed.startTime += 5_000_000_000L;
        resumed.waitingStartTime += 5_000_000_000L;
        data.setComponent(room, resumed);
        state.update(0);
        assertEquals(AudioBackend.Status.PLAYING, backends.getFirst().status);
        assertEquals(2, backends.getFirst().plays);
    }

    @Test
    void pauseResumeDoesNotReviveANaturallyFinishedVoice() {
        realiseSound();
        var backend = backends.getFirst();
        backend.stop();
        state.setPaused(true);
        now.addAndGet(5_000_000_000L);
        state.setPaused(false);
        var resumed = new DungeonHeartIntroduction(intro);
        resumed.startTime += 5_000_000_000L;
        resumed.waitingStartTime += 5_000_000_000L;
        data.setComponent(room, resumed);
        state.update(0);
        assertEquals(1, backend.plays);
        assertEquals(AudioBackend.Status.STOPPED, backend.status);
    }

    private static final class RecordingBackend implements AudioBackend {
        private Status status = Status.STOPPED;
        private int plays;
        private final List<Float> seeks = new ArrayList<>();
        @Override public Status getStatus() { return status; }
        @Override public void play() { plays++; status = Status.PLAYING; }
        @Override public void pause() { status = Status.PAUSED; }
        @Override public void stop() { status = Status.STOPPED; }
        @Override public void setGain(float gain) { }
        @Override public void setPitch(float pitch) { }
        @Override public void setLooping(boolean looping) { }
        @Override public void setTimeOffset(float seconds) { seeks.add(seconds); }
        @Override public float getPlaybackTime() { return 0; }
        @Override public float getDuration() { return 10; }
    }
}
