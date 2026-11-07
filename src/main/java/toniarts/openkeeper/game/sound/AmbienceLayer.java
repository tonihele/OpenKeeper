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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.SoundVoice.Behaviour;
import toniarts.openkeeper.utils.Point;

/**
 * One never ending background sound: a single event that is kept alive for as
 * long as the layer exists.
 * <p>
 * The engine plays one clip per voice, so the continuous behaviours of the
 * original are built here, out of voices:
 * <ul>
 * <li>a repeater starts a new clip every {@value #REPEATER_GAP_MS} ms plus the
 * random delay of its group, and lets the old clip play out,</li>
 * <li>a segmented event starts the next segment just before the previous one
 * ends (plus the delay of the group, if it has one),</li>
 * <li>a loop is simply restarted when its voice is gone.</li>
 * </ul>
 * Which group and clip comes next is decided by the engine, exactly as for any
 * other voice.
 */
final class AmbienceLayer {

    private static final int REPEATER_GAP_MS = 250;
    private static final int RETRY_MS = 1000;

    /**
     * Voices that are queued but never get to play must not eat all the slots.
     */
    private static final int MAX_LIVE_VOICES = 3;

    private final SoundEngine engine;
    private final SoundCategory category;
    private final int eventId;
    private final IntSupplier property;
    private final ArrayDeque<Integer> live = new ArrayDeque<>();

    @Nullable
    private Point position;
    @Nullable
    private Float volumeScale;
    private long nextAt;
    private boolean scheduled;
    private boolean ended;
    private boolean stopped;

    AmbienceLayer(SoundEngine engine, SoundCategory category, int eventId, @Nullable Point position, int property) {
        this(engine, category, eventId, position, () -> property);
    }

    /**
     * @param property supplies the property to play the event with, e.g. the
     * music intensity
     */
    AmbienceLayer(SoundEngine engine, SoundCategory category, int eventId, @Nullable Point position,
            IntSupplier property) {
        this.engine = engine;
        this.category = category;
        this.eventId = eventId;
        this.position = position;
        this.property = property;
    }

    SoundCategory getCategory() {
        return category;
    }

    /**
     * Moves the layer, i.e. everything it plays now and in the future.
     */
    void move(Point position) {
        if (position.equals(this.position)) {
            return;
        }
        this.position = position;
        for (int handle : live) {
            engine.move(handle, position);
        }
    }

    /**
     * Scales the volume of the voices, relative to the volume of their group.
     *
     * @param scale 0..1, {@code null} leaves the group volume alone
     */
    void setVolumeScale(@Nullable Float scale) {
        this.volumeScale = scale;
        applyVolume();
    }

    /**
     * Fades all the voices of the layer out, the layer is not usable afterwards.
     */
    void stop() {
        stopped = true;
        for (int handle : live) {
            engine.stop(handle);
        }
        live.clear();
    }

    boolean isStopped() {
        return stopped;
    }

    int getLiveCount() {
        return live.size();
    }

    void tick(long now) {
        if (stopped || ended) {
            return;
        }
        prune();

        SoundEvent event = category.getEvent(eventId);
        if (event == null) {
            ended = true;
            return;
        }

        boolean due;
        if (nextAt == 0) {
            due = true;
        } else if (scheduled) {
            due = now >= nextAt;
        } else {
            due = live.isEmpty() && now >= nextAt;
        }
        if (due) {
            spawn(now, event);
        }
        applyVolume();
    }

    private void spawn(long now, SoundEvent event) {
        if (live.size() >= MAX_LIVE_VOICES) {
            retryLater(now);
            return;
        }

        // A property linked event has nothing to play while no link matches,
        // as the ambience under louder music
        int currentProperty = property.getAsInt();
        if (event.hasPropertyLinks() && !hasMatchingLink(event, currentProperty)) {
            retryLater(now);
            return;
        }

        int handle = engine.play(category, eventId, position, currentProperty);
        SoundVoice voice = handle == SoundEngine.NO_HANDLE ? null : engine.getVoice(handle);
        if (voice == null) {
            retryLater(now);
            return;
        }
        live.add(handle);

        SoundEventGroup group = voice.getGroup();
        int delay = SoundRng.delay(group);
        if (SoundEngine.chainsItself(event)) {
            // The engine plays the following clips of the voice, drawing the groups and the delays itself. The
            // layer must not start more voices, each of them would chain on its own and all would sound at once.
            // It only has to start a new voice if the chain has ended.
            if (event.getBehaviour() == Behaviour.REPEAT && delay <= 0) {
                // a repeater without a delay ends after its first clip
                ended = true;
                return;
            }
            scheduled = false;
            nextAt = now;
            return;
        }
        switch (event.getBehaviour()) {
            case REPEAT -> {
                if (delay <= 0) {
                    // a repeater without a delay ends after its first clip
                    ended = true;
                    return;
                }
                scheduled = true;
                nextAt = now + REPEATER_GAP_MS + delay;
            }
            case REPEAT_LOOP_EACH -> {
                // the voice loops by itself, the layer only has to replace it if it goes away
                scheduled = false;
                nextAt = now;
            }
            default -> {
                int duration = voice.getSample().getDurationMs();
                if (duration <= 0) {
                    scheduled = false;
                    nextAt = now;
                    return;
                }
                // without a gap the next segment fades in under the end of this one
                long lead = delay == 0 ? SoundEngine.FADE_IN_MS : 0;
                scheduled = true;
                nextAt = now + Math.max(duration + delay - lead, REPEATER_GAP_MS);
            }
        }
    }

    private static boolean hasMatchingLink(SoundEvent event, int property) {
        for (SoundEventGroup group : event.getGroups()) {
            for (SoundLink link : group.getLinks()) {
                if (link.matchesProperty(property)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void retryLater(long now) {
        scheduled = true;
        nextAt = now + RETRY_MS;
    }

    private void prune() {
        Iterator<Integer> it = live.iterator();
        while (it.hasNext()) {
            if (!engine.isPlaying(it.next())) {
                it.remove();
            }
        }
    }

    private void applyVolume() {
        if (volumeScale == null) {
            return;
        }
        for (int handle : live) {
            SoundVoice voice = engine.getVoice(handle);
            if (voice != null) {
                voice.setVolume(Math.round(voice.getGroup().getVolumeMax() * volumeScale));
            }
        }
    }
}
