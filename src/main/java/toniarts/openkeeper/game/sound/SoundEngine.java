/*
 * Copyright (C) 2014-2025 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.sound;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.Point;

/**
 * The sound engine: turns category and event requests into playing voices.
 * <p>
 * It owns a fixed size pool of deferred voices, a bounded number of actually
 * audible voices, generation checked handles, the slow frame throttle and the
 * per channel volumes.
 */
public class SoundEngine {

    private static final Logger logger = System.getLogger(SoundEngine.class.getName());

    public static final int NO_HANDLE = 0;
    public static final int MAX_VOICES = 12;
    public static final int MAX_AUDIBLE_VOICES = 8;

    /**
     * Positional voices further away than this many tiles are not realised.
     * The paper states this as a squared game unit distance; here it is applied
     * to the tile distance that the game itself uses.
     */
    public static final int MAX_ELIGIBLE_DISTANCE = 6;
    public static final int FADE_IN_MS = 250;
    public static final int FADE_OUT_MS = 250;
    public static final int CHANNEL_COUNT = 8;
    // the camera hovers well above the ground, so a source on the ground must not start to fade right away
    private static final float REF_DISTANCE = 8.0f;

    private final AudioBackend.AudioBackendFactory factory;
    private final SoundThrottle throttle = new SoundThrottle();

    private final Map<Integer, SoundVoice> voices = new LinkedHashMap<>();
    private final List<SoundVoice> pending = new ArrayList<>();
    private final ArrayDeque<SoundVoice> deferred = new ArrayDeque<>(MAX_VOICES);

    private final int[] slotGenerations = new int[MAX_VOICES];
    private final int[] channelVolume = new int[CHANNEL_COUNT];
    private final boolean[] channelMuted = new boolean[CHANNEL_COUNT];

    private int masterVolume = 100;
    private int generationCounter = 1;
    private Point listener = new Point();
    private boolean paused;

    public SoundEngine(AudioBackend.AudioBackendFactory factory) {
        this.factory = factory;
        java.util.Arrays.fill(channelVolume, 100);
    }

    /**
     * Plays a stereo sound.
     *
     * @return a generation checked handle, {@link #NO_HANDLE} on failure
     */
    public int play(SoundCategory category, int eventId) {
        return play(category, eventId, null, -1);
    }

    /**
     * Plays a sound at a world position.
     *
     * @param position world position, or {@code null} for a stereo sound
     */
    public int play(SoundCategory category, int eventId, @Nullable Point position) {
        return play(category, eventId, position, -1);
    }

    /**
     * Plays a sound, resolving property dependent groups.
     *
     * @param property the property to match links against, {@code -1} for none
     */
    public int play(SoundCategory category, int eventId, @Nullable Point position, int property) {
        if (category == null || paused) {
            return NO_HANDLE;
        }
        SoundEvent event = category.getEvent(eventId);
        if (event == null) {
            logger.log(Level.DEBUG, "Sound event 0x{0} is not defined in category {1}",
                    new Object[]{Integer.toHexString(eventId), category.getName()});
            return NO_HANDLE;
        }
        if (!throttle.allow(eventId)) {
            return NO_HANDLE;
        }
        // a deferred sound beyond the audible distance is not accepted at all
        if (position != null && event.isDeferred() && !isEligible(position)) {
            return NO_HANDLE;
        }

        SoundVoice voice = createVoice(category, event, position, property);
        if (voice == null) {
            return NO_HANDLE;
        }
        if (event.isDeferred()) {
            return defer(voice);
        }
        pending.add(voice);
        // register immediately, so that handles can be stopped or queried before the voice is realised
        voices.put(voice.getHandle(), voice);
        return voice.getHandle();
    }

    @Nullable
    private SoundVoice createVoice(SoundCategory category, SoundEvent event, @Nullable Point position, int property) {
        int slot = allocateSlot();
        if (slot < 0) {
            return null;
        }

        SoundEventGroup group = selectGroup(event, property);
        if (group == null) {
            return null;
        }
        SoundSample sample = group.pickSample();
        if (sample == null || sample.getAssetPath() == null) {
            releaseSlot(slot);
            return null;
        }

        float distance = 0.0f;
        if (position != null) {
            distance = (float) listener.distance(position);
        }

        return new SoundVoice(slotGenerations[slot], slot, category, event, group, sample, SoundRng.volume(group),
                SoundRng.pitch(group) / 100.0f, position, group.getMaxDistance(), distance,
                now() + SoundRng.delay(group));
    }

