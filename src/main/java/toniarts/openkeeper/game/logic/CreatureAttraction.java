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
package toniarts.openkeeper.game.logic;

import java.util.ArrayList;
import java.util.Set;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.Attraction;
import toniarts.openkeeper.tools.convert.map.Creature.AttractionType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * The attraction requirements a creature sets for coming in through a Portal,
 * checked as the original checks them. The original packs both values of a
 * requirement into one byte each, so only their low bytes count.
 */
public final class CreatureAttraction {

    private CreatureAttraction() {
        // Nope
    }

    /**
     * See if the keeper meets all the creature's attraction requirements:
     * <ul>
     * <li>room: the keeper's rooms of the type have at least the given number
     * of tiles together</li>
     * <li>gold: the keeper has at least the given amount of gold</li>
     * <li>mana: the keeper has at least the given amount of mana</li>
     * <li>creature: never stops the creature, the original compares the
     * creature count with zero</li>
     * </ul>
     *
     * @param creature the creature
     * @param player the keeper
     * @param kwdFile the level data
     * @return true if the creature can be attracted
     */
    public static boolean isSatisfied(Creature creature, IPlayerController player, IKwdFile kwdFile) {
        for (Attraction attraction : creature.getAttractions()) {
            AttractionType type = attraction.getType();
            if (type == null) {
                continue;
            }
            switch (type) {
                case ROOM -> {
                    if (!isRoomRequirementMet(attraction, player, kwdFile)) {
                        return false;
                    }
                }
                case GOLD -> {
                    if (player.getKeeper().getGold() < getAmount(attraction)) {
                        return false;
                    }
                }
                case MANA -> {
                    if (player.getKeeper().getMana() < getAmount(attraction)) {
                        return false;
                    }
                }
                default -> {
                    // No requirement
                }
            }
        }
        return true;
    }

    private static boolean isRoomRequirementMet(Attraction attraction, IPlayerController player, IKwdFile kwdFile) {
        Room room = kwdFile.getRoomById(attraction.getRoomId() & 0xFF);
        int size = attraction.getRoomSize() & 0xFF;
        int tiles = getTileCount(room, player);

        // Without a size the original asks for room capacity left for the creature type when it picks the
        // creature, we just want the room
        if (size == 0) {
            return tiles > 0;
        }
        return tiles >= size;
    }

    private static int getTileCount(Room room, IPlayerController player) {
        Set<IRoomController> rooms = player.getRoomControl().getRoomControllers().get(room);
        if (rooms == null) {
            return 0;
        }
        int tiles = 0;
        for (IRoomController roomController : new ArrayList<>(rooms)) {
            tiles += roomController.getRoomInstance().getCoordinates().size();
        }
        return tiles;
    }

    private static int getAmount(Attraction attraction) {
        return ((attraction.getRoomSize() & 0xFF) << 8) | (attraction.getRoomId() & 0xFF);
    }

}
