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
package toniarts.openkeeper.game.controller.shot;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.tools.convert.map.Shot;

/**
 * One fired shot, as handed to an {@link IShotProcessor}
 *
 * @param shot the shot type
 * @param shotData1 first parameter, meaning depends on the process type
 * @param shotData2 second parameter, meaning depends on the process type
 * @param playerId the player firing the shot
 * @param position where the shot was fired at
 * @param target the targeted entity, may be {@code null}
 */
public record ShotRequest(Shot shot, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target) {

}
