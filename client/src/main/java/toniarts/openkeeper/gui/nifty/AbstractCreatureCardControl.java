/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.jme3.util.SafeArrayList;
import de.lessvoid.nifty.controls.AbstractController;

/**
 * Abstract base class for creature card UI components
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractCreatureCardControl extends AbstractController {

    public enum CreatureUIState {
        IDLE, BUSY, FIGHT, WORK, DEFENCE, HAPPY, UNHAPPY, ANGRY
    };

    private SafeArrayList<CreatureCardEventListener> listeners;

    public void zoomTo(String uiState) {
        if (listeners != null && !listeners.isEmpty()) {
            CreatureUIState selectedState = parseUiStateString(uiState);
            for (CreatureCardEventListener listener : listeners.getArray()) {
                listener.zoomTo(getCreatureId(), selectedState);
            }
        }
    }

    public void pickUp(String uiState) {
        if (listeners != null && !listeners.isEmpty()) {
            CreatureUIState selectedState = parseUiStateString(uiState);
            for (CreatureCardEventListener listener : listeners.getArray()) {
                listener.pickUp(getCreatureId(), selectedState);
            }
        }
    }

    private static CreatureUIState parseUiStateString(String uiState) {
        return "null".equals(uiState) ? null : CreatureUIState.valueOf(uiState.toUpperCase());
    }

    public void addListener(CreatureCardEventListener listener) {
        if (listeners == null) {
            listeners = new SafeArrayList<>(CreatureCardEventListener.class);
        }
        listeners.add(listener);
    }

    public void removeListener(CreatureCardEventListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Get creature ID that this card represents
     *
     * @return creature ID
     */
    public abstract short getCreatureId();

}
