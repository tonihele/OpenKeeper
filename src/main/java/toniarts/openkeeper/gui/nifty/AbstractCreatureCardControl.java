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
import com.simsilica.es.Entity;
import de.lessvoid.nifty.controls.AbstractController;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.creature.CreatureState;

/**
 * Abstract base class for creature card UI components
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractCreatureCardControl extends AbstractController {

    public enum CreatureUIState {
        IDLE, BUSY, FIGHT, WORK, DEFENCE, HAPPY, UNHAPPY, ANGRY;

        /**
         * Checks if the creature belongs to this category. IDLE, FIGHT and BUSY
         * (everything else) are exclusive, WORK is the busy creatures that
         * have a task. The moods follow the native anger state. DK II's own
         * category boundaries are not known, and nothing tells defending from
         * attacking, so DEFENCE matches no one.
         *
         * @param entity the creature, with {@link CreatureAi},
         * {@link AttackTarget}, {@link TaskComponent} and {@link CreatureMood}
         * if it has them
         * @param unhappyThreshold the creature's runtime unhappy threshold
         * @return {@code true} if the creature is in this category
         */
        public boolean matches(Entity entity, int unhappyThreshold) {
            CreatureAi creatureAi = entity.get(CreatureAi.class);
            boolean idle = creatureAi != null && creatureAi.getCreatureState() == CreatureState.IDLE;
            boolean fight = !idle && entity.get(AttackTarget.class) != null;
            CreatureMood mood = entity.get(CreatureMood.class);
            int moodState = mood != null ? mood.getState(unhappyThreshold) : 0;
            return switch (this) {
                case IDLE -> idle;
                case FIGHT -> fight;
                case BUSY -> !idle && !fight;
                case WORK -> !idle && !fight && entity.get(TaskComponent.class) != null;
                case DEFENCE -> false;
                case HAPPY -> moodState == 0;
                case UNHAPPY -> moodState == 1;
                case ANGRY -> moodState == 2;
            };
        }
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
