/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.player;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import de.lessvoid.nifty.controls.Label;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ArchDemon
 */
public class PlayerGoldControl extends AbstractControl {

    private float tick = 0;
    private int gold = 0;
    private int goldMax = 0;
    private int goldMined = 0;
    private final List<Label> listeners = new ArrayList<>();

    public PlayerGoldControl() {
    }

    @Override
    protected void controlUpdate(float tpf) {
        tick += tpf;
        if (tick >= 1) {
            // update max value
            tick -= 1;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public int getGold() {
        return gold;
    }

    public int getGoldMined() {
        return goldMined;
    }

    public void addGold(int value) {
        if (value < 0) {
            return;
        }
        gold += value;
        goldMined += value; // Nope, need to get this some other way
        updateListeners();
    }

    public boolean subGold(int value) {
        if (gold >= value && value > 0) {
            gold -= value;
            updateListeners();
            return true;
        }
        return false;
    }

    public void addListener(Label label) {
        if (label != null) {
            updateListener(label);
            listeners.add(label);
        }
    }

    public void removeListeners() {
        listeners.clear();
    }

    private void updateListeners() {
        for (Label label : listeners) {
            updateListener(label);
        }
    }

    public void setGoldMax(int goldMax) {
        this.goldMax = goldMax;
    }

    public int getGoldMax() {
        return goldMax;
    }

    private void updateListener(Label label) {
        label.setText(String.format("%s", gold));
    }
}
