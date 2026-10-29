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
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.SoundVoice.Behaviour;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.AssetUtils;
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
    public static final int MAX_VOICES = 32;
    public static final int MAX_AUDIBLE_VOICES = 24;

    /**
     * Positional voices further away than this many tiles are not realised.
     * The distance is measured between tile coordinates, not world units.
     */
    public static final int MAX_ELIGIBLE_DISTANCE = 12;
    /**
     * A one-shot that has not got a voice by then is dropped, it would only be late
     */
    private static final long MAX_PENDING_AGE_MS = 500;
    public static final int FADE_IN_MS = 250;
    public static final int FADE_OUT_MS = 250;
    public static final int CHANNEL_COUNT = 16;
    // sounds are as loud as they get within this many tiles (on the ground) of the listener
    private static final float REF_DISTANCE = 12.0f;

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
    public synchronized int play(SoundCategory category, int eventId, @Nullable Point position, int property) {
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
        // a sound beyond the audible distance is not accepted at all, only the repeating beds wait for the listener
        boolean repeating = event.getBehaviour() == Behaviour.REPEAT
                || event.getBehaviour() == Behaviour.REPEAT_LOOP_EACH;
        if (position != null && (event.isDeferred() || !repeating) && !isEligible(position)) {
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

    /**
     * Plays a single clip as it is, bypassing the event rules, i.e. for
     * previewing in the tools. Not tracked, so it can not be stopped.
     */
    public synchronized void playClip(String assetPath) {
        AudioBackend backend = factory.create(AudioSpec.stereo(AssetUtils.getCanonicalAssetKey(assetPath), masterVolume / 100.0f));
        if (backend != null) {
            backend.play();
        }
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
    public synchronized void stop(int handle) {
        SoundVoice voice = voices.get(handle);
        if (voice != null) {
            voice.setStopped(true);
        }
    }

    /**
     * Stops every voice of an event, fading them out.
     */
    public synchronized void stopAll(SoundCategory category, int eventId) {
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

    public synchronized void stopAll() {
        for (SoundVoice voice : voices.values()) {
            voice.setStopped(true);
            voice.stopChildren();
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
    public synchronized boolean isPlaying(int handle) {
        // Called from the game logic thread too, so this must not ask the audio device: voices that
        // have played to their end are removed by the tick, which runs on the render thread
        SoundVoice voice = voices.get(handle);
        return voice != null && !voice.isStopped();
    }

    public synchronized int getActiveVoiceCount() {
        return voices.size();
    }

    /**
     * @return the voice of the handle, or {@code null} if the handle is not
     * (or no longer) valid
     */
    @Nullable
    public synchronized SoundVoice getVoice(int handle) {
        return voices.get(handle);
    }

    /**
     * Moves a positional voice.
     *
     * @return whether the voice still exists, i.e. {@code false} if the handle
     * is stale and the caller has to start a new voice
     */
    public synchronized boolean move(int handle, Point position) {
        SoundVoice voice = voices.get(handle);
        if (voice == null || voice.isStopped()) {
            return false;
        }
        if (voice.isPositional()) {
            voice.setPosition(position);
        }
        return true;
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
    public synchronized void tick() {
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
            voice.stopChildren();
            return false;
        }

        if (voice.isChained() && !voice.isStopped()) {
            updateChain(voice, now);
        }

        if (voice.isFinished()) {
            backend.stop();
            voice.stopChildren();
            return false;
        }

        float fade = 1.0f;
        long age = now - voice.getStartTime();
        // one-shots must start at once, only beds and music are faded in
        if (age < FADE_IN_MS && voice.getEvent().getBehaviour() != Behaviour.ONE_SHOT) {
            fade = Math.max(0.0f, (float) age / FADE_IN_MS);
        }
        if (voice.isStopped()) {
            long fadeOut = now - voice.getStopTime();
            fade *= Math.max(0.0f, 1.0f - (float) fadeOut / FADE_OUT_MS);
            if (fadeOut >= FADE_OUT_MS) {
                backend.stop();
                voice.stopChildren();
                return false;
            }
        }

        voice.setGain(fade * effectiveGain(voice));
        backend.setGain(voice.getGain());
        for (SoundVoice.Child child : voice.getChildren()) {
            child.backend().setGain(fade * effectiveGain(voice, child.volume()));
        }
        return true;
    }

    private enum Chain {
        NONE,
        REPEATER,
        SEGMENTED_LOOP
    }

    /**
     * Whether the engine itself plays the clips of the event one after another once the event is started, see
     * {@link #chainOf}.
     */
    static boolean chainsItself(SoundEvent event) {
        return chainOf(event) != Chain.NONE;
    }

    /**
     * Repeaters and segmented loops play a clip after another, they never end by themselves.
     */
    private static Chain chainOf(SoundEvent event) {
        Set<SoundEventFlag> flags = event.getFlags();
        if (!flags.contains(SoundEventFlag.REPEATING)
                || flags.contains(SoundEventFlag.PROPERTY_LINKS)
                || flags.contains(SoundEventFlag.LOOP_EACH)
                || flags.contains(SoundEventFlag.LINEAR_SEQUENCE)
                || flags.contains(SoundEventFlag.BACKEND_EVENT)) {
            return Chain.NONE;
        }
        return flags.contains(SoundEventFlag.SEGMENTED) ? Chain.SEGMENTED_LOOP : Chain.REPEATER;
    }

    /**
     * A repeater starts its next clip 250 ms plus a random delay after the start of the previous one (and ends
     * when that delay is 0), a segmented loop starts the next clip when the previous one ends.
     */
    private void updateChain(SoundVoice voice, long now) {
        // Forget the clips that have ended
        voice.getChildren().removeIf(child -> {
            AudioBackend childBackend = child.backend();
            float duration = childBackend.getDuration();
            boolean ended = childBackend.getStatus() == AudioBackend.Status.STOPPED
                    || (duration > 0.0f && childBackend.getPlaybackTime() >= duration);
            if (ended) {
                childBackend.stop();
            }
            return ended;
        });

        if (voice.isChainEnded() || now < voice.getChainNextAt()) {
            return;
        }
        Chain chain = chainOf(voice.getEvent());
        if (chain == Chain.REPEATER && voice.getChainDelay() == 0) {
            voice.setChainEnded(true);
            return;
        }

        List<SoundEventGroup> groups = voice.getEvent().getGroups();
        SoundEventGroup group = groups.size() > 1
                ? SoundRng.pick(groups, SoundEventGroup::getCumulativeWeight) : groups.get(0);
        SoundSample sample = group.pickSample();
        if (sample == null) {
            voice.setChainEnded(true);
            return;
        }
        AudioBackend childBackend = createBackend(voice, sample, SoundRng.pitch(group) / 100.0f);
        if (childBackend != null) {
            int volume = SoundRng.volume(group);
            childBackend.setGain(effectiveGain(voice, volume));
            childBackend.play();
            voice.getChildren().add(new SoundVoice.Child(childBackend, volume));
        }
        scheduleNextChild(voice, chain, group, sample, now);
    }

    private static void scheduleNextChild(SoundVoice voice, Chain chain, SoundEventGroup group, SoundSample sample,
            long now) {
        int delay = SoundRng.delay(group);
        voice.setChainDelay(delay);
        voice.setChainNextAt(now + (chain == Chain.REPEATER ? 250L + delay : Math.max(1, sample.getDurationMs())));
    }

    /**
     * Volume of the voice as a 0..1 gain, after the channels that affect it.
     */
    public float effectiveGain(SoundVoice voice) {
        return effectiveGain(voice, voice.getVolume());
    }

    private float effectiveGain(SoundVoice voice, int volume) {
        float gain = volume / 100.0f * (masterVolume / 100.0f);
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
        dropStalePending(now);
        int audible = countAudible();
        if (audible >= MAX_AUDIBLE_VOICES || pending.isEmpty()) {
            return;
        }
        pending.sort(Comparator.comparingLong(SoundVoice::getRank).reversed());
        List<SoundVoice> deferredBack = new ArrayList<>();
        while (audible < MAX_AUDIBLE_VOICES && !pending.isEmpty()) {
            SoundVoice voice = pending.remove(0);
            // distant beds stay queued until the listener comes closer
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
     * One-shots are only worth playing at the moment they happen. If one is out of earshot, or has waited for a
     * free voice for too long, it is gone, not kept around to play when the listener arrives later. Only the
     * repeating beds, which are supposed to be there as long as the thing is, stay queued.
     */
    private void dropStalePending(long now) {
        Iterator<SoundVoice> it = pending.iterator();
        while (it.hasNext()) {
            SoundVoice voice = it.next();
            if (isRepeating(voice) || voice.isStopped()) {
                continue;
            }
            if (!isEligible(voice) || now - voice.getStartTime() > MAX_PENDING_AGE_MS) {
                it.remove();
                voices.remove(voice.getHandle());
                releaseSlot(voice.getSlot());
            }
        }
    }

    private static boolean isRepeating(SoundVoice voice) {
        Behaviour behaviour = voice.getEvent().getBehaviour();
        return behaviour == Behaviour.REPEAT || behaviour == Behaviour.REPEAT_LOOP_EACH;
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
        Chain chain = chainOf(voice.getEvent());
        if (chain != Chain.NONE) {
            voice.setChained(true);
            scheduleNextChild(voice, chain, voice.getGroup(), voice.getSample(), now);
        }
        voices.put(voice.getHandle(), voice);
        return true;
    }

    @Nullable
    private AudioBackend createBackend(SoundVoice voice) {
        return createBackend(voice, voice.getSample(), voice.getPitch());
    }

    @Nullable
    private AudioBackend createBackend(SoundVoice voice, SoundSample sample, float pitch) {
        String path = sample.getAssetPath();
        if (path == null) {
            return null;
        }

        boolean looping = voice.getEvent().getBehaviour() == Behaviour.REPEAT_LOOP_EACH;
        boolean streamed = looping || sample.getDurationMs() > 20_000;
        AudioSpec spec = AudioSpec.stereo(AssetsConverter.SOUNDS_FOLDER + '/' + path, 1.0f, looping, streamed)
                .withPitch(pitch);
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
    public synchronized Map<Integer, Integer> getEventPlayCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (SoundVoice voice : voices.values()) {
            counts.merge(voice.getEvent().getId(), 1, Integer::sum);
        }
        return counts;
    }
}