    @Nullable
    private SoundEventGroup selectGroup(SoundEvent event, int property) {
        List<SoundEventGroup> groups = event.getGroups();
        if (groups.isEmpty()) {
            return null;
        }
        SoundEventGroup primary = groups.get(0);
        if (property < 0 || !event.hasPropertyLinks()) {
            return primary;
        }

        List<SoundEventGroup> candidates = new ArrayList<>();
        for (SoundEventGroup group : primary.resolve()) {
            for (SoundLink link : group.getLinks()) {
                if (link.getTarget() == group || link.matchesProperty(property)) {
                    candidates.add(group);
                    break;
                }
            }
        }
        if (candidates.isEmpty()) {
            return primary;
        }
        return SoundRng.pick(candidates, SoundEventGroup::getCumulativeWeight);
    }

    private int defer(SoundVoice voice) {
        if (deferred.size() >= MAX_VOICES) {
            SoundVoice oldest = deferred.poll();
            if (oldest != null) {
                voices.remove(oldest.getHandle());
                releaseSlot(oldest.getSlot());
            }
        }
        deferred.add(voice);
        voices.put(voice.getHandle(), voice);
        return voice.getHandle();
    }

    /**
     * Stops the voice, fading it out.
     */
    public void stop(int handle) {
        SoundVoice voice = voices.get(handle);
        if (voice != null) {
            voice.setStopped(true);
        }
    }

    /**
     * Stops every voice of an event, fading them out.
     */
    public void stopAll(SoundCategory category, int eventId) {
        for (SoundVoice voice : voices.values()) {
            if (voice.getCategory() == category && voice.getEvent().getId() == eventId) {
                voice.setStopped(true);
            }
        }
    }

    /**
     * Stops a looping sound, i.e. background music or an ambience bed.
     */
    public void stopLooping(SoundCategory category, int eventId) {
        stopAll(category, eventId);
    }

    public void stopAll() {
        for (SoundVoice voice : voices.values()) {
            voice.setStopped(true);
        }
        pending.clear();
        deferred.clear();
        for (SoundVoice voice : voices.values()) {
            if (voice.getBackend() == null) {
                // not realised yet, there is nothing to fade out
                releaseSlot(voice.getSlot());
            }
        }
        voices.clear();
    }

    /**
     * @return whether the voice of the handle exists and has not finished yet,
     * which includes voices that are still queued
     */
    public boolean isPlaying(int handle) {
        SoundVoice voice = voices.get(handle);
        return voice != null && !voice.isFinished();
    }

    public int getActiveVoiceCount() {
        return voices.size();
    }

    /**
     * @return the voice of the handle, or {@code null} if the handle is not
     * (or no longer) valid
     */
    @Nullable
    public SoundVoice getVoice(int handle) {
        return voices.get(handle);
    }

    public void setListener(Point position) {
        listener = position;
    }

