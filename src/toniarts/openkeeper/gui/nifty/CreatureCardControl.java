/*
 * Copyright (C) 2014-2017 OpenKeeper
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
import de.lessvoid.nifty.NiftyMethodInvoker;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.input.NiftyMouseInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import toniarts.openkeeper.game.state.PlayerScreenController;
import toniarts.openkeeper.gui.nifty.WorkerAmountControl.State;

/**
 *
 * @author archdemon
 */
public class CreatureCardControl extends AbstractController {

    private String creatureId;
    private Nifty nifty;
    private Element element;
    private Screen screen;

    private Label total;
    private Label idle;
    private Label busy;
    private Label work;
    private Label fight;
    private Label defence;
    private Label happy;
    private Label unhappy;
    private Label angry;

    private State state = State.TOTAL;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.screen = screen;
        this.element = element;

        this.creatureId = parameter.get("creatureId");

        total = this.element.findNiftyControl("#total", Label.class);
        idle = this.element.findNiftyControl("#idle", Label.class);
        busy = this.element.findNiftyControl("#busy", Label.class);
        fight = this.element.findNiftyControl("#fight", Label.class);
        work = this.element.findNiftyControl("#work", Label.class);
        defence = this.element.findNiftyControl("#defence", Label.class);
        happy = this.element.findNiftyControl("#happy", Label.class);
        unhappy = this.element.findNiftyControl("#unhappy", Label.class);
        angry = this.element.findNiftyControl("#angry", Label.class);
    }

    @Override
    public void onStartScreen() {
        for (Element tab : element.findElementById("#tab-creature-card").getChildren()) {
            String id = tab.getId();
            if (id != null && id.endsWith("#tab-" + state.toString().toLowerCase())) {
                tab.show();
            } else {
                tab.hide();
            }
        }
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    public void setState(State state) {
        this.state = state;
        onStartScreen();
    }

    public void zoomTo(String uiState) {
        // FIXME very bad
        ScreenController sc = screen.getScreenController();
        if (sc instanceof PlayerScreenController) {
            ((PlayerScreenController) sc).zoomToCreature(creatureId, uiState);
        }
    }

    public void pickUp(String uiState) {
        // FIXME very bad
        ScreenController sc = screen.getScreenController();
        if (sc instanceof PlayerScreenController) {
            ((PlayerScreenController) sc).pickUpCreature(creatureId, uiState);
        }
    }

    public void setTotal(int value) {
        total.setText(Integer.toString(value));
    }

    public void setIdle(int value) {
        idle.setText(Integer.toString(value));
    }

    public void setBusy(int value) {
        busy.setText(Integer.toString(value));
    }

    public void setWork(int value) {
        work.setText(Integer.toString(value));
    }

    public void setFight(int value) {
        fight.setText(Integer.toString(value));
    }

    public void setDefence(int value) {
        defence.setText(Integer.toString(value));
    }

    public void setHappy(int value) {
        happy.setText(Integer.toString(value));
    }

    public void setUnhappy(int value) {
        unhappy.setText(Integer.toString(value));
    }

    public void setAngry(int value) {
        angry.setText(Integer.toString(value));
    }

}
