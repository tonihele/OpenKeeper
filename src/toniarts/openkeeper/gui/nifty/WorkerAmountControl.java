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
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;

/**
 *
 * @author archdemon
 */
public class WorkerAmountControl extends AbstractCreatureCardControl {

    public enum State {
        TOTAL(0),
        JOBS(1),
        FIGHTS(2),
        MOODS(3);

        private final int value;

        private State(int value) {
            this.value = value;
        }

        public int getValue () {
            return value;
        }
    }

    private short creatureId;
    private Nifty nifty;
    private Element element;
    private Screen screen;

    private Label total;
    private Label idle;
    private Label busy;
    private Label fight;
    private State state = State.TOTAL;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.screen = screen;
        this.element = element;

        this.creatureId = Short.parseShort(parameter.get("creatureId"));

        total = this.element.findNiftyControl("#workerTotal", Label.class);
        idle = this.element.findNiftyControl("#workerIdle", Label.class);
        busy = this.element.findNiftyControl("#workerBusy", Label.class);
        fight = this.element.findNiftyControl("#workerFight", Label.class);
    }

    @Override
    public void onStartScreen() {
        initState();
    }

    private void initState() {
        Element e = element.findElementById("#t-creature_panel");
        String fileName = String.format("Textures/GUI/Tabs/t-creature_panel-%s.png", state.getValue());

        ImageRenderer imageRenderer = e.getRenderer(ImageRenderer.class);
        imageRenderer.setImage(nifty.getRenderEngine().createImage(screen, fileName, true));
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    public void setState(State state) {
        this.state = state;
        initState();
    }

    public void setTotal(int value) {
        this.total.setText(String.valueOf(value));
    }

    public void setIdle(int value) {
        this.idle.setText(String.valueOf(value));
    }

    public void setBusy(int value) {
        this.busy.setText(String.valueOf(value));
    }

    public void setFight(int value) {
        this.fight.setText(String.valueOf(value));
    }

    public void setValues(int total, int idle, int busy, int fight) {
        setTotal(total);
        setIdle(idle);
        setBusy(busy);
        setFight(fight);
    }

    @Override
    public short getCreatureId() {
        return creatureId;
    }

}
