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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.component.CreatureTypeAttraction;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

/**
 * Chooses the creature a Portal attracts, as the original does. Each creature
 * type that can come gets a weight and the choice is random by the weights:
 * <ul>
 * <li>a creature whose first attraction is the Lair weighs 1, or 3 if it
 * sleeps in a Goblin bed</li>
 * <li>any other creature weighs the capacity of the keeper's rooms of its
 * first attraction, less one for each creature of its type the keeper already
 * has</li>
 * </ul>
 * A type the keeper can attract for the first time during the level is
 * favoured: 200 times on the next generation, then 9 times, 8 times and so on.
 * Nothing comes while the keeper lacks Lair space for the creatures it has.
 * <br>
 * In skirmish and multiplayer games a secret room layout calls in its unique
 * creature, once per level, before an ordinary creature is chosen.
 * <br>
 * The creature types a keeper has been able to attract, their favour and the
 * unique creatures called in are kept in CreatureTypeAttraction entities.
 */
public final class PortalCreatureChoice {

    private static final int LAIR_ROOM_ID = 2;
    private static final int PORTAL_ROOM_ID = 3;
    private static final int LIBRARY_ROOM_ID = 6;
    private static final int TRAINING_ROOM_ID = 7;
    private static final int WORKSHOP_ROOM_ID = 10;
    private static final int TORTURE_CHAMBER_ROOM_ID = 12;
    private static final int TEMPLE_ROOM_ID = 13;
    private static final int CASINO_ROOM_ID = 15;
    private static final int HERO_PORTAL_ROOM_ID = 25;
    private static final short GOBLIN_BED_OBJECT_ID = 22;
    private static final int FAVOURED_GENERATIONS = 10;
    private static final double FIRST_GENERATION_FAVOUR = 200;

    /**
     * The secret room layouts of the unique creatures, in the order the
     * original checks them
     */
    private static final List<UniqueLayout> UNIQUE_LAYOUTS = List.of(
            new UniqueLayout((short) 32, 2, 1, cornered(3, 2, 4)),
            new UniqueLayout((short) 33, 4, 1, cornered(3, 4, 2)),
            new UniqueLayout((short) 34, 2, 1, cornered(5, 2, 6)),
            new UniqueLayout((short) 35, 2, 1, cornered(5, 2, 10)),
            new UniqueLayout((short) 36, 7, 16, ringed(5, 7, 9)),
            new UniqueLayout((short) 37, 1, 1, cornered(5, 1, 11)),
            new UniqueLayout((short) 38, 12, 16, ringed(5, 12, 11)),
            new UniqueLayout((short) 39, 2, 1, cornered(5, 2, 7)),
            new UniqueLayout((short) 40, 1, 1, cornered(5, 1, 15)),
            new UniqueLayout((short) 41, 10, 24, ringed(7, 10, 4)),
            new UniqueLayout((short) 42, 6, 32, new int[][]{
                {6, 6, 6, 6, 6, 6, 6},
                {6, 6, 6, 14, 14, 14, 6},
                {6, 6, 6, 14, 14, 14, 6},
                {6, 14, 14, 14, 14, 14, 6},
                {6, 14, 14, 14, 6, 6, 6},
                {6, 14, 14, 14, 6, 6, 6},
                {6, 6, 6, 6, 6, 6, 6}}),
            new UniqueLayout((short) 43, 7, 24, ringed(7, 7, 16)),
            new UniqueLayout((short) 44, 1, 7, new int[][]{
                {1, 2, 2, 2, 2, 2, 7},
                {1, 13, 13, 13, 13, 13, 7},
                {1, 13, 13, 13, 13, 13, 7},
                {1, 13, 13, 13, 13, 13, 7},
                {1, 13, 13, 13, 13, 13, 7},
                {1, 13, 13, 13, 13, 13, 7},
                {1, 12, 12, 12, 12, 12, 7}}));

    private final EntityData entityData;
    private final RandomGenerator random;

    public PortalCreatureChoice(EntityData entityData, RandomGenerator random) {
        this.entityData = entityData;
        this.random = random;
    }

    /**
     * Note the creature types the keeper can attract at the start of the
     * level, they are never favoured
     *
     * @param player the keeper
     * @param creaturesController the creatures controller
     * @param available whether the creature type is in the pool and meets
     * its attraction requirements
     */
    public void start(IPlayerController player, ICreaturesController creaturesController, Predicate<Creature> available) {
        if (getFreeLairSpace(player, creaturesController) >= 0) {
            updateAttractableTypes(player, available, false);
        }
    }

