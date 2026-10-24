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
package toniarts.openkeeper.game.state;

import java.util.Set;
import java.util.function.Consumer;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Decides which HUD room icons the keeper can afford. The ones it can't are
 * shown darkened, like in DK II
 */
public final class HudAffordability {

    private HudAffordability() {
        // Nope
    }

    /**
     * A room can be afforded while the gold covers at least one tile of it
     *
     * @param room the room
     * @param gold the gold the keeper has
     * @return true if the keeper can build the room
     */
    public static boolean canAffordRoom(Room room, int gold) {
        return room.getCost() <= gold;
    }

    /**
     * Gives an icon the effects it should have: the effects are all stopped,
     * then the icon is darkened if it is unaffordable, or marked if it is the
     * selected one. A darkened icon stays darkened even if it is selected, the
     * same as when the selection changes.
     *
     * @param <T> the icon type
     * @param darkened the darkened icons of one kind
     * @param icon the icon
     * @param selected is the icon the selected one
     * @param stop stops the effects of an icon
     * @param darken darkens an icon
     * @param select marks an icon as selected
     */
    public static <T> void reapply(Set<T> darkened, T icon, boolean selected, Consumer<T> stop,
            Consumer<T> darken, Consumer<T> select) {
        stop.accept(icon);
        if (darkened.contains(icon)) {
            darken.accept(icon);
        } else if (selected) {
            select.accept(icon);
        }
    }

}
