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
package toniarts.openkeeper.game.event;

import com.simsilica.es.EntityId;

/**
 * A creature of a keeper turned unhappy or angry, like DK II's messages 0x11
 * and 0x12 (0x478B80, 0x478C30)
 *
 * @param playerId the keeper
 * @param creatureId the creature
 * @param angry true when it turned angry, false when unhappy
 */
public record CreatureUpset(short playerId, EntityId creatureId, boolean angry) {
}