    public Point getListener() {
        return listener;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Switches the slow frame throttle off, which is what speech playback does
     * around its own call.
     */
    public void setThrottleEnabled(boolean enabled) {
        throttle.setEnabled(enabled);
    }

    public boolean isThrottleEnabled() {
        return throttle.isEnabled();
    }

    public boolean isPaused() {
        return paused;
    }

    public int getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(int volume) {
        masterVolume = SoundRng.clampVolume(volume);
    }

    public int getChannelVolume(int channel) {
        return channelVolume[channel];
    }

    public void setChannelVolume(int channel, int volume) {
        channelVolume[channel] = SoundRng.clampVolume(volume);
    }

    public void setChannelMuted(int channel, boolean muted) {
        channelMuted[channel] = muted;
    }

    /**
     * Advances fades, starts deferred voices and reaps finished ones.
     */
    public void tick() {
        long now = now();

        throttle.tick(now);
        realisePending(now);
        startDeferred(now);

        Iterator<Map.Entry<Integer, SoundVoice>> it = voices.entrySet().iterator();
        while (it.hasNext()) {
            SoundVoice voice = it.next().getValue();
            if (!update(voice, now)) {
                it.remove();
                releaseSlot(voice.getSlot());
            }
        }
    }

    private boolean update(SoundVoice voice, long now) {
        AudioBackend backend = voice.getBackend();
        if (backend == null) {
            // not realised yet, it only disappears if it was stopped
            return !voice.isStopped();
        }

        if (backend.getStatus() == AudioBackend.Status.STOPPED && voice.isStopped()) {
            backend.stop();
            return false;
        }

        if (voice.isFinished()) {
            backend.stop();
            return false;
        }

        float fade = 1.0f;
        long age = now - voice.getStartTime();
        // short one-shots such as clicks and slaps would be almost inaudible when faded in
        if (age < FADE_IN_MS) {
        //if (age < FADE_IN_MS && voice.getSample().getDurationMs() > 4 * FADE_IN_MS) {
            fade = Math.max(0.0f, (float) age / FADE_IN_MS);
        }
        if (voice.isStopped()) {
            long fadeOut = now - voice.getStopTime();
            fade *= Math.max(0.0f, 1.0f - (float) fadeOut / FADE_OUT_MS);
            if (fadeOut >= FADE_OUT_MS) {
                backend.stop();
                return false;
            }
        }

        voice.setGain(fade * effectiveGain(voice));
        backend.setGain(voice.getGain());
        return true;
    }

    /**
     * Volume of the voice as a 0..1 gain, after the channels that affect it.
     */
    public float effectiveGain(SoundVoice voice) {
        float gain = voice.getVolume() / 100.0f * (masterVolume / 100.0f);
        int mask = voice.getChannelMask();
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if ((mask & (1 << channel)) == 0) {
                continue;
            }
            if (channelMuted[channel]) {
                return 0.0f;
            }
            gain *= channelVolume[channel] / 100.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, gain));
    }

    private void realisePending(long now) {
        int audible = countAudible();
        if (audible >= MAX_AUDIBLE_VOICES || pending.isEmpty()) {
            return;
        }
        pending.sort(Comparator.comparingLong(SoundVoice::getRank).reversed());
        List<SoundVoice> deferredBack = new ArrayList<>();
        while (audible < MAX_AUDIBLE_VOICES && !pending.isEmpty()) {
            SoundVoice voice = pending.remove(0);
            // distant voices stay queued until the listener comes closer
            if (!isEligible(voice)) {
                deferredBack.add(voice);
                continue;
            }
            if (start(voice, now)) {
                audible++;
            }
        }
        pending.addAll(0, deferredBack);
    }

    /**
     * Whether a positional voice is close enough to be heard at all.
     */
    private boolean isEligible(SoundVoice voice) {
        return isEligible(voice.getPosition());
    }

    private boolean isEligible(@Nullable Point position) {
        if (position == null) {
            return true;
        }
        return listener.distance(position) <= MAX_ELIGIBLE_DISTANCE;
    }

    private int countAudible() {
        int count = 0;
        for (SoundVoice voice : voices.values()) {
            if (voice.isRealised() && !voice.isStopped()) {
                count++;
            }
        }
        return count;
    }

    private boolean start(SoundVoice voice, long now) {
        AudioBackend backend = createBackend(voice);
        if (backend == null) {
            voices.remove(voice.getHandle());
            releaseSlot(voice.getSlot());
            return false;
        }
        backend.setGain(0.0f);
        backend.play();
        voice.setBackend(backend);
        voice.setRealised(true);
        voice.setGain(effectiveGain(voice));
        backend.setGain(voice.getGain());
        voices.put(voice.getHandle(), voice);
        return true;
    }

    @Nullable
    private AudioBackend createBackend(SoundVoice voice) {
        SoundSample sample = voice.getSample();
        String path = sample.getAssetPath();
        if (path == null) {
            return null;
        }

        boolean looping = voice.getEvent().getBehaviour() == SoundVoiceBehaviour.REPEAT_LOOP_EACH;
        boolean streamed = looping || sample.getDurationMs() > 20_000;
        AudioSpec spec = AudioSpec.stereo(AssetsConverter.SOUNDS_FOLDER + '/' + path, 1.0f, looping, streamed)
                .withPitch(voice.getPitch());
        if (voice.isPositional()) {
            Point position = voice.getPosition();
            assert position != null;
            spec = spec.withPosition(position.x, 0.0f, position.y);
        }
        if (voice.getMaxDistance() > 0.0f) {
            spec = new AudioSpec(spec.assetPath(), spec.positional(), spec.looping(), spec.streamed(), spec.gain(),
                    spec.pitch(), spec.x(), spec.y(), spec.z(), REF_DISTANCE, voice.getMaxDistance());
        }
        try {
            return factory.create(spec);
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, () -> "Failed to play " + path, ex);
            return null;
        }
    }

    private void startDeferred(long now) {
        while (!deferred.isEmpty()) {
            SoundVoice voice = deferred.peek();
            if (voice.getStartTime() > now) {
                break;
            }
            deferred.poll();
            if (voice.isStopped()) {
                voices.remove(voice.getHandle());
                releaseSlot(voice.getSlot());
                continue;
            }
            pending.add(voice);
        }
    }

    private int allocateSlot() {
        for (int slot = 0; slot < MAX_VOICES; slot++) {
            if (slotGenerations[slot] == 0) {
                slotGenerations[slot] = generationCounter++;
                if (generationCounter == 0xFFFF) {
                    generationCounter = 1;
                }
                return slot;
            }
        }
        return -1;
    }

    private void releaseSlot(int slot) {
        slotGenerations[slot] = 0;
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    /**
     * Per event statistics, mainly useful for tests and debugging.
     */
    public Map<Integer, Integer> getEventPlayCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (SoundVoice voice : voices.values()) {
            counts.merge(voice.getEvent().getId(), 1, Integer::sum);
        }
        return counts;
    }
}