    /**
     * See if the level is played the way unique creatures come to their
     * secret room layouts. The original doesn't call them in on single player
     * levels.
     *
     * @param levelFlags the level's flags
     * @return true on skirmish and multiplayer levels
     */
    public static boolean isUniqueLayoutLevel(EnumSet<GameLevel.LevFlag> levelFlags) {
        return levelFlags.contains(GameLevel.LevFlag.IS_SKIRMISH_LEVEL) || levelFlags.contains(GameLevel.LevFlag.IS_MULTIPLAYER_LEVEL);
    }

    /**
     * Choose the unique creature the keeper's rooms call in. A unique creature
     * comes when one of the keeper's rooms of its layout's type has exactly
     * the layout's size and the rooms around it, anyone's, form the layout
     * from the room's top left corner. Each comes once per level, and only
     * while the keeper isn't short of Lair space. The pool, the attraction
     * requirements and the favour play no part.
     *
     * @param player the keeper
     * @param players all the players, their rooms make up the layouts
     * @param creaturesController the creatures controller
     * @param kwdFile the level data
     * @return the unique creature, or {@code null} if none is called in
     */
    public Creature chooseUniqueCreature(IPlayerController player, Collection<IPlayerController> players,
            ICreaturesController creaturesController, IKwdFile kwdFile) {
        if (getFreeLairSpace(player, creaturesController) < 0) {
            return null;
        }

        short playerId = player.getKeeper().getId();
        Map<Point, Integer> roomTypes = null;
        for (UniqueLayout layout : UNIQUE_LAYOUTS) {
            if (getAttraction(playerId, layout.creatureId()) != null) {
                continue;
            }
            if (roomTypes == null) {
                roomTypes = getRoomTypes(players);
            }
            if (isLayoutBuilt(layout, player, roomTypes)) {
                addAttraction(playerId, layout.creatureId(), 0);
                return kwdFile.getCreature(layout.creatureId());
            }
        }
        return null;
    }

    private static Map<Point, Integer> getRoomTypes(Collection<IPlayerController> players) {
        Map<Point, Integer> roomTypes = new HashMap<>();
        for (IPlayerController player : players) {
            for (Set<IRoomController> rooms : player.getRoomControl().getRoomControllers().values()) {
                for (IRoomController room : new ArrayList<>(rooms)) {
                    for (Point p : room.getRoomInstance().getCoordinates()) {
                        roomTypes.put(p, (int) room.getRoom().getId());
                    }
                }
            }
        }
        return roomTypes;
    }

