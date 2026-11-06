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
package toniarts.openkeeper.gui.nifty.flowlayout;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.builder.ControlDefinitionBuilder;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.nulldevice.NullSoundDevice;
import de.lessvoid.nifty.spi.input.InputSystem;
import de.lessvoid.nifty.spi.render.RenderDevice;
import de.lessvoid.nifty.spi.time.impl.AccurateTimeProvider;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The HUD build panels split their icons into two rows once one row is full
 */
public class CustomScrollTest {

    @Test
    public void splittingIntoRowsKeepsNiftyRenderOrderIntact() {
        Nifty nifty = new Nifty(stub(RenderDevice.class), new NullSoundDevice(), stub(InputSystem.class),
                new AccurateTimeProvider());
        new ControlDefinitionBuilder("customScroll") {
            {
                controller(CustomScroll.class.getName());
                panel(new PanelBuilder() {
                    {
                        childLayoutHorizontal();
                        panel(new PanelBuilder("#back"));
                        panel(new PanelBuilder("#content") {
                            {
                                childLayoutAbsolute();
                                width("*");
                                height("*");
                            }
                        });
                        panel(new PanelBuilder("#forward"));
                    }
                });
            }
        }.registerControlDefintion(nifty);
        new ControlDefinitionBuilder("icon") {
            {
                panel(new PanelBuilder() {
                    {
                        width("64px");
                        height("64px");
                    }
                });
            }
        }.registerControlDefintion(nifty);
        new ScreenBuilder("hud") {
            {
                layer(new LayerBuilder("layer") {
                    {
                        childLayoutAbsolute();
                        control(new ControlBuilder("scroll", "customScroll") {
                            {
                                // Room for several icons before the second row
                                width("1300px");
                                height("128px");
                            }
                        });
                    }
                });
            }
        }.build(nifty);
        nifty.gotoScreen("hud");
        nifty.update();

        List<LogRecord> problems = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                    problems.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger niftyLogger = Logger.getLogger("de.lessvoid.nifty");
        niftyLogger.addHandler(handler);
        try {
            CustomScroll scroll = nifty.getCurrentScreen().findControl("scroll", CustomScroll.class);
            List<Element> icons = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                icons.add(scroll.addElement(new ControlBuilder("icon-" + i, "icon")));
                nifty.update();
            }

            assertEquals(List.of(), problems.stream().map(LogRecord::getMessage).toList());
            Element firstRow = icons.get(0).getParent();
            Element secondRow = icons.get(1).getParent();
            assertEquals(List.of(icons.get(0), icons.get(2), icons.get(4), icons.get(6), icons.get(8), icons.get(10)),
                    firstRow.getChildren());
            assertEquals(List.of(icons.get(1), icons.get(3), icons.get(5), icons.get(7), icons.get(9), icons.get(11)),
                    secondRow.getChildren());
        } finally {
            niftyLogger.removeHandler(handler);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return method.getName().equals("getHeight") ? 600 : method.getName().equals("getWidth") ? 800 : 0;
            }
            return null;
        });
    }
}
