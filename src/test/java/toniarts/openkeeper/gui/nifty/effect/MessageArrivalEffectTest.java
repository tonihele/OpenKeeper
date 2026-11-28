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
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.nulldevice.NullSoundDevice;
import de.lessvoid.nifty.spi.input.InputSystem;
import de.lessvoid.nifty.spi.render.RenderDevice;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageArrivalEffectTest {

    @Test
    void movementCompletionUsesAnimationClockAndEarlyDeactivationIsSilent() {
        AtomicLong clock = new AtomicLong(100);
        Nifty nifty = createHud(clock);
        var element = nifty.getCurrentScreen().findElementById("message");
        var properties = new EffectProperties(new Properties());
        properties.setProperty("length", "640");
        properties.setProperty("mode", "in");
        properties.setProperty("direction", "right");
        Audio audio = new Audio();
        var effect = new MessageArrivalEffect(audio.sequence());
        effect.activate(nifty, element, properties);
        clock.set(739);
        effect.deactivate();
        assertEquals(List.of(800), audio.events);

        effect.activate(nifty, element, properties);
        clock.set(1379);
        effect.deactivate();
        assertEquals(List.of(800, 800, 802), audio.events);
    }

    @Test
    void hidingTheMessageSuppressesCompletionEvenAfterTheDuration() {
        AtomicLong clock = new AtomicLong();
        Nifty nifty = createHud(clock);
        var element = nifty.getCurrentScreen().findElementById("message");
        var properties = new EffectProperties(new Properties());
        properties.setProperty("length", "640");
        properties.setProperty("mode", "in");
        Audio audio = new Audio();
        var effect = new MessageArrivalEffect(audio.sequence());
        effect.activate(nifty, element, properties);
        element.hide();
        clock.set(1000);
        effect.deactivate();
        assertEquals(List.of(800), audio.events);
        assertTrue(audio.playing.isEmpty());
    }

    @Test
    void slideWaitsForStartImpactAndDoesNotRestartOnEveryFrame() {
        Audio audio = new Audio();
        var sounds = audio.sequence();
        sounds.start();
        sounds.update();
        sounds.update();
        assertEquals(List.of(800), audio.events);

        audio.playing.clear();
        sounds.update();
        assertEquals(List.of(800, 801), audio.events);
        audio.playing.clear();
        sounds.update();
        sounds.update();
        assertEquals(List.of(800, 801), audio.events);
    }

    @Test
    void completionStopsSlideAndPlaysEndImpactExactlyOnce() {
        Audio audio = new Audio();
        var sounds = audio.sequence();
        sounds.start();
        audio.playing.clear();
        sounds.update();
        sounds.finish(true);
        sounds.finish(true);
        sounds.update();
        assertEquals(List.of(800, 801, 802), audio.events);
        assertEquals(List.of(2), audio.stopped);
    }

    @Test
    void removalSuppressesCompletionEvenIfAnimationEndsLater() {
        Audio audio = new Audio();
        var sounds = audio.sequence();
        sounds.start();
        sounds.cancel();
        sounds.update();
        sounds.finish(true);
        assertEquals(List.of(800), audio.events);
        assertEquals(List.of(1), audio.stopped);
    }

    @Test
    void interruptedMovementDoesNotPlayArrivalImpact() {
        Audio audio = new Audio();
        var sounds = audio.sequence();
        sounds.start();
        audio.playing.clear();
        sounds.update();
        sounds.finish(false);
        assertEquals(List.of(800, 801), audio.events);
        assertTrue(audio.playing.isEmpty());
    }

    @Test
    void messagesOwnTheirHandlesAndCancellationDoesNotStopAnotherMessage() {
        Audio audio = new Audio();
        var first = audio.sequence();
        var second = audio.sequence();
        first.start();
        second.start();
        first.cancel();
        assertEquals(Set.of(2), audio.playing);
        assertEquals(List.of(1), audio.stopped);
    }

    @Test
    void restartStopsPreviousCompletionTail() {
        Audio audio = new Audio();
        var sounds = audio.sequence();
        sounds.start();
        sounds.finish(true);
        sounds.start();
        assertEquals(List.of(800, 802, 800), audio.events);
        assertEquals(List.of(1, 2), audio.stopped);
    }

    @Test
    void unavailableSoundDoesNotRetrySlideOnEveryFrame() {
        List<Integer> events = new ArrayList<>();
        var sounds = new MessageArrivalEffect.ArrivalSounds(event -> {
            events.add(event);
            return 0;
        }, handle -> false, handle -> {
            throw new AssertionError("No accepted voice to stop");
        });
        sounds.start();
        sounds.update();
        sounds.update();
        sounds.finish(true);
        assertEquals(List.of(800, 801, 802), events);
    }

    private static Nifty createHud(AtomicLong clock) {
        Nifty nifty = new Nifty(stub(RenderDevice.class), new NullSoundDevice(), stub(InputSystem.class), clock::get);
        new ScreenBuilder("hud") {{
            layer(new LayerBuilder("layer") {{
                childLayoutAbsolute();
                panel(new PanelBuilder("message") {{ width("32px"); height("64px"); }});
            }});
        }}.build(nifty);
        nifty.gotoScreen("hud");
        nifty.update();
        return nifty;
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            if (method.getReturnType() == int.class) {
                return method.getName().equals("getHeight") ? 600 : method.getName().equals("getWidth") ? 800 : 0;
            }
            if (method.getReturnType().isInterface()) {
                return stub(method.getReturnType());
            }
            return null;
        });
    }

    private static final class Audio {
        final List<Integer> events = new ArrayList<>();
        final List<Integer> stopped = new ArrayList<>();
        final Set<Integer> playing = new HashSet<>();
        int nextHandle;

        MessageArrivalEffect.ArrivalSounds sequence() {
            return new MessageArrivalEffect.ArrivalSounds(event -> {
                events.add(event);
                playing.add(++nextHandle);
                return nextHandle;
            }, playing::contains, handle -> {
                stopped.add(handle);
                playing.remove(handle);
            });
        }
    }
}
