package toniarts.openkeeper.game.sound;

import java.util.EnumSet;
import toniarts.openkeeper.tools.convert.sound.BankMapFileEntry.BankFlag;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundEngineTest {

    private static final int EVENT = 0x1C5;
    private static final int LOOP_EVENT = 0x200;
    private static final int DEFERRED_EVENT = 0x300;

    private List<RecordingBackend> backends;
    private SoundEngine engine;
    private SoundCategory category;

    @BeforeEach
    void setUp() {
        backends = new ArrayList<>();
        engine = new SoundEngine(spec -> {
            RecordingBackend backend = new RecordingBackend(spec);
            backends.add(backend);
            return backend;
        });
        category = category();
    }

    @Test
    void playsAnEventAndReturnsAGenerationCheckedHandle() {
        int handle = engine.play(category, EVENT);

        assertNotEquals(SoundEngine.NO_HANDLE, handle);
        // the voice is queued first and realised on the next tick
        assertTrue(engine.isPlaying(handle));
        assertEquals(0, backends.size());

        engine.tick();

        assertEquals(1, backends.size());
        assertTrue(engine.isPlaying(handle));
        assertTrue(backends.get(0).played);
        assertFalse(backends.get(0).spec.positional());

        engine.stop(handle);
        assertFalse(engine.isPlaying(handle));
    }

    @Test
    void unknownEventIsNotPlayed() {
        assertEquals(SoundEngine.NO_HANDLE, engine.play(category, 0x4711));
        assertEquals(0, backends.size());
    }

    @Test
    void positionalEventCarriesThePosition() {
        engine.setListener(new Point(0, 0));

        int handle = engine.play(category, EVENT, new Point(3, 4));
        engine.tick();

        assertEquals(1, backends.size());
        assertTrue(backends.get(0).spec.positional());
        assertEquals(3.0f, backends.get(0).spec.x());
        assertEquals(4.0f, backends.get(0).spec.z());
        assertEquals(5.0f, voiceOf(handle).getDistanceToListener(), 0.001f);
        engine.stop(handle);
    }

    @Test
    void handleOfTheSameSlotIsNotValidAfterRestart() {
        int first = engine.play(category, EVENT);
        engine.tick();
        engine.stop(first);
        engine.tick();

        int second = engine.play(category, EVENT);
        engine.tick();

        assertNotEquals(first, second, "the handle must be generation checked");
        assertFalse(engine.isPlaying(first), "the old handle must not be valid anymore");
        assertTrue(engine.isPlaying(second));
        engine.stop(second);
    }

    @Test
    void onlyAsManyVoicesAsSlotsAreHandedOut() {
        List<Integer> handles = new ArrayList<>();
        for (int i = 0; i < SoundEngine.MAX_VOICES; i++) {
            int handle = engine.play(category, EVENT);
            assertNotEquals(SoundEngine.NO_HANDLE, handle);
            handles.add(handle);
        }

        assertEquals(SoundEngine.NO_HANDLE, engine.play(category, EVENT));
        assertEquals(SoundEngine.MAX_VOICES, engine.getActiveVoiceCount());

        engine.stopAll();
        assertEquals(0, engine.getActiveVoiceCount());
        assertNotEquals(SoundEngine.NO_HANDLE, engine.play(category, EVENT),
                "slots must be free again after stopAll");
    }

    @Test
    void loopEventKeepsPlaying() {
        int handle = engine.play(category, LOOP_EVENT);
        engine.tick();

        assertTrue(engine.isPlaying(handle));
        assertTrue(backends.get(0).spec.looping());

        engine.tick();
        assertTrue(engine.isPlaying(handle), "a loop event never finishes by itself");
        engine.stop(handle);
    }

    @Test
    void channelVolumesAndMuteAffectTheGain() {
        int handle = engine.play(category, EVENT);
        engine.tick();

        SoundVoice voice = voiceOf(handle);
        assertEquals(1.0f, engine.effectiveGain(voice), 0.001f);

        engine.setChannelVolume(0, 50);
        assertEquals(0.5f, engine.effectiveGain(voice), 0.001f);

        engine.setChannelMuted(0, true);
        assertEquals(0.0f, engine.effectiveGain(voice), 0.001f);

        engine.setChannelMuted(0, false);
        engine.setChannelVolume(0, 100);
        assertEquals(1.0f, engine.effectiveGain(voice), 0.001f);

        // the master volume is clamped like the volume of a voice
        engine.setMasterVolume(200);
        engine.setChannelVolume(0, 50);
        assertEquals(1.27f * 0.5f, engine.effectiveGain(voiceOf(handle)), 0.001f);
        engine.stop(handle);
    }

    @Test
    void pausedEnginePlaysNothing() {
        engine.setPaused(true);

        assertEquals(SoundEngine.NO_HANDLE, engine.play(category, EVENT));
        assertEquals(0, backends.size());
    }

    @Test
    void deferredEventIsPlayedLater() {
        int handle = engine.play(category, DEFERRED_EVENT);

        assertTrue(engine.isPlaying(handle), "a deferred voice is queued, not finished");
        engine.tick();

        assertTrue(engine.isPlaying(handle));
        assertTrue(backends.size() <= 1);
    }

    @Test
    void stopAllForgetsQueuedVoices() {
        engine.play(category, EVENT);
        engine.play(category, EVENT);

        engine.stopAll();

        assertEquals(0, engine.getActiveVoiceCount());
        engine.tick();
        assertEquals(0, backends.size(), "queued voices must not be started after stopAll");
    }

    @Test
    void deferredSoundBeyondTheAudibleDistanceIsNotAccepted() {
        engine.setListener(new Point(0, 0));

        assertEquals(SoundEngine.NO_HANDLE, engine.play(category, DEFERRED_EVENT, distant()),
                "a deferred sound is dropped beyond the audible distance");

        int handle = engine.play(category, DEFERRED_EVENT, new Point(SoundEngine.MAX_ELIGIBLE_DISTANCE, 0));
        assertNotEquals(SoundEngine.NO_HANDLE, handle, "a deferred sound is accepted within the distance");
        engine.stop(handle);
    }

    @Test
    void distantPositionalOneShotsAreNotKeptForTheListener() {
        engine.setListener(new Point(0, 0));

        assertEquals(SoundEngine.NO_HANDLE, engine.play(category, EVENT, distant()),
                "a distant one-shot is not accepted");

        engine.setListener(distant());
        engine.tick();

        assertEquals(0, backends.size(), "the sound must not turn up when the listener arrives later");
    }

    @Test
    void queuedOneShotIsDroppedWhenTheListenerIsFarAway() {
        engine.setListener(new Point(0, 0));

        int handle = engine.play(category, EVENT, new Point(1, 0));
        engine.setListener(distant());
        engine.tick();

        assertFalse(engine.isPlaying(handle), "the one-shot went out of earshot before it was realised");
        assertEquals(0, backends.size());

        engine.setListener(new Point(1, 0));
        engine.tick();
        assertEquals(0, backends.size(), "and it does not come back");
    }

    @Test
    void distantLoopingBedsWaitForTheListener() {
        engine.setListener(new Point(0, 0));

        int handle = engine.play(category, LOOP_EVENT, distant());
        engine.tick();

        assertTrue(engine.isPlaying(handle), "the bed is queued, not finished");
        assertEquals(0, backends.size());

        engine.setListener(distant());
        engine.tick();

        assertEquals(1, backends.size(), "the bed starts once the listener is close enough");
        engine.stop(handle);
    }

    private static Point distant() {
        return new Point(SoundEngine.MAX_ELIGIBLE_DISTANCE + 10, 0);
    }

    private SoundVoice voiceOf(int handle) {
        SoundVoice voice = engine.getVoice(handle);
        if (voice == null) {
            throw new AssertionError("no voice for handle " + handle);
        }
        return voice;
    }

    private static SoundCategory category() {
        Map<Integer, SoundEvent> events = new HashMap<>();
        events.put(EVENT, new SoundEvent(EVENT, 100, EnumSet.noneOf(SoundEventFlag.class), List.of(group())));
        events.put(LOOP_EVENT, new SoundEvent(LOOP_EVENT, 100, EnumSet.of(SoundEventFlag.REPEATING, SoundEventFlag.LOOP_EACH), List.of(group())));
        events.put(DEFERRED_EVENT, new SoundEvent(DEFERRED_EVENT, 100, EnumSet.of(SoundEventFlag.DEFERRED), List.of(group())));
        // channel 0 is the master channel, channel 1 the music channel
        return new SoundCategory("TEST", 0b11, events);
    }

    private static SoundEventGroup group() {
        SoundArchive archive = new SoundArchive("Global" + File.separator + "Test", EnumSet.of(BankFlag.RESIDENT), null,
                List.of("clip1.mp2"));
        SoundSample sample = new SoundSample(1, 100, 1000, 0, archive);
        return new SoundEventGroup(100, 100, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0.0f, List.of(sample), List.of());
    }

    /**
     * Backend that records what the engine asked it to do.
     */
    private static final class RecordingBackend implements AudioBackend {

        private final AudioSpec spec;
        private Status status = Status.STOPPED;
        private float gain;
        private boolean played;

        RecordingBackend(AudioSpec spec) {
            this.spec = spec;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public void play() {
            played = true;
            status = Status.PLAYING;
        }

        @Override
        public void pause() {
            status = Status.PAUSED;
        }

        @Override
        public void stop() {
            status = Status.STOPPED;
        }

        @Override
        public void setGain(float gain) {
            this.gain = gain;
        }

        @Override
        public void setPitch(float pitch) {
        }

        @Override
        public void setLooping(boolean looping) {
        }

        @Override
        public void setTimeOffset(float seconds) {
        }

        @Override
        public float getPlaybackTime() {
            return 0;
        }

        @Override
        public float getDuration() {
            return spec.streamed() ? Float.MAX_VALUE : 1.0f;
        }
    }
}