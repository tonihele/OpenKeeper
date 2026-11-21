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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CombatPitFight;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.CombatPitFixtures.pitRoom;

/**
 * The Combat Pit is walled in, its door is the way in and out
 */
class CombatPitControllerTest {

    @Test
    void doorIsTheOnlyWayInAndOut() {
        CombatPitController pit = buildPit(5);

        // The inside is 2..4, the door is one of the edge tiles north of it
        int doors = 0;
        for (int x = 2; x <= 4; x++) {
            if (pit.isTileAccessible(x, 2, x, 1) && pit.isTileAccessible(x, 1, x, 2)) {
                doors++;
            }
        }
        assertEquals(1, doors);
        assertFalse(pit.isTileAccessible(1, 3, 2, 3));
        assertFalse(pit.isTileAccessible(3, 4, 3, 5));
    }

    @Test
    void fighterWaitsOnlyInsideThePit() {
        CombatPitController pit = buildPit(5);

        Set<Point> tiles = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            tiles.add(pit.getRandomFightingTile());
        }

        // Every tile of the inside 2..4 and none of the edge
        assertEquals(9, tiles.size());
        assertTrue(tiles.stream().allMatch(pit::isFightingTile));
    }

    @Test
    void spectatorsStandOnTheTilesRoundThePit() {
        CombatPitController pit = buildPit(5);

        List<Point> tiles = pit.getSpectatorTiles();

        assertEquals(16, tiles.size());
        assertTrue(tiles.stream().noneMatch(pit::isFightingTile));
    }

    @Test
    void fightIsOnWithAFighterOnItsFeetAndWatchedFourSecondsLonger() {
        EntityData entityData = new DefaultEntityData();
        double[] gameTime = {10};
        CombatPitController pit = buildPit(entityData, 5, () -> gameTime[0]);
        assertFalse(pit.isFightToWatch());

        EntityId fighter = creature(entityData, Player.KEEPER2_ID, false);
        pit.addFighter(fighter, new Point(3, 3));
        assertTrue(pit.isFightOn());
        assertEquals(10, entityData.getComponent(pit.getEntityId(), CombatPitFight.class).lastFightTime);

        // Knocked out, the fight is over, but it was seen a moment ago
        entityData.setComponent(fighter, new Unconscious(11));
        gameTime[0] = 14;
        assertFalse(pit.isFightOn());
        assertTrue(pit.isFightToWatch());
        gameTime[0] = 14.25;
        assertFalse(pit.isFightToWatch());
    }

    @Test
    void prisonerDroppedIntoThePitFightsHeldByThePitsKeeper() {
        EntityData entityData = new DefaultEntityData();
        CombatPitController pit = buildPit(entityData, 5);
        EntityId prisoner = creature(entityData, Player.KEEPER2_ID, false);
        EntityId own = creature(entityData, Player.KEEPER1_ID, false);

        assertTrue(pit.addFighter(prisoner, new Point(3, 3)));
        assertTrue(pit.addFighter(own, new Point(2, 4)));

        assertEquals(pit.getEntityId(), entityData.getComponent(prisoner, CombatPitFighter.class).pit);
        assertEquals(Player.KEEPER2_ID, entityData.getComponent(prisoner, Owner.class).ownerId);
        assertEquals(Player.KEEPER1_ID, entityData.getComponent(prisoner, Owner.class).controlId);
        assertEquals(Player.KEEPER1_ID, entityData.getComponent(own, Owner.class).controlId);
    }

    @Test
    void edgeOfThePitAndWorkersTakeNoFight() {
        EntityData entityData = new DefaultEntityData();
        CombatPitController pit = buildPit(entityData, 5);
        EntityId prisoner = creature(entityData, Player.KEEPER2_ID, false);
        EntityId imp = creature(entityData, Player.KEEPER2_ID, true);

        assertFalse(pit.addFighter(prisoner, new Point(1, 3)));
        assertFalse(pit.addFighter(imp, new Point(3, 3)));

        assertNull(entityData.getComponent(prisoner, CombatPitFighter.class));
        assertEquals(Player.KEEPER2_ID, entityData.getComponent(prisoner, Owner.class).controlId);
        assertNull(entityData.getComponent(imp, CombatPitFighter.class));
    }

    @Test
    void fightersOfARemovedOrCapturedPitWalkFree() {
        EntityData entityData = new DefaultEntityData();
        CombatPitController removed = buildPit(entityData, 5);
        CombatPitController captured = buildPit(entityData, 5);
        CombatPitController other = buildPit(entityData, 5);
        EntityId prisoner = creature(entityData, Player.KEEPER2_ID, false);
        EntityId secondPrisoner = creature(entityData, Player.KEEPER2_ID, false);
        EntityId elsewhere = creature(entityData, Player.KEEPER2_ID, false);
        removed.addFighter(prisoner, new Point(3, 3));
        captured.addFighter(secondPrisoner, new Point(3, 3));
        other.addFighter(elsewhere, new Point(3, 3));

        removed.remove();
        captured.captured(Player.KEEPER3_ID);

        for (EntityId freed : new EntityId[]{prisoner, secondPrisoner}) {
            assertNull(entityData.getComponent(freed, CombatPitFighter.class));
            assertEquals(Player.KEEPER2_ID, entityData.getComponent(freed, Owner.class).controlId);
        }
        assertEquals(other.getEntityId(), entityData.getComponent(elsewhere, CombatPitFighter.class).pit);
        assertEquals(Player.KEEPER1_ID, entityData.getComponent(elsewhere, Owner.class).controlId);
    }

    private static EntityId creature(EntityData entityData, short ownerId, boolean worker) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.worker = worker;
        entityData.setComponents(creature, creatureComponent, new Owner(ownerId, ownerId));
        return creature;
    }

    private static CombatPitController buildPit(int size) {
        return buildPit(new DefaultEntityData(), size);
    }

    private static CombatPitController buildPit(EntityData entityData, int size) {
        return buildPit(entityData, size, () -> 0);
    }

    private static CombatPitController buildPit(EntityData entityData, int size, IGameTimer gameTimer) {
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);
        RoomInstance roomInstance = new RoomInstance(pitRoom());
        roomInstance.setOwnerId(Player.KEEPER1_ID);
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        CombatPitController pit = new CombatPitController(entityData.createEntity(), entityData, null, roomInstance, objectsController, gameTimer);
        pit.construct();
        return pit;
    }
}
