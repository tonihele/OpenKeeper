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
 * Builds the creature attraction fixtures for tests. Lives in the data
 * classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class CreatureAttractionFixtures {

    private CreatureAttractionFixtures() {
    }

    /**
     * A room attraction requirement
     */
    public static Creature.Attraction roomAttraction(short roomId, int size) {
        Creature.Attraction attraction = attraction(Creature.AttractionType.ROOM);
        attraction.setRoomId(roomId);
        attraction.setRoomSize(size);
        return attraction;
    }

    /**
     * A gold or mana attraction requirement
     */
    public static Creature.Attraction amountAttraction(Creature.AttractionType type, int amount) {
        Creature.Attraction attraction = attraction(type);
        attraction.setAmount(amount);
        return attraction;
    }

    public static Creature.Attraction attraction(Creature.AttractionType type) {
        Creature.Attraction attraction = new Creature.Attraction();
        attraction.setType(type);
        return attraction;
    }

    public static Creature creature(short creatureId, Creature.Attraction... attractions) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setAttractions(attractions);
        return creature;
    }

    public static Room room(short roomId) {
        Room room = new Room();
        room.setRoomId(roomId);
        return room;
    }

}
