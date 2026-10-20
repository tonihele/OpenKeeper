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
package toniarts.openkeeper.game.controller;

/**
 * Who may slap which creature. Shared by the client, which decides whether a
 * click is a slap at all, and the server, which applies it, so that the two
 * agree.
 */
public final class SlapRules {

    private SlapRules() {
        // Nope
    }

    /**
     * A prisoner can only be slapped by the keeper holding it, whoever it
     * belongs to: heroes and creatures of other keepers in the keeper's own
     * prison. A creature taken prisoner by someone else can be slapped by its
     * jailer, but no longer by its owner. A free creature only by its owner,
     * an enemy walking on the keeper's land cannot be slapped.
     *
     * @param playerId the slapping keeper
     * @param creatureOwnerId the owner of the creature
     * @param prisonerOf the keeper holding the creature prisoner,
     * {@code null} if it is free
     * @return whether the keeper may slap the creature
     */
    public static boolean canSlapCreature(short playerId, short creatureOwnerId, Short prisonerOf) {
        if (prisonerOf != null) {
            return prisonerOf == playerId;
        }
        return creatureOwnerId == playerId;
    }

}
