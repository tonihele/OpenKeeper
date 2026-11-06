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
package toniarts.openkeeper.view.effect;

import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;

/** The effects requested by DK II's successful per-tile room actions. */
public final class RoomActionEffects {
    public static final int BUILD_EFFECT = 81;
    public static final int GOLD_VALUE_EFFECT = 22;

    private RoomActionEffects() {
    }

    public record Request(int effectId, float height, Integer goldValue) {
    }

    public static Request resolve(GameEvent event, IKwdFile level) {
        if (event.subjectKind != GameEvent.ThingFlag.ROOM || event.position == null) {
            return null;
        }
        return switch (event.type) {
            case ROOM_TILE_BUILT -> new Request(BUILD_EFFECT, 1.5f, null);
            case ROOM_SOLD -> {
                var room = level.getRoomById((short) event.subjectId);
                if (room == null) {
                    yield null;
                }
                float percentage = level.getVariables().get(
                        Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST).getValue();
                yield new Request(GOLD_VALUE_EFFECT, 1f, (int) (room.getCost() * (percentage / 100)));
            }
            default -> null;
        };
    }
}
