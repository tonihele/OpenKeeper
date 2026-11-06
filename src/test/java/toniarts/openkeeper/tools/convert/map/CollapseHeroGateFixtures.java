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
package toniarts.openkeeper.tools.convert.map;

/**
 * Builds the "collapse hero gate" script action for tests. Lives in the
 * trigger package because the setters are {@code protected}.
 */
public final class CollapseHeroGateFixtures {

    private CollapseHeroGateFixtures() {
    }

    /**
     * A "collapse hero gate" action, like the ones from level3 on
     *
     * @param id the trigger id
     * @param posX the tile x, 1-based like in the map files
     * @param posY the tile y, 1-based like in the map files
     * @return the action
     */
    public static TriggerAction collapseHeroGate(int id, int posX, int posY) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setRepeatTimes((short) 1);
        action.setType(TriggerAction.ActionType.COLLAPSE_HERO_GATE);
        action.setUserData("posX", posX);
        action.setUserData("posY", posY);
        return action;
    }

}
