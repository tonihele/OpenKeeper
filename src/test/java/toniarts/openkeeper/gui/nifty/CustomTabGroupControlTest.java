/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.gui.nifty;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.input.keyboard.KeyboardInputEvent;
import de.lessvoid.nifty.nulldevice.NullSoundDevice;
import de.lessvoid.nifty.spi.input.InputSystem;
import de.lessvoid.nifty.spi.render.RenderDevice;
import de.lessvoid.nifty.spi.time.impl.AccurateTimeProvider;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomTabGroupControlTest {
    @Test
    void pinKeepsBothCategoriesVisibleWithSeparateSelectionAndOriginalNotches() {
        Nifty nifty = createHud(true);
        var group = nifty.getCurrentScreen().findNiftyControl("tabs", CustomTabGroupControl.class);
        group.togglePinnedTab(1);
        group.setSelectedTabIndex(2);
        nifty.update();
        assertEquals(1, group.getPinnedTabIndex());
        assertEquals(2, group.getSelectedTabIndex());
        assertFalse(group.getTabAtIndex(0).isVisible());
        assertTrue(group.getTabAtIndex(1).isVisible());
        assertTrue(group.getTabAtIndex(2).isVisible());
        var active = group.getTabAtIndex(2).getElement();
        var pinned = group.getTabAtIndex(1).getElement();
        assertEquals(active.getWidth(), pinned.getWidth());
        assertEquals(64, active.getY());
        assertEquals(active.getY(), pinned.getY());
        assertEquals(136, active.getHeight());
        assertEquals(active.getX() + active.getWidth(), pinned.getX());
        var button = nifty.getCurrentScreen().findElementById("tabs#tab-button-panel#tabButton-1");
        assertTrue(button.findElementById("#pin").isVisible());
        assertFalse(button.findElementById("#selector").isVisible());
        group.setSelectedTabIndex(1);
        nifty.update();
        assertEquals(800, pinned.getWidth());
        assertTrue(button.findElementById("#pin").isVisible());
        assertTrue(button.findElementById("#selector").isVisible());
    }

    @Test
    void replacingAndReleasingPinRestoresFullWidthWithoutDuplicatingContent() {
        Nifty nifty = createHud(true);
        var group = nifty.getCurrentScreen().findNiftyControl("tabs", CustomTabGroupControl.class);
        group.togglePinnedTab(0);
        assertEquals(0, group.getSelectedTabIndex());
        group.togglePinnedTab(2);
        group.setSelectedTabIndex(1);
        nifty.update();
        assertEquals(2, group.getPinnedTabIndex());
        assertFalse(group.getTabAtIndex(0).isVisible());
        group.togglePinnedTab(2);
        nifty.update();
        assertEquals(-1, group.getPinnedTabIndex());
        assertEquals(800, group.getTabAtIndex(2).getElement().getWidth());
        assertFalse(group.getTabAtIndex(1).isVisible());
        assertEquals(3, group.getTabCount());
    }

    @Test
    void groupsWithoutOptInKeepSingleTabBehavior() {
        Nifty nifty = createHud(false);
        var group = nifty.getCurrentScreen().findNiftyControl("tabs", CustomTabGroupControl.class);
        group.togglePinnedTab(1);
        assertEquals(-1, group.getPinnedTabIndex());
        assertTrue(group.getTabAtIndex(0).isVisible());
        assertFalse(group.getTabAtIndex(1).isVisible());
    }

    @Test
    void shiftClickPinsAndOrdinaryClickKeepsThePinAcrossScreenReentry() {
        Nifty nifty = createHud(true);
        var screen = nifty.getCurrentScreen();
        var group = screen.findNiftyControl("tabs", CustomTabGroupControl.class);
        String buttonId = "tabs#tab-button-panel#tabButton-1";
        Button button = screen.findNiftyControl(buttonId, Button.class);
        screen.keyEvent(new KeyboardInputEvent(KeyboardInputEvent.KEY_LSHIFT, '\0', true, true, false));
        nifty.publishEvent(buttonId, new ButtonClickedEvent(button));
        assertEquals(1, group.getPinnedTabIndex());
        assertEquals(1, group.getSelectedTabIndex());
        group.onEndScreen();
        group.onStartScreen();
        buttonId = "tabs#tab-button-panel#tabButton-2";
        button = screen.findNiftyControl(buttonId, Button.class);
        nifty.publishEvent(buttonId, new ButtonClickedEvent(button));
        assertEquals(1, group.getPinnedTabIndex());
        assertEquals(2, group.getSelectedTabIndex());
        nifty.update();
        assertTrue(group.getTabAtIndex(1).isVisible());
        assertTrue(group.getTabAtIndex(2).isVisible());
    }

    private Nifty createHud(boolean pinnable) {
        Nifty nifty = new Nifty(stub(RenderDevice.class), new NullSoundDevice(), stub(InputSystem.class),
                new AccurateTimeProvider());
        nifty.loadStyleFile("Interface/CustomTabStyle.xml");
        nifty.loadControlFile("Interface/CustomTabControl.xml");
        new ScreenBuilder("hud") {{
            layer(new LayerBuilder("layer") {{
                childLayoutVertical();
                control(new ControlBuilder("tabs", "customTabGroup") {{
                    width("100%"); height("200px"); parameter("pinnable", Boolean.toString(pinnable));
                    for (int i = 0; i < 3; i++) {
                        final int index = i;
                        control(new ControlBuilder("tab-" + i, "customTab") {{
                            parameter("image", "dummy.png"); parameter("active", "selection.png");
                            parameter("hintText", "hint"); parameter("tooltip", "tooltip"); parameter("sound", "sound");
                            panel(new PanelBuilder("content-" + index) {{width("100%"); height("100%");}});
                        }});
                    }
                }});
            }});
        }}.build(nifty);
        nifty.gotoScreen("hud");
        nifty.update();
        return nifty;
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) {
                if (method.getName().equals("getWidth")) return type == RenderDevice.class ? 800 : 64;
                if (method.getName().equals("getHeight")) return type == RenderDevice.class ? 600 : 64;
                return 0;
            }
            if (method.getReturnType().isInterface()) return stub(method.getReturnType());
            return null;
        });
    }
}
