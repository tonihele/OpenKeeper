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
import java.util.function.Function;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import toniarts.openkeeper.utils.Point;

/**
 * Everything that sounds in the background of a level, without anyone asking
 * for it. Four layers:
 * <ol>
 * <li>the ambience bed: long low drones and creaks, 2D,</li>
 * <li>the one-shot atmos: a distant drip, door or groan every now and then, 2D,</li>
 * <li>the room and terrain beds around the listener, see {@link AmbienceBeds},</li>
 * <li>the crackle of the nearest wall torch.</li>
 * </ol>
 * The first two do not depend on anything; the other two are told what is
 * around the listener on every update.
 */
public final class AmbienceDriver {

    private static final Logger logger = System.getLogger(AmbienceDriver.class.getName());

    public static final int AMBIENCE_EVENT = 0x155;
    public static final int ONE_SHOT_ATMOS_EVENT = 0x2EA;
    public static final int TORCH_EVENT = 0xFB;
    public static final String OBJECT_TORCH = "OBJECT_TORCH";

    /**
     * The "resting" intensity of the music. The one-shot atmos always plays at
     * it, the ambience follows the music intensity and drops out under louder
     * music.
     */
    private static final int BED_PROPERTY = 0;

    private final SoundEngine engine;
    private final Function<String, SoundCategory> loader;
    private final AmbienceBeds beds;
    private final IntSupplier musicIntensity;

    @Nullable
    private AmbienceLayer ambience;
    @Nullable
    private AmbienceLayer atmos;
    @Nullable
    private AmbienceLayer torch;
    @Nullable
    private Point torchPosition;
    private boolean started;
    private boolean torchMissing;

    public AmbienceDriver(SoundEngine engine, Function<String, SoundCategory> loader) {
        this(engine, loader, () -> BED_PROPERTY);
    }

    /**
     * @param musicIntensity the current music intensity, the property of the
     * ambience
     */
    public AmbienceDriver(SoundEngine engine, Function<String, SoundCategory> loader, IntSupplier musicIntensity) {
        this.engine = engine;
        this.loader = loader;
        this.musicIntensity = musicIntensity;
        this.beds = new AmbienceBeds(engine, loader);
    }

    /**
     * @param now time in milliseconds, as {@link System#nanoTime()} / 1000000
     * @param cells category names of the 11x11 tiles around the listener, row by
     * row, {@code null} for none. See {@link AmbienceBeds}
     * @param listener tile of the listener
     * @param nearestTorch tile of the nearest torch, {@code null} if there is none
     */
    public void update(long now, @Nullable String[] cells, Point listener, @Nullable Point nearestTorch) {
        if (!started) {
            started = true;
            ambience = createLayer(GlobalCategory.AMBIENCE, AMBIENCE_EVENT, musicIntensity);
            atmos = createLayer(GlobalCategory.ONE_SHOT_ATMOS, ONE_SHOT_ATMOS_EVENT, () -> BED_PROPERTY);
        }

        if (ambience != null) {
            ambience.tick(now);
        }
        if (atmos != null) {
            atmos.tick(now);
        }
        beds.update(now, cells, listener);
        updateTorch(now, listener, nearestTorch);
    }

    /**
     * Fades everything out.
     */
    public void stop() {
        for (AmbienceLayer layer : new AmbienceLayer[]{ambience, atmos, torch}) {
            if (layer != null) {
                layer.stop();
            }
        }
        ambience = null;
        atmos = null;
        torch = null;
        torchPosition = null;
        beds.stop();
        started = false;
        torchMissing = false;
    }

    @Nullable
    private AmbienceLayer createLayer(String categoryName, int eventId, IntSupplier property) {
        SoundCategory category = loader.apply(categoryName);
        if (category == null || !category.hasEvent(eventId)) {
            logger.log(Level.WARNING, "No ambience event 0x{0} in category {1}",
                    Integer.toHexString(eventId), categoryName);
            return null;
        }
        return new AmbienceLayer(engine, category, eventId, null, property);
    }

    private void updateTorch(long now, Point listener, @Nullable Point nearestTorch) {
        float scale = 0.0f;
        if (nearestTorch != null) {
            double distance = listener.distance(nearestTorch);
            scale = (float) Math.max(0.0, 1.0 - 0.25 * distance * distance);
        }
        if (scale <= 0.0f) {
            if (torch != null) {
                torch.stop();
                torch = null;
                torchPosition = null;
            }
            return;
        }

        if (torch == null) {
            SoundCategory category = torchMissing ? null : loader.apply(OBJECT_TORCH);
            if (category == null || !category.hasEvent(TORCH_EVENT)) {
                torchMissing = true;
                return;
            }
            torch = new AmbienceLayer(engine, category, TORCH_EVENT, nearestTorch, 0);
        } else if (!nearestTorch.equals(torchPosition)) {
            torch.move(nearestTorch);
        }
        torchPosition = nearestTorch;
        torch.setVolumeScale(scale);
        torch.tick(now);
    }

    /**
     * For tests.
     */
    AmbienceBeds getBeds() {
        return beds;
    }
}
