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
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag2;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.CreatureAttractionFixtures.attraction;
import static toniarts.openkeeper.tools.convert.map.CreatureAttractionFixtures.room;
import static toniarts.openkeeper.tools.convert.map.PortalChoiceFixtures.creature;

/**
 * The Portal picks the creature it attracts by weight, as the original does:
 * creatures that want the Lair weigh 1 (Goblins 3), the others the capacity
 * of their rooms less the creatures of their type the keeper already has. A
 * type the keeper attracts for the first time is strongly favoured for a
 * while, and nothing comes while the keeper is short of Lair space.
 */
class PortalCreatureChoiceTest {

    private static final short PLAYER_ID = 3;
    private static final int ROOM = 1;
    private static final Predicate<Creature> ALL = creature -> true;

    private final Room lair = room((short) 2);
    private final Room portal = room((short) 3);
    private final Room library = room((short) 6);
    private final Room trainingRoom = room((short) 7);
    private final Room guardRoom = room((short) 9);
    private final Room tortureChamber = room((short) 12);
    private final Room temple = room((short) 13);
    private final Room heroPortal = room((short) 25);

    private final Creature warlock = creature((short) 5, (short) 27, flags(CreatureFlag.AVAILABLE_VIA_PORTAL),
            EnumSet.noneOf(CreatureFlag2.class), attraction(ROOM, 6, 0));
    private final Creature goblin = creature((short) 7, (short) 22, flags(CreatureFlag.AVAILABLE_VIA_PORTAL),
            EnumSet.noneOf(CreatureFlag2.class), attraction(ROOM, 2, 0));
    private final Creature salamander = creature((short) 11, (short) 24, flags(CreatureFlag.AVAILABLE_VIA_PORTAL),
            EnumSet.noneOf(CreatureFlag2.class), attraction(ROOM, 7, 0));
    private final Creature firefly = creature((short) 12, (short) 21, flags(CreatureFlag.AVAILABLE_VIA_PORTAL),
            EnumSet.noneOf(CreatureFlag2.class), attraction(ROOM, 2, 0));
    private final Creature knight = creature((short) 13, (short) 6, EnumSet.noneOf(CreatureFlag.class),
            EnumSet.of(CreatureFlag2.AVAILABLE_VIA_HERO_PORTAL), attraction(ROOM, 2, 0));
    private final Creature uniqueGoblin = creature((short) 33, (short) 22, flags(CreatureFlag.AVAILABLE_VIA_PORTAL, CreatureFlag.IS_UNIQUE),
            EnumSet.noneOf(CreatureFlag2.class), attraction(ROOM, 2, 0));
    private final Creature skeleton = creature((short) 9, (short) 0, flags(CreatureFlag.AVAILABLE_VIA_PORTAL),
            EnumSet.noneOf(CreatureFlag2.class), attraction(ROOM, 2, 0));

