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

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementSystemTest {

    /**
     * A creature standing on 15,23 turns on the spot to face 16,25. Facing is
     * (sin r, cos r) on x, z for the Position rotation r.
     */
    @ParameterizedTest
    @ValueSource(floats = {0f, -0.46f, 1f, -1f, 3f, -3f})
    void turnsOnTheSpotToFaceTheTarget(float rotation) {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(rotation, new Vector3f(15, 0, 23)), new Mobile(false, false, false, 1),
                new Navigation(new Point(15, 23), new Point(16, 25), new ArrayList<>()));
        MovementSystem movementSystem = new MovementSystem(entityData);

        for (int frame = 0; frame < 180 && entityData.getComponent(creature, Navigation.class) != null; frame++) {
            movementSystem.processTick(1 / 60f);
        }

        assertNull(entityData.getComponent(creature, Navigation.class), "still turning after 3 seconds");
        float facing = entityData.getComponent(creature, Position.class).rotation;
        double cos = (Math.sin(facing) * 1 + Math.cos(facing) * 2) / Math.sqrt(5);
        assertTrue(cos > Math.cos(0.2), "facing " + Math.toDegrees(Math.acos(cos)) + " degrees away");
    }

    @Test
    void movementDoesNotOvershootTheEndOfAShortPath() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 5f),
                new Navigation(new Point(1, 0), null,
                        new ArrayList<>(List.of(new Vector2(0, 0), new Vector2(1, 0)))));

        new MovementSystem(entityData).processTick(0.5f);

        Position moved = entityData.getComponent(creature, Position.class);
        assertTrue(moved.position.x <= 1f, "overshot to " + moved.position.x);
        assertEquals(0f, moved.position.z, 0.001f);
    }

    @Test
    void movementContinuesPastIntermediateWaypoints() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 1.2f),
                new Navigation(new Point(2, 0), null,
                        new ArrayList<>(List.of(new Vector2(0, 0), new Vector2(1, 0), new Vector2(2, 0)))));
        MovementSystem movementSystem = new MovementSystem(entityData);

        for (int tick = 0; tick < 80; tick++) {
            movementSystem.processTick(1f / 20f);
        }

        Position moved = entityData.getComponent(creature, Position.class);
        assertEquals(2f, moved.position.x, MovementSettings.DEFAULT.waypointTolerance());
        assertNull(entityData.getComponent(creature, Navigation.class));
        movementSystem.stop();
        entityData.close();
    }

    @Test
    void movementRecomputesHeadingAfterTurningToNextWaypoint() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 5f),
                new Navigation(new Point(1, 1), null,
                        new ArrayList<>(List.of(new Vector2(0, 0), new Vector2(1, 0), new Vector2(1, 1)))));
        MovementSystem movementSystem = new MovementSystem(entityData);

        for (int tick = 0; tick < 3; tick++) {
            movementSystem.processTick(0.1f);
        }

        Position moved = entityData.getComponent(creature, Position.class);
        assertEquals(1f, moved.position.x, 0.001f);
        assertEquals(0.5f, moved.position.z, 0.001f);
        assertEquals(0f, moved.rotation, 0.001f);
        movementSystem.stop();
        entityData.close();
    }

    @Test
    void replacingNavigationResetsTheRouteCursor() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 5f),
                new Navigation(new Point(4, 0), null,
                        new ArrayList<>(List.of(new Vector2(0, 0), new Vector2(1, 0), new Vector2(2, 0),
                                new Vector2(3, 0), new Vector2(4, 0)))));
        MovementSystem movementSystem = new MovementSystem(entityData);
        movementSystem.processTick(0.2f);
        movementSystem.processTick(0.2f);

        entityData.setComponent(creature, new Navigation(new Point(1, 0), null,
                new ArrayList<>(List.of(new Vector2(2, 0), new Vector2(3, 0), new Vector2(0, 0),
                        new Vector2(1, 0)))));
        movementSystem.processTick(0.2f);

        Position moved = entityData.getComponent(creature, Position.class);
        assertEquals(3f, moved.position.x, 0.001f);
        movementSystem.stop();
        entityData.close();
    }

    @Test
    void movementKeepsProgressOnLongTurningRoute() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        List<Vector2> path = new ArrayList<>(49);
        float pathX = 0f;
        float pathZ = 0f;
        path.add(new Vector2(pathX, pathZ));
        for (int waypoint = 1; waypoint < 49; waypoint++) {
            switch ((waypoint - 1) % 4) {
                case 0 -> pathX += 1f;
                case 1 -> pathZ += 1f;
                case 2 -> pathX += 1f;
                default -> pathZ -= 1f;
            }
            path.add(new Vector2(pathX, pathZ));
        }
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 1.2f),
                new Navigation(new Point(Math.round(pathX), Math.round(pathZ)), null, path));
        MovementSystem movementSystem = new MovementSystem(entityData);

        for (int tick = 0; tick < 600; tick++) {
            movementSystem.processTick(1f / 20f);
        }

        Position moved = entityData.getComponent(creature, Position.class);
        assertTrue(moved.position.x > 10f, "stalled or reversed at x=" + moved.position.x);
        assertNotNull(entityData.getComponent(creature, Navigation.class));
        movementSystem.stop();
        entityData.close();
    }

    @Test
    void animationUsesTheMovementResultFromTheSameSimulationTick() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 5f),
                new Navigation(new Point(1, 0), null,
                        new ArrayList<>(List.of(new Vector2(0, 0), new Vector2(1, 0)))),
                new CreatureAi(0, CreatureState.IDLE, (short) 1),
                new CreatureViewState((short) 1, 0, Creature.AnimationType.WALK));
        MovementSystem movement = new MovementSystem(entityData);
        CreatureViewSystem animation = new CreatureViewSystem(entityData, null);
        GameLogicManager simulation = new GameLogicManager(movement, animation);

        simulation.processTick(0.5f);

        assertNull(entityData.getComponent(creature, Navigation.class));
        assertEquals(Creature.AnimationType.IDLE_1,
                entityData.getComponent(creature, CreatureViewState.class).state);
        simulation.stop();
        entityData.close();
    }

    @Test
    void animationUsesViewStateChangesMadeBeforeTheTick() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new CreatureAi(0, CreatureState.IDLE, (short) 1),
                new CreatureViewState((short) 1, 0, Creature.AnimationType.IDLE_1));
        CreatureViewSystem animation = new CreatureViewSystem(entityData, null);

        entityData.setComponent(creature,
                new CreatureViewState((short) 1, 0, Creature.AnimationType.WALK));
        animation.processTick(0.05f);

        assertEquals(Creature.AnimationType.IDLE_1,
                entityData.getComponent(creature, CreatureViewState.class).state);
        animation.stop();
        entityData.close();
    }

    @Test
    void aiMovementStopsBeforeAnImpassableTile() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 5f),
                new Navigation(new Point(2, 0), null,
                        new ArrayList<>(List.of(new Vector2(0, 0), new Vector2(1, 0), new Vector2(2, 0)))));
        MovementSettings settings = MovementSettings.DEFAULT;
        GridMovementCollision collision = new GridMovementCollision(
                PossessedMovementSystemTest.mapWithWallAtXOne(), null, settings);
        MovementSystem movementSystem = new MovementSystem(entityData, settings, collision,
                PossessedMovementSystemTest.creaturesWithWallAtXOne());

        movementSystem.processTick(0.5f);

        float x = entityData.getComponent(creature, Position.class).position.x;
        assertTrue(x > 0f && x <= 1f / 3f + 0.001f, "creature crossed wall to x=" + x);
        movementSystem.stop();
        entityData.close();
    }

    @Test
    void identicalTickSequencesProduceIdenticalMovement() {
        Position first = simulatePath();
        Position second = simulatePath();

        assertEquals(first.position.x, second.position.x, 0f);
        assertEquals(first.position.z, second.position.z, 0f);
        assertEquals(first.rotation, second.rotation, 0f);
    }

    private Position simulatePath() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0f, new Vector3f(0, 0, 0)),
                new Mobile(false, false, false, 1f),
                new Navigation(new Point(2, 0), null,
                        new ArrayList<>(List.of(new Vector2(0, 0), new Vector2(1, 0), new Vector2(2, 0)))));
        MovementSystem movementSystem = new MovementSystem(entityData);

        for (int i = 0; i < 120; i++) {
            movementSystem.processTick(1f / 60f);
        }

        Position position = entityData.getComponent(creature, Position.class);
        Position copy = new Position(position.rotation, position.position.clone());
        movementSystem.stop();
        entityData.close();
        return copy;
    }
}