    private static boolean isLayoutBuilt(UniqueLayout layout, IPlayerController player, Map<Point, Integer> roomTypes) {
        for (Entry<Room, Set<IRoomController>> rooms : player.getRoomControl().getRoomControllers().entrySet()) {
            if (rooms.getKey().getId() != layout.roomId()) {
                continue;
            }
            for (IRoomController room : new ArrayList<>(rooms.getValue())) {
                List<Point> coordinates = room.getRoomInstance().getCoordinates();
                if (coordinates.size() == layout.roomSize() && isLayoutAt(layout, coordinates, roomTypes)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isLayoutAt(UniqueLayout layout, List<Point> coordinates, Map<Point, Integer> roomTypes) {
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        for (Point p : coordinates) {
            left = Math.min(left, p.x);
            top = Math.min(top, p.y);
        }
        int[][] pattern = layout.pattern();
        for (int row = 0; row < pattern.length; row++) {
            for (int column = 0; column < pattern[row].length; column++) {
                Integer roomType = roomTypes.get(new Point(left + column, top + row));
                if (roomType == null || roomType != pattern[row][column]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * A square of one room type with another at its corners
     */
    private static int[][] cornered(int size, int corner, int rest) {
        int[][] pattern = new int[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                boolean isCorner = (row == 0 || row == size - 1) && (column == 0 || column == size - 1);
                pattern[row][column] = isCorner ? corner : rest;
            }
        }
        return pattern;
    }

    /**
     * A square of one room type inside a ring of another
     */
    private static int[][] ringed(int size, int ring, int inner) {
        int[][] pattern = new int[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                boolean isRing = row == 0 || row == size - 1 || column == 0 || column == size - 1;
                pattern[row][column] = isRing ? ring : inner;
            }
        }
        return pattern;
    }

    /**
     * Choose the creature the Portal attracts
     *
     * @param player the keeper
     * @param entrance the Portal
     * @param creaturesController the creatures controller
     * @param available whether the creature type is in the pool and meets
     * its attraction requirements
     * @return the creature type, or {@code null} if none can come
     */
    public Creature chooseCreature(IPlayerController player, IRoomController entrance, ICreaturesController creaturesController,
            Predicate<Creature> available) {
        if (getFreeLairSpace(player, creaturesController) < 0) {
            return null;
        }

        Map<Creature, Double> weights = getCandidateWeights(player, entrance, creaturesController, available);
        if (weights.isEmpty()) {
            return null;
        }

        // Every generation lessens the favour
        for (EntityId entityId : findAttractions(player.getKeeper().getId())) {
            CreatureTypeAttraction attraction = entityData.getComponent(entityId, CreatureTypeAttraction.class);
            if (attraction.favour > 0) {
                entityData.setComponent(entityId, new CreatureTypeAttraction(attraction.playerId, attraction.creatureId, attraction.favour - 1));
            }
        }

        double total = 0;
        for (double weight : weights.values()) {
            total += weight;
        }
        return pick(weights, random.nextDouble() * total);
    }

    /**
     * Get the creature types the Portal can attract, with their weights
     *
     * @param player the keeper
     * @param entrance the Portal
     * @param creaturesController the creatures controller
     * @param available whether the creature type is in the pool and meets
     * its attraction requirements
     * @return the creature types and their weights
     */
    Map<Creature, Double> getCandidateWeights(IPlayerController player, IRoomController entrance,
            ICreaturesController creaturesController, Predicate<Creature> available) {
        updateAttractableTypes(player, available, true);

        Map<Creature, Double> weights = new LinkedHashMap<>();
        for (Creature creature : player.getCreatureControl().getTypesAvailable()) {
            if (!isAvailableVia(creature, entrance.getRoom()) || !isAttractable(creature, player, available)) {
                continue;
            }
            EntityId attraction = getAttraction(player.getKeeper().getId(), creature.getCreatureId());
            int generations = attraction != null ? entityData.getComponent(attraction, CreatureTypeAttraction.class).favour : 0;
            double multiplier = generations == FAVOURED_GENERATIONS ? FIRST_GENERATION_FAVOUR : Math.max(1, generations);
            weights.put(creature, getWeight(creature, player) * multiplier);
        }
        return weights;
    }

    /**
     * Mark the creature types the keeper can attract now, the ones that
     * become attractable after the start of the level are favoured
     */
    private void updateAttractableTypes(IPlayerController player, Predicate<Creature> available, boolean favourNew) {
        short playerId = player.getKeeper().getId();
        for (Creature creature : player.getCreatureControl().getTypesAvailable()) {
            if (getAttraction(playerId, creature.getCreatureId()) != null || !isAttractable(creature, player, available)) {
                continue;
            }
            boolean favoured = favourNew && getFirstAttractionRoomId(creature) != LAIR_ROOM_ID;
            addAttraction(playerId, creature.getCreatureId(), favoured ? FAVOURED_GENERATIONS : 0);
        }
    }

    private EntityId getAttraction(short playerId, short creatureId) {
        for (EntityId entityId : findAttractions(playerId)) {
            if (entityData.getComponent(entityId, CreatureTypeAttraction.class).creatureId == creatureId) {
                return entityId;
            }
        }
        return null;
    }

    private Set<EntityId> findAttractions(short playerId) {
        return entityData.findEntities(new FieldFilter<>(CreatureTypeAttraction.class, "playerId", playerId),
                CreatureTypeAttraction.class);
    }

    private void addAttraction(short playerId, short creatureId, int favour) {
        entityData.setComponent(entityData.createEntity(), new CreatureTypeAttraction(playerId, creatureId, favour));
    }

    private static boolean isAttractable(Creature creature, IPlayerController player, Predicate<Creature> available) {
        return !creature.getFlags().contains(Creature.CreatureFlag.IS_UNIQUE)
                && available.test(creature)
                && getWeight(creature, player) > 0;
    }

    private static boolean isAvailableVia(Creature creature, Room entrance) {
        return switch (entrance.getId()) {
            case PORTAL_ROOM_ID ->
                creature.getFlags().contains(Creature.CreatureFlag.AVAILABLE_VIA_PORTAL);
            case HERO_PORTAL_ROOM_ID ->
                creature.getFlags2() != null && creature.getFlags2().contains(Creature.CreatureFlag2.AVAILABLE_VIA_HERO_PORTAL);
            default ->
                false;
        };
    }

    /**
     * Get the weight of a creature type for the Portal's choice, before any
     * favour
     *
     * @param creature the creature type
     * @param player the keeper
     * @return the weight, 0 if the creature has no room to come to
     */
    static int getWeight(Creature creature, IPlayerController player) {
        int roomId = getFirstAttractionRoomId(creature);
        if (roomId == LAIR_ROOM_ID) {
            return creature.getLairObjectId() == GOBLIN_BED_OBJECT_ID ? 3 : 1;
        }

        int capacity = 0;
        for (Entry<Room, Set<IRoomController>> rooms : player.getRoomControl().getRoomControllers().entrySet()) {
            if (rooms.getKey().getId() != roomId) {
                continue;
            }
            for (IRoomController room : new ArrayList<>(rooms.getValue())) {
                capacity += getAttractionCapacity(room);
            }
        }
        return Math.max(0, capacity - player.getCreatureControl().getTypeCount(creature));
    }

    /**
     * Get how many creatures a room supports when the Portal chooses its
     * creature. The furnished rooms count their furniture, one more if they
     * have any, except the Torture Chamber. The Temple supports two if it has
     * its hand and a Portal none. The others count their tiles.
     *
     * @param room the room
     * @return the capacity
     */
    static int getAttractionCapacity(IRoomController room) {
        int furniture = room.getFloorFurnitureCount() + room.getWallFurnitureCount();
        return switch (room.getRoom().getId()) {
            case PORTAL_ROOM_ID ->
                0;
            case LIBRARY_ROOM_ID, TRAINING_ROOM_ID, WORKSHOP_ROOM_ID, CASINO_ROOM_ID ->
                furniture > 0 ? furniture + 1 : 0;
            case TORTURE_CHAMBER_ROOM_ID ->
                furniture;
            case TEMPLE_ROOM_ID ->
                room.getFloorFurnitureCount() > 0 ? 2 : 0;
            default ->
                room.getRoomInstance().getCoordinates().size();
        };
    }

    /**
     * Get the free space in the keeper's Lairs, less the space the keeper's
     * creatures without a bed still need. Creatures can come while this is not
     * negative.
     *
     * @param player the keeper
     * @param creaturesController the creatures controller
     * @return the free Lair space
     */
    static int getFreeLairSpace(IPlayerController player, ICreaturesController creaturesController) {
        int space = 0;
        for (Entry<Room, Set<IRoomController>> rooms : player.getRoomControl().getRoomControllers().entrySet()) {
            if (rooms.getKey().getId() != LAIR_ROOM_ID) {
                continue;
            }
            for (IRoomController room : new ArrayList<>(rooms.getValue())) {
                space += room.getMaxCapacity() - room.getUsedCapacity();
            }
        }
        for (Entry<Creature, Set<EntityId>> creatures : player.getCreatureControl().getAllCreatures().entrySet()) {
            if (creatures.getKey().getLairObjectId() == 0) {
                continue;
            }
            for (EntityId entityId : new ArrayList<>(creatures.getValue())) {
                if (!creaturesController.createController(entityId).hasLair()) {
                    space--;
                }
            }
        }
        return space;
    }

    /**
     * Pick a creature type by its weight
     *
     * @param weights the creature types and their weights
     * @param roll a random number from 0 to the total weight
     * @return the creature type
     */
    static Creature pick(Map<Creature, Double> weights, double roll) {
        double cumulative = 0;
        Creature last = null;
        for (Entry<Creature, Double> weight : weights.entrySet()) {
            cumulative += weight.getValue();
            last = weight.getKey();
            if (roll < cumulative) {
                return last;
            }
        }
        return last;
    }

    private static int getFirstAttractionRoomId(Creature creature) {
        Creature.Attraction[] attractions = creature.getAttractions();
        if (attractions == null || attractions.length == 0) {
            return 0;
        }

        // The original takes the room of the first attraction whatever its type
        return attractions[0].getRoomId() & 0xFF;
    }

    /**
     * A unique creature's secret room layout: a room of the given type and
     * exact size, with the pattern of room types laid from its top left
     * corner, rows going down the map
     */
    private record UniqueLayout(short creatureId, int roomId, int roomSize, int[][] pattern) {

    }

}