    private final Set<EntityId> creaturesWithoutLair = new HashSet<>();
    private final EntityData entityData = new DefaultEntityData();
    private final PortalCreatureChoice choice = new PortalCreatureChoice(entityData, new Random(7));
    private PlayerRoomControl rooms;
    private PlayerCreatureControl creatures;
    private IPlayerController player;
    private ICreaturesController creaturesController;
    private IRoomController portalRoom;
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        Keeper keeper = new Keeper(false, PLAYER_ID);
        rooms = new PlayerRoomControl(keeper, List.of(lair, portal, library, trainingRoom, guardRoom, tortureChamber, temple, heroPortal));
        creatures = new PlayerCreatureControl(keeper, null, List.of(warlock, goblin, salamander, firefly, knight, uniqueGoblin, skeleton));
        player = stub(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" ->
                keeper;
            case "getRoomControl" ->
                rooms;
            case "getCreatureControl" ->
                creatures;
            default ->
                throw new AssertionError(method);
        });
        creaturesController = stub(ICreaturesController.class, (method, args) -> switch (method) {
            case "createController" -> {
                EntityId entityId = (EntityId) args[0];
                yield stub(ICreatureController.class, (creatureMethod, creatureArgs) -> switch (creatureMethod) {
                    case "hasLair" ->
                        !creaturesWithoutLair.contains(entityId);
                    default ->
                        throw new AssertionError(creatureMethod);
                });
            }
            default ->
                throw new AssertionError(method);
        });
        portalRoom = build(portal, 9, 0, 0, 0);
    }

    @Test
    void lairCreaturesWeighOneAndGoblinsThree() {
        assertEquals(1, PortalCreatureChoice.getWeight(firefly, player));
        assertEquals(3, PortalCreatureChoice.getWeight(goblin, player));
    }

    @Test
    void roomCreatureWeighsItsRoomsCapacityLessItsKind() {
        assertEquals(0, PortalCreatureChoice.getWeight(warlock, player));

        // Two libraries with 4 and 2 pieces of furniture, a capacity of 5 and 3
        build(library, 16, 4, 0, 0);
        build(library, 9, 2, 0, 0);
        assertEquals(8, PortalCreatureChoice.getWeight(warlock, player));

        addCreatures(warlock, 3, false);
        assertEquals(5, PortalCreatureChoice.getWeight(warlock, player));

        addCreatures(warlock, 6, false);
        assertEquals(0, PortalCreatureChoice.getWeight(warlock, player));
    }

    @Test
    void attractionCapacityOfTheRooms() {
        assertEquals(5, PortalCreatureChoice.getAttractionCapacity(build(library, 16, 4, 0, 0)));
        assertEquals(0, PortalCreatureChoice.getAttractionCapacity(build(library, 4, 0, 0, 0)));
        assertEquals(3, PortalCreatureChoice.getAttractionCapacity(build(trainingRoom, 9, 2, 0, 0)));
        assertEquals(3, PortalCreatureChoice.getAttractionCapacity(build(tortureChamber, 9, 3, 0, 0)));
        assertEquals(2, PortalCreatureChoice.getAttractionCapacity(build(temple, 25, 1, 0, 0)));
        assertEquals(0, PortalCreatureChoice.getAttractionCapacity(build(temple, 16, 0, 0, 0)));
        assertEquals(12, PortalCreatureChoice.getAttractionCapacity(build(guardRoom, 12, 4, 0, 0)));
        assertEquals(0, PortalCreatureChoice.getAttractionCapacity(portalRoom));
    }

    @Test
    void freeLairSpaceLessTheCreaturesWithoutABed() {
        build(lair, 6, 0, 5, 2);
        assertEquals(3, PortalCreatureChoice.getFreeLairSpace(player, creaturesController));

        addCreatures(goblin, 2, true);
        addCreatures(warlock, 1, false);
        assertEquals(1, PortalCreatureChoice.getFreeLairSpace(player, creaturesController));

        // Creatures that don't sleep in the Lair don't need space there
        addCreatures(skeleton, 2, true);
        assertEquals(1, PortalCreatureChoice.getFreeLairSpace(player, creaturesController));
    }

    @Test
    void nothingComesWhileShortOfLairSpace() {
        setAvailable(goblin);
        build(lair, 1, 0, 1, 1);
        addCreatures(goblin, 1, true);
        assertNull(choice.chooseCreature(player, portalRoom, creaturesController, ALL));

        build(lair, 1, 0, 1, 0);
        assertNotNull(choice.chooseCreature(player, portalRoom, creaturesController, ALL));
    }

    @Test
    void creaturesComeWithoutALairUntilOneIsLeftWithoutABed() {
        setAvailable(firefly);
        assertSame(firefly, choice.chooseCreature(player, portalRoom, creaturesController, ALL));

        addCreatures(firefly, 1, true);
        assertNull(choice.chooseCreature(player, portalRoom, creaturesController, ALL));
    }

    @Test
    void portalAttractsOnlyCreaturesAvailableViaItsKind() {
        setAvailable(goblin, uniqueGoblin, knight);
        IRoomController heroPortalRoom = build(heroPortal, 9, 0, 0, 0);

        Map<Creature, Double> viaPortal = choice.getCandidateWeights(player, portalRoom, creaturesController, ALL);
        assertEquals(Set.of(goblin), viaPortal.keySet());

        Map<Creature, Double> viaHeroPortal = choice.getCandidateWeights(player, heroPortalRoom, creaturesController, ALL);
        assertEquals(Set.of(knight), viaHeroPortal.keySet());
    }

    @Test
    void candidatesMustPassTheAvailabilityCheck() {
        setAvailable(goblin, firefly);

        Map<Creature, Double> weights = choice.getCandidateWeights(player, portalRoom, creaturesController, creature -> creature != goblin);

        assertEquals(Set.of(firefly), weights.keySet());
    }

    @Test
    void firstTimeAttractableTypeIsFavouredForTenGenerations() {
        setAvailable(firefly, salamander);
        choice.start(player, creaturesController, ALL);
        build(trainingRoom, 9, 2, 0, 0);

        // Capacity 3: x200 on the first generation, then x9 down to x1
        double[] expected = {600, 27, 24, 21, 18, 15, 12, 9, 6, 3, 3, 3};
        for (double weight : expected) {
            Map<Creature, Double> weights = choice.getCandidateWeights(player, portalRoom, creaturesController, ALL);
            assertEquals(weight, weights.get(salamander));
            assertEquals(1.0, weights.get(firefly));
            assertNotNull(choice.chooseCreature(player, portalRoom, creaturesController, ALL));
        }
    }

    @Test
    void favourIsKeptInTheEntityData() {
        setAvailable(salamander);
        build(trainingRoom, 9, 2, 0, 0);
        assertNotNull(choice.chooseCreature(player, portalRoom, creaturesController, ALL));

        // A loaded game starts with a new choice on the same entities
        PortalCreatureChoice loaded = new PortalCreatureChoice(entityData, new Random(7));

        assertEquals(27.0, loaded.getCandidateWeights(player, portalRoom, creaturesController, ALL).get(salamander));
    }

    @Test
    void typesAttractableAtTheStartAreNotFavoured() {
        setAvailable(salamander);
        build(trainingRoom, 9, 2, 0, 0);
        choice.start(player, creaturesController, ALL);

        Map<Creature, Double> weights = choice.getCandidateWeights(player, portalRoom, creaturesController, ALL);

        assertEquals(3.0, weights.get(salamander));
    }

    @Test
    void lairCreaturesAreNeverFavoured() {
        setAvailable(goblin);

        Map<Creature, Double> weights = choice.getCandidateWeights(player, portalRoom, creaturesController, ALL);

        assertEquals(3.0, weights.get(goblin));
    }

    @Test
    void favourDoesNotRunOutWithoutCandidates() {
        setAvailable(salamander);
        choice.start(player, creaturesController, ALL);
        build(trainingRoom, 9, 2, 0, 0);
        assertEquals(600.0, choice.getCandidateWeights(player, portalRoom, creaturesController, ALL).get(salamander));

        // While nothing can come, the generations don't count
        assertNull(choice.chooseCreature(player, portalRoom, creaturesController, creature -> false));
        assertEquals(600.0, choice.getCandidateWeights(player, portalRoom, creaturesController, ALL).get(salamander));
    }

    @Test
    void pickFollowsTheWeights() {
        Map<Creature, Double> weights = new LinkedHashMap<>();
        weights.put(goblin, 3.0);
        weights.put(firefly, 1.0);
        weights.put(warlock, 6.0);

        assertSame(goblin, PortalCreatureChoice.pick(weights, 0.0));
        assertSame(goblin, PortalCreatureChoice.pick(weights, 2.99));
        assertSame(firefly, PortalCreatureChoice.pick(weights, 3.0));
        assertSame(warlock, PortalCreatureChoice.pick(weights, 4.0));
        assertSame(warlock, PortalCreatureChoice.pick(weights, 9.99));
    }

    @Test
    void choiceFavoursTheHeavierCreature() {
        setAvailable(goblin, firefly);
        int goblins = 0;
        for (int i = 0; i < 4000; i++) {
            if (choice.chooseCreature(player, portalRoom, creaturesController, ALL) == goblin) {
                goblins++;
            }
        }

        // 3 to 1
        assertTrue(goblins > 2800 && goblins < 3200, "goblins " + goblins);
    }

    private void setAvailable(Creature... types) {
        for (Creature type : types) {
            creatures.setTypeAvailable(type, true);
        }
    }

    private void addCreatures(Creature type, int count, boolean withoutLair) {
        for (int i = 0; i < count; i++) {
            EntityId entityId = new EntityId(nextId++);
            creatures.onCreatureAdded(entityId, type);
            if (withoutLair) {
                creaturesWithoutLair.add(entityId);
            }
        }
    }

    private IRoomController build(Room room, int tiles, int floorFurniture, int maxCapacity, int usedCapacity) {
        RoomInstance instance = new RoomInstance(room);
        instance.setOwnerId(PLAYER_ID);
        for (int i = 0; i < tiles; i++) {
            instance.addCoordinate(new Point(i, rooms.getTypeCount()));
        }
        IRoomController controller = stub(IRoomController.class, (method, args) -> switch (method) {
            case "getRoom" ->
                room;
            case "getRoomInstance" ->
                instance;
            case "isDungeonHeart" ->
                false;
            case "getFloorFurnitureCount" ->
                floorFurniture;
            case "getWallFurnitureCount" ->
                0;
            case "getMaxCapacity" ->
                maxCapacity;
            case "getUsedCapacity" ->
                usedCapacity;
            default ->
                throw new AssertionError(method);
        });
        rooms.onBuild(controller);
        return controller;
    }

    private static EnumSet<CreatureFlag> flags(CreatureFlag... flags) {
        EnumSet<CreatureFlag> set = EnumSet.noneOf(CreatureFlag.class);
        set.addAll(List.of(flags));
        return set;
    }

    private interface Calls {

        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                calls.call(method.getName(), args);
        }));
    }
}
