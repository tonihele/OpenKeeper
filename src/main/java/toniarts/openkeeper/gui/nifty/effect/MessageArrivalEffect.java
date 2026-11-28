/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.gui.nifty.effect;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.effects.Falloff;
import de.lessvoid.nifty.effects.impl.Move;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.render.NiftyRenderEngine;
import de.lessvoid.nifty.screen.Screen;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.sound.SoundManager;

/** Moves an arriving message and owns its start, slide and completion sounds. */
public final class MessageArrivalEffect extends Move {

    private final ArrivalSounds sounds;
    private Nifty nifty;
    private Element element;
    private Screen screen;
    private long completesAt;

    public MessageArrivalEffect() {
        this(new ArrivalSounds(event -> SoundManager.playSound(GlobalCategory.GUI_MESSAGE_BAR, event),
                SoundManager::isPlaying, SoundManager::stopSound));
    }

    MessageArrivalEffect(ArrivalSounds sounds) {
        this.sounds = sounds;
    }

    @Override
    public void activate(@Nonnull Nifty nifty, @Nonnull Element element, @Nonnull EffectProperties parameters) {
        super.activate(nifty, element, parameters);
        this.nifty = nifty;
        this.element = element;
        screen = nifty.getCurrentScreen();
        completesAt = nifty.getTimeProvider().getMsTime() + Long.parseLong(parameters.getProperty("length", "640"));
        sounds.start();
    }

    @Override
    public void execute(@Nonnull Element element, float normalizedTime, @Nullable Falloff falloff,
            @Nonnull NiftyRenderEngine renderEngine) {
        super.execute(element, normalizedTime, falloff, renderEngine);
        sounds.update();
    }

    @Override
    public void deactivate() {
        // Nifty also deactivates effects when they are interrupted. Only an
        // arrival that reaches its duration on the visible screen gets END 3.
        sounds.finish(nifty != null && nifty.getCurrentScreen() == screen
                && element.isVisibleWithParent() && nifty.getTimeProvider().getMsTime() >= completesAt);
        super.deactivate();
    }

    /** Stops this message's audio before dismissal, expiry or level cleanup. */
    public void cancel() {
        sounds.cancel();
    }

    static final class ArrivalSounds {

        private final IntUnaryOperator play;
        private final IntPredicate isPlaying;
        private final IntConsumer stop;
        private int handle;
        private boolean active;
        private boolean sliding;

        ArrivalSounds(IntUnaryOperator play, IntPredicate isPlaying, IntConsumer stop) {
            this.play = play;
            this.isPlaying = isPlaying;
            this.stop = stop;
        }

        void start() {
            cancel();
            active = true;
            handle = play.applyAsInt(GlobalType.GUI_MESSAGE_BAR_END.getId());
        }

        void update() {
            // Native requests share a voice handle and equal priority: the
            // slide is admitted after END 1, rather than cutting it short.
            if (active && !sliding && !isPlaying.test(handle)) {
                sliding = true;
                handle = play.applyAsInt(GlobalType.GUI_MESSAGE_BAR_SLIDE.getId());
            }
        }

        void finish(boolean completed) {
            if (!active) {
                return;
            }
            cancel();
            if (completed) {
                handle = play.applyAsInt(GlobalType.GUI_MESSAGE_BAR_END_2.getId());
            }
        }

        void cancel() {
            if (handle != 0) {
                stop.accept(handle);
                handle = 0;
            }
            active = false;
            sliding = false;
        }
    }
}
