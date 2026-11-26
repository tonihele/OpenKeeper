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
package toniarts.openkeeper.gui.nifty;

import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.effects.impl.Hint;
import de.lessvoid.nifty.elements.Element;
import java.util.Properties;

/** Cursor-side map hint using the same appearance as the HUD hints. */
public final class MapHoverHint {

    // DK II's map hint path waits 1500 ms; GUI buttons use a separate delay.
    private static final float DELAY = 1.5f;

    private final Element anchor;
    private final String text;
    private float elapsed;
    private Hint hint;

    public MapHoverHint(Element anchor, String text) {
        this.anchor = anchor;
        this.text = text;
    }

    public void update(boolean eligible, float tpf) {
        if (!eligible) {
            clear();
            return;
        }

        elapsed = Math.min(DELAY, elapsed + tpf);
        if (elapsed < DELAY) {
            return;
        }

        if (hint == null) {
            Properties properties = new Properties();
            properties.setProperty("hintText", text);
            // The map target determines the delay, rather than mouse inactivity.
            properties.setProperty("hintDelay", "0");
            hint = new Hint();
            hint.activate(anchor.getNifty(), anchor, new EffectProperties(properties));
        }
        hint.execute(anchor, 1f, null, anchor.getNifty().getRenderEngine());
    }

    public void clear() {
        elapsed = 0;
        if (hint != null) {
            hint.deactivate();
            hint = null;
        }
    }
}
