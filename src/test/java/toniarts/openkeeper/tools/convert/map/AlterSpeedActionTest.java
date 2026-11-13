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

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.logic.MovementSystem;
import toniarts.openkeeper.game.logic.CreatureViewSystem;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlterSpeedActionTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 255})
    void loadedCreatureActionUsesRuntimeWalkOrRunSpeed(int available) {
        Fixture f = new Fixture(2.5f, 6.25f);
        EntityId other = f.data.createEntity();
        Mobile otherMobile = new Mobile(false, true, false, 4);
        f.data.setComponent(other, otherMobile);
        Navigation navigation = new Navigation(new Point(0, 100), new Point(1, 101),
                List.of(new Vector2(0, 0), new Vector2(0, 100)));
        f.data.setComponent(f.actor, navigation);

        f.action(available, true);

        Mobile updated = f.mobile();
        assertEquals(available == 0 ? 2.5f : 6.25f, updated.maxSpeed);
        assertEquals(available != 0, updated.running);
        assertTrue(updated.canFly);
        assertFalse(updated.canWalkOnWater);
        assertTrue(updated.canWalkOnLava);
        assertSame(navigation, f.data.getComponent(f.actor, Navigation.class));
        assertSame(otherMobile, f.data.getComponent(other, Mobile.class));
    }

    @Test
    void runningThenWalkingChangesExistingMovementWithoutRestartingPath() {
        Fixture f = new Fixture(1, 3);
        Navigation navigation = new Navigation(new Point(0, 100), null,
                List.of(new Vector2(0, 0), new Vector2(0, 100)));
        f.data.setComponents(f.actor, new Position(0, new Vector3f()), navigation);
        MovementSystem movement = new MovementSystem(f.data);
        try {
            advance(movement, 180);
            assertEquals(1 / 60f, frameDistance(f, movement), 0.001f);
            f.action(1, true);
            assertSame(navigation, f.data.getComponent(f.actor, Navigation.class));
            advance(movement, 180);
            assertEquals(3 / 60f, frameDistance(f, movement), 0.001f);
            f.action(0, true);
            // Slowing down clamps the existing velocity on the next frame.
            assertEquals(1 / 60f, frameDistance(f, movement), 0.001f);
            assertSame(navigation, f.data.getComponent(f.actor, Navigation.class));
        } finally {
            movement.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void movementAnimationFollowsModeWithFallbackForMissingRunAssets(boolean hasRun) {
        Fixture f = new Fixture(1, 3);
        Creature definition = new Creature();
        if (hasRun) {
            ArtResource resource = new ArtResource();
            resource.setType(ArtResource.ArtResourceType.ANIMATING_MESH);
            definition.setAnimation(Creature.AnimationType.RUN, resource);
        }
        IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> {
            if (method.equals("getCreature")) return definition;
            throw new UnsupportedOperationException(method);
        });
        f.data.setComponents(f.actor, new Position(0, new Vector3f()),
                new CreatureViewState((short) 7, 0, Creature.AnimationType.WALK),
                new Navigation(new Point(0, 100), null, List.of(new Vector2(0, 0), new Vector2(0, 100))));
        CreatureViewSystem views = new CreatureViewSystem(f.data, kwd);
        try {
            f.action(1, true);
            views.processTick(0.25f);
            assertEquals(hasRun ? Creature.AnimationType.RUN : Creature.AnimationType.WALK,
                    f.data.getComponent(f.actor, CreatureViewState.class).state);
            f.action(0, true);
            views.processTick(0.25f);
            assertEquals(Creature.AnimationType.WALK,
                    f.data.getComponent(f.actor, CreatureViewState.class).state);
        } finally {
            views.stop();
        }
    }

    @Test
    void eachActionReadsTheLatestRuntimeSpeeds() {
        Fixture f = new Fixture(1, 3);
        f.action(1, true);
        assertEquals(3, f.mobile().maxSpeed);
        CreatureComponent scaled = new CreatureComponent();
        scaled.creatureId = 7;
        scaled.speed = 2;
        scaled.runSpeed = 7;
        f.data.setComponent(f.actor, scaled);
        f.action(1, true);
        assertEquals(7, f.mobile().maxSpeed);
        f.action(0, true);
        assertEquals(2, f.mobile().maxSpeed);
    }

    @Test
    void unboundCreatureActionDoesNothing() {
        Fixture f = new Fixture(1, 3);
        Mobile before = f.mobile();
        assertDoesNotThrow(() -> f.action(1, false));
        assertSame(before, f.mobile());
    }

    @Test
    void missingMobilityDoesNotCreateIt() {
        Fixture f = new Fixture(1, 3);
        f.data.removeComponent(f.actor, Mobile.class);
        assertDoesNotThrow(() -> f.action(1, true));
        assertNull(f.mobile());
    }

    @Test
    void removedCreatureDoesNotGetRecreated() {
        Fixture f = new Fixture(1, 3);
        f.data.removeEntity(f.actor);
        assertDoesNotThrow(() -> f.controller.setMovementSpeed(true));
        assertNull(f.mobile());
    }

    private static void advance(MovementSystem movement, int frames) {
        for (int i = 0; i < frames; i++) movement.processTick(1 / 60f);
    }

    private static float frameDistance(Fixture f, MovementSystem movement) {
        float before = f.data.getComponent(f.actor, Position.class).position.z;
        movement.processTick(1 / 60f);
        return f.data.getComponent(f.actor, Position.class).position.z - before;
    }

    private static final class Fixture {
        final DefaultEntityData data = new DefaultEntityData();
        final EntityId actor = data.createEntity();
        final CreatureController controller;

        Fixture(float walkSpeed, float runSpeed) {
            CreatureComponent stats = new CreatureComponent();
            stats.creatureId = 7;
            stats.speed = walkSpeed;
            stats.runSpeed = runSpeed;
            data.setComponents(actor, stats, new Mobile(true, false, true, walkSpeed));
            Creature definition = new Creature();
            definition.getAttributes().setSpeed(0.5f);
            definition.getAttributes().setRunSpeed(0.75f);
            controller = new CreatureController(actor, data, definition, null, null,
                    () -> 0, Map.of(), null, null, null, null, null, null, null);
        }

        Mobile mobile() {
            return data.getComponent(actor, Mobile.class);
        }

        void action(int available, boolean bound) {
            TriggerAction action = new TriggerAction(null);
            action.setId(1);
            action.setType(TriggerAction.ActionType.ALTER_SPEED);
            action.setUserData("available", (short) available);
            Thing.KeeperCreature thing = new Thing.KeeperCreature();
            thing.setTriggerId(1);
            thing.setPlayerId(Player.KEEPER1_ID);
            IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getThings" -> args[0] == Thing.KeeperCreature.class ? List.of(thing) : List.of();
                case "getTriggers" -> Map.of(1, action);
                default -> throw new UnsupportedOperationException(method);
            });
            ILevelInfo info = proxy(ILevelInfo.class, (method, args) -> {
                if (method.equals("getLevelData")) return kwd;
                throw new UnsupportedOperationException(method);
            });
            ICreaturesController creatures = proxy(ICreaturesController.class, (method, args) -> {
                if (method.equals("createController")) return controller;
                throw new UnsupportedOperationException(method);
            });
            if (bound) data.setComponent(actor, new toniarts.openkeeper.game.component.Trigger(1));
            CreatureTriggerLogicController scripts = new CreatureTriggerLogicController(
                    null, info, () -> 0, null, creatures, null, data);
            try {
                scripts.processTick(0.25f);
            } finally {
                scripts.stop();
            }
        }
    }

    private interface Call {
        Object apply(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> call.apply(method.getName(), args));
    }
}
