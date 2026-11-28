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
package toniarts.openkeeper.gui.nifty;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.controls.slider.SliderControl;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.SoundManager;

/** Settings slider feedback at mouse press/release, independent of value changes. */
public final class SoundSliderControl extends SliderControl {

    private String soundCategory;
    private Nifty nifty;
    private Screen screen;
    private boolean dragging;
    private boolean arrowHeld;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        super.bind(nifty, screen, element, parameter);
        this.nifty = nifty;
        this.screen = screen;
        soundCategory = parameter.get("soundCategory");
        if (soundCategory == null) {
            soundCategory = GlobalCategory.OBJECT_FE_PARCHMENT;
        }
        dragging = false;
        arrowHeld = false;
    }

    @Override
    public void onStartScreen() {
        super.onStartScreen();
        dragging = false;
        arrowHeld = false;
    }

    public void decrease() {
        if (!arrowHeld) {
            arrowHeld = true;
            SoundManager.playSound(soundCategory, 0x306);
        }
        super.upClick();
    }

    public void increase() {
        if (!arrowHeld) {
            arrowHeld = true;
            SoundManager.playSound(soundCategory, 0x307);
        }
        super.downClick();
    }

    public void releaseArrow() {
        arrowHeld = false;
    }

    public void startDrag(int mouseX, int mouseY) {
        dragging = true;
        SoundManager.playSound(soundCategory, 0x304);
        super.mouseClick(mouseX, mouseY);
    }

    public void finishDrag() {
        if (dragging) {
            dragging = false;
            Element element = getElement();
            if (nifty.getCurrentScreen() == screen && element != null
                    && element.isVisible() && element.isEnabled()) {
                SoundManager.playSound(soundCategory, 0x305);
            }
        }
    }
}
