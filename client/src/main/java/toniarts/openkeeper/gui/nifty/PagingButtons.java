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

import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.tools.Color;

/** Applies DK II's shared enabled/disabled state to a pair of paging arrows. */
public final class PagingButtons {

    private static final float DISABLED_BRIGHTNESS = 100f / 255f;

    private final Element forward;
    private final Element back;
    private final Color forwardColor;
    private final Color backColor;
    private boolean enabled = true;

    public PagingButtons(Element forward, Element back) {
        this.forward = forward;
        this.back = back;
        forwardColor = forward.getRenderer(ImageRenderer.class).getImage().getColor();
        backColor = back.getRenderer(ImageRenderer.class).getImage().getColor();
        setEnabled(false);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        setArrowState(forward, forwardColor, enabled);
        setArrowState(back, backColor, enabled);
        this.enabled = enabled;
    }

    private static void setArrowState(Element arrow, Color normalColor, boolean enabled) {
        if (enabled) {
            arrow.enable();
        } else {
            arrow.disable();
        }
        Color color = normalColor == null ? Color.WHITE : normalColor;
        // Tint each image itself: the jME/Nifty batch image path does not use
        // the renderer's general RGB color. Preserve alpha for opaque dimming.
        arrow.getRenderer(ImageRenderer.class).getImage().setColor(enabled ? normalColor
                : new Color(color.getRed() * DISABLED_BRIGHTNESS,
                        color.getGreen() * DISABLED_BRIGHTNESS,
                        color.getBlue() * DISABLED_BRIGHTNESS, color.getAlpha()));
    }
}
