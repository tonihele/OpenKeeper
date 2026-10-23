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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.GraveyardController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomCorpseControl;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GraveyardFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveyardSystemTest {

    private static final short PLAYER = 3;
    private static final short VAMPIRE = 8;
    private static final short GOBLIN = 7;
    private static final Point CRYPT_LOCATION = new Point(7, 8);

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId graveyardEntity = entityData.createEntity();
    private final Set<EntityId> furniture = new HashSet<>();
    private final List<Object[]> spawned = new ArrayList<>();
    private final RoomInstance roomInstance = new RoomInstance(GraveyardFixtures.graveyard(VAMPIRE));
    private final IRoomController graveyard = createGraveyard();
    private final RoomCorpseControl storage = new RoomCorpseControl(null, graveyard, entityData, () -> 0) {
        @Override
        protected int getNumberOfAccessibleTiles() {
            return roomInstance.getCoordinates().size();
        }
    };
    private final GraveyardSystem system = new GraveyardSystem(entityData, kwdFile(), Map.of(
            Variable.MiscVariable.MiscType.DECOMPOSE_VALUE_NEEDED_FOR_VAMPIRE,
            GraveyardFixtures.miscVariable(Variable.MiscVariable.MiscType.DECOMPOSE_VALUE_NEEDED_FOR_VAMPIRE, 1000)),
            mapController(), creaturesController());

    @Test
    void vampireRisesAtACryptWhenEnoughBodiesHaveDecomposed() {
        addCrypt();
        EntityId first = storeCorpse(new Point(1, 1), 600);
        EntityId second = storeCorpse(new Point(2, 1), 600);
        EntityId third = storeCorpse(new Point(3, 1), 600);
        system.processTick(0.25f);

        decompose(first);
        assertTrue(spawned.isEmpty());

        decompose(second);
        assertEquals(1, spawned.size());
        assertEquals(VAMPIRE, spawned.get(0)[0]);
        assertEquals(PLAYER, spawned.get(0)[1]);
        assertEquals(1, spawned.get(0)[2]);
        assertEquals(WorldUtils.pointToVector2f(CRYPT_LOCATION), spawned.get(0)[3]);
        assertEquals(ICreaturesController.SpawnType.PLACE, spawned.get(0)[4]);

        // The decomposition count starts again from zero
        decompose(third);
        assertEquals(1, spawned.size());
        assertEquals(0, storage.getCurrentCapacity());
    }

    @Test
    void withoutCryptsTheVampireRisesAtTheRoomCentre() {
        EntityId corpse = storeCorpse(new Point(1, 1), 1000);
        system.processTick(0.25f);

        decompose(corpse);

        assertEquals(1, spawned.size());
        assertEquals(WorldUtils.pointToVector2f(roomInstance.getCenter()), spawned.get(0)[3]);
    }

    @Test
    void releasedCorpsesDoNotDecomposeInTheGraveyard() {
        EntityId corpse = storeCorpse(new Point(1, 1), 1000);
        system.processTick(0.25f);

        storage.removeItem(corpse);
        system.processTick(0.25f);
        entityData.removeEntity(corpse);
        system.processTick(0.25f);

        assertTrue(spawned.isEmpty());
    }

    @Test
    void graveyardSizeLimitsTheNumberOfVampires() {
        // Four graveyard tiles allow four vampires
        EntityId vampire = null;
        for (int i = 0; i < 4; i++) {
            vampire = createCreature(VAMPIRE, PLAYER);
        }
        createCreature(GOBLIN, PLAYER);
        createCreature(VAMPIRE, (short) 4);
        EntityId first = storeCorpse(new Point(1, 1), 1000);
        EntityId second = storeCorpse(new Point(2, 1), 0);
        system.processTick(0.25f);

        decompose(first);
        assertTrue(spawned.isEmpty());

        // The value is kept, once there is room the next body raises the vampire
        entityData.removeComponent(vampire, Health.class);
        decompose(second);
        assertEquals(1, spawned.size());
    }

    @Test
    void decomposedCorpseLeavesTheGraveyardStorage() {
        EntityId corpse = storeCorpse(new Point(1, 1), 10);
        system.processTick(0.25f);

        decompose(corpse);

        assertEquals(0, storage.getCurrentCapacity());
        assertTrue(storage.getItems(new Point(1, 1)) == null || storage.getItems(new Point(1, 1)).isEmpty());
    }

    private void decompose(EntityId corpse) {
        entityData.removeEntity(corpse);
        system.processTick(0.25f);
    }

    private EntityId storeCorpse(Point p, int decomposeValue) {
        EntityId corpse = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = GOBLIN;
        creatureComponent.decomposeValue = decomposeValue;
        entityData.setComponents(corpse, creatureComponent, new Death(0), new Owner(PLAYER, PLAYER));
        storage.addItem(corpse, p);
        assertEquals(graveyardEntity, entityData.getComponent(corpse, Stored.class).room);
        return corpse;
    }

    private EntityId createCreature(short creatureId, short ownerId) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(creature, creatureComponent, new Health(10, 10), new Owner(ownerId, ownerId));
        return creature;
    }

    private void addCrypt() {
        EntityId crypt = entityData.createEntity();
        entityData.setComponents(crypt,
                new ObjectComponent(GraveyardController.OBJECT_CRYPT_ID, null),
                new Position(0, WorldUtils.pointToVector3f(CRYPT_LOCATION)));
        furniture.add(crypt);
        EntityId tombStone = entityData.createEntity();
        entityData.setComponents(tombStone,
                new ObjectComponent((short) 31, null),
                new Position(0, WorldUtils.pointToVector3f(new Point(1, 2))));
        furniture.add(tombStone);
    }

    private IRoomController createGraveyard() {
        roomInstance.setOwnerId(PLAYER);
        for (int x = 1; x <= 4; x++) {
            roomInstance.addCoordinate(new Point(x, 1));
        }
        return (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> graveyardEntity;
                    case "getOwnerId" -> PLAYER;
                    case "getRoom" -> roomInstance.getRoom();
                    case "getRoomInstance" -> roomInstance;
                    case "getFloorFurniture" -> furniture;
                    case "getObjectControl" -> storage;
                    case "isTileAccessible" -> true;
                    case "isRemoved", "isDestroyed" -> false;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private IMapController mapController() {
        return (IMapController) Proxy.newProxyInstance(
                IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomController" -> graveyardEntity.equals(args[0]) ? graveyard : null;
                    case "getRoomsByFunction" -> List.of(graveyard);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(
                ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "spawnCreature" -> {
                        spawned.add(args);
                        yield entityData.createEntity();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IKwdFile kwdFile() {
        List<Creature> creatures = List.of(
                GraveyardFixtures.creature(VAMPIRE, Creature.CreatureFlag.CAN_BE_RESURECTED),
                GraveyardFixtures.creature(GOBLIN, Creature.CreatureFlag.GENERATE_DEAD_BODY));
        return (IKwdFile) Proxy.newProxyInstance(
                IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getCreatureList" -> creatures;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
