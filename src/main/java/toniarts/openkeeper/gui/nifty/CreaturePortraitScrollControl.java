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
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import java.util.List;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.GlobalType;

/** Scrolls creature portraits by one whole card per arrow click, without wrapping. */
public final class CreaturePortraitScrollControl extends AbstractController {

    private Nifty nifty;
    private Element viewport;
    private Element visibleWindow;
    private Element content;
    private List<Element> portraits = List.of();
    private int firstVisible;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        viewport = screen.findElementById("tab-creature-viewport");
        visibleWindow = screen.findElementById("tab-creature-visible");
        content = screen.findElementById("tab-creature-content");
    }

    @Override
    public void init(Parameters parameter) {
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onFocus(boolean getFocus) {
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    public void forward() {
        playClickSound();
        if (firstVisible < maxFirstVisible()) {
            firstVisible++;
            layoutPortraits();
        }
    }

    public void back() {
        playClickSound();
        if (firstVisible > 0) {
            firstVisible--;
            layoutPortraits();
        }
    }

    public void setPortraits(List<Element> portraits) {
        this.portraits = List.copyOf(portraits);
        firstVisible = Math.min(firstVisible, maxFirstVisible());
        layoutPortraits();
    }

    private int portraitStep() {
        if (portraits.isEmpty()) {
            return 1;
        }
        Element portrait = portraits.getFirst();
        return Math.max(1, portrait.getWidth()
                + portrait.getMarginLeft().getValueAsInt(1f)
                + portrait.getMarginRight().getValueAsInt(1f));
    }

    private int maxFirstVisible() {
        if (viewport == null) {
            return 0;
        }
        int visibleSlots = Math.max(1, viewport.getWidth() / portraitStep());
        return Math.max(0, portraits.size() - visibleSlots);
    }

    private void layoutPortraits() {
        if (content == null || viewport == null || visibleWindow == null) {
            return;
        }
        int step = portraitStep();
        int visibleSlots = Math.max(1, viewport.getWidth() / step);
        visibleWindow.setConstraintWidth(SizeValue.px(visibleSlots * step));
        content.setConstraintX(SizeValue.px(-firstVisible * step));
        viewport.layoutElements();
    }

    private void playClickSound() {
        var sound = NiftyUtils.getSoundHandler(nifty, GlobalCategory.GUI_BUTTON_DEFAULT,
                GlobalType.GUI_BUTTON_CKICK.getId());
        if (sound != null) {
            sound.play();
        }
    }
}
