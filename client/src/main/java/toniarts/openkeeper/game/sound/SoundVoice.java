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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import toniarts.openkeeper.utils.Point;

/**
 * One playing sound: the chosen clip with its volume, pitch and position.
 */
public class SoundVoice {

    private final int generation;
    private final int slot;
    private final SoundEvent event;
    private final SoundCategory category;
    private final SoundEventGroup group;
    private final SoundSample sample;
    private final long startTime;
    private final float distanceToListener;
    private final int channelMask;
    @Nullable
    private Point position;
    private final float maxDistance;

    private int volume;
    private float pitch;
    private float gain;
    private boolean realised;
    private boolean stopped;
    private long stopTime;

    @Nullable
    private AudioBackend backend;

    /**
     * A chained voice (a repeater or a segmented loop) plays one clip after another, the clips that follow the
     * first one are here.
     */
    private final List<Child> children = new ArrayList<>();
    private boolean chained;
    private boolean chainEnded;
    private long chainNextAt;
    private int chainDelay;

    public SoundVoice(int generation, int slot, SoundCategory category, SoundEvent event, SoundEventGroup group,
            SoundSample sample, int volume, float pitch, @Nullable Point position, float maxDistance,
            float distanceToListener, long startTime) {
        this.generation = generation;
        this.slot = slot;
        this.category = category;
        this.event = event;
        this.group = group;
        this.sample = sample;
        this.volume = volume;
        this.pitch = pitch;
        this.gain = 0.0f;
        this.position = position;
        this.maxDistance = maxDistance;
        this.distanceToListener = distanceToListener;
        this.channelMask = category.getChannelMask();
        this.startTime = startTime;
    }

    /**
     * Generation checked handle, {@code 0} is never a valid handle.
     */
    public int getHandle() {
        return (generation << 16) | slot;
    }

    public int getGeneration() {
        return generation;
    }

    public int getSlot() {
        return slot;
    }

    public SoundCategory getCategory() {
        return category;
    }

    public SoundEvent getEvent() {
        return event;
    }

    public SoundEventGroup getGroup() {
        return group;
    }

    public SoundSample getSample() {
        return sample;
    }

    public int getChannelMask() {
        return channelMask;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isRealised() {
        return realised;
    }

    void setRealised(boolean realised) {
        this.realised = realised;
    }

    public boolean isStopped() {
        return stopped;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
        if (stopped) {
            stopTime = System.nanoTime() / 1_000_000L;
        }
    }

    @Nullable
    public Point getPosition() {
        return position;
    }

    /**
     * Moves a positional voice, also while it is playing.
     */
    public void setPosition(Point position) {
        this.position = position;
        if (backend != null) {
            backend.setPosition(position.x, 0.0f, position.y);
        }
    }

    public boolean isPositional() {
        return position != null;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public float getDistanceToListener() {
        return distanceToListener;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = SoundRng.clampVolume(volume);
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(0.01f, pitch);
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = Math.max(0.0f, Math.min(1.0f, gain));
    }

    @Nullable
    public AudioBackend getBackend() {
        return backend;
    }

    void setBackend(@Nullable AudioBackend backend) {
        this.backend = backend;
    }

    /**
     * Ranking used to decide which of the virtual voices may actually play:
     * louder and more important and closer wins.
     */
    public long getRank() {
        long rank = (long) event.getPriority() << 40;
        rank += (long) volume << 24;
        rank -= (long) (distanceToListener * 16.0f);
        return rank;
    }

    /**
     * Whether the voice reached the end of its clip. jME does not report an
     * "ended" state, so the playback position has to be compared with the
     * duration. A stream of unknown length never finishes.
     */
    public boolean isFinished() {
        if (stopped) {
            return true;
        }
        if (chained) {
            // never ends by itself, unless the chain has been told to end
            return chainEnded && children.isEmpty() && isBackendFinished();
        }
        return isBackendFinished();
    }

    private boolean isBackendFinished() {
        if (backend == null) {
            return false;
        }
        return switch (backend.getStatus()) {
            case STOPPED -> true;
            case PLAYING -> {
                if (event.getBehaviour() == Behaviour.REPEAT_LOOP_EACH) {
                    yield false;
                }
                float duration = backend.getDuration();
                yield duration > 0.0f && backend.getPlaybackTime() >= duration;
            }
            default -> false;
        };
    }

    boolean isChained() {
        return chained;
    }

    void setChained(boolean chained) {
        this.chained = chained;
    }

    boolean isChainEnded() {
        return chainEnded;
    }

    void setChainEnded(boolean chainEnded) {
        this.chainEnded = chainEnded;
    }

    long getChainNextAt() {
        return chainNextAt;
    }

    void setChainNextAt(long chainNextAt) {
        this.chainNextAt = chainNextAt;
    }

    int getChainDelay() {
        return chainDelay;
    }

    void setChainDelay(int chainDelay) {
        this.chainDelay = chainDelay;
    }

    List<Child> getChildren() {
        return children;
    }

    /**
     * Stops the clips that follow the first clip of a chained voice.
     */
    void stopChildren() {
        for (Child child : children) {
            child.backend().stop();
        }
        children.clear();
    }

    /**
     * Playback strategy of a single voice, derived from the event flags.
     */
    public enum Behaviour {

        ONE_SHOT,
        PLAY_ONCE_SEGMENTED,
        REPEAT,
        REPEAT_LOOP_EACH,
        LINEAR_SEQUENCE,
        BACKEND_EVENT
    }

    /**
     * A clip of a chained voice, with the volume of the group it came from.
     */
    record Child(AudioBackend backend, int volume) {

    }

    @Override
    public String toString() {
        return "SoundVoice[" + category.getName() + ":0x" + Integer.toHexString(event.getId()) + ", handle="
                + getHandle() + ", sample=" + sample + ']';
    }
}