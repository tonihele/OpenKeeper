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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.trap.ITrapEffects;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapSystemTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final short SENTRY = 3;
    private static final short SPIKE = 10;
    private static final short CANNON_BALL = 44;
    private static final short SPIKE_SHOT = 27;
    private static final short IMP = 1;
    private static final short REAPER = 2;
    private static final Point TRAP_TILE = new Point(10, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Integer, Trap> traps = new HashMap<>();
    private final Map<Short, Creature> creatures = new HashMap<>();
    private final Set<Point> walls = new HashSet<>();
    private final List<Fired> shots = new ArrayList<>();
    private final Keeper keeper = new Keeper(false, KEEPER);
    private double gameTime = 0;
    private TrapSystem trapSystem;

    private record Fired(short trapId, EntityId target) {

    }

    TrapSystemTest() {
        traps.put((int) SENTRY, TrapFiringFixtures.withManaCost(
                TrapFiringFixtures.trap(SENTRY, Trap.TriggerType.LINE_OF_SIGHT, 5, 6, CANNON_BALL), 250));
        traps.put((int) SPIKE, TrapFiringFixtures.trap(SPIKE, Trap.TriggerType.PRESSURE, 1, 4, SPIKE_SHOT));
        creatures.put(IMP, TrapFiringFixtures.creature(IMP, Creature.CreatureFlag.IS_WORKER));
        creatures.put(REAPER, TrapFiringFixtures.creature(REAPER, Creature.CreatureFlag.IS_HORNY));
        keeper.setMana(1000);
    }

    @Test
    void sentryFiresAtEnemyInSight() {
        addTrap(SENTRY, TRAP_TILE);
        EntityId enemy = addCreature(IMP, ENEMY, 13, 10);

        tick();

        assertEquals(List.of(new Fired(SENTRY, enemy)), shots);
    }

    @Test
    void sentryPaysManaForTheShot() {
        addTrap(SENTRY, TRAP_TILE);
        addCreature(IMP, ENEMY, 13, 10);

        tick();

        assertEquals(750, keeper.getMana());
    }

    @Test
    void sentryDoesNotFireWithoutMana() {
        keeper.setMana(249);
        addTrap(SENTRY, TRAP_TILE);
        EntityId enemy = addCreature(IMP, ENEMY, 13, 10);

        tick();
        assertTrue(shots.isEmpty());

        // The trap keeps trying, it didn't start to recharge
        keeper.setMana(250);
        gameTime = 0.25;
        tick();
        assertEquals(List.of(new Fired(SENTRY, enemy)), shots);
    }

    @Test
    void sentryIgnoresEnemyOutOfRange() {
        addTrap(SENTRY, TRAP_TILE);
        addCreature(IMP, ENEMY, 10, 15);

        tick();

        assertTrue(shots.isEmpty());
    }

    @Test
    void sentryIgnoresOwnAndNeutralCreatures() {
        addTrap(SENTRY, TRAP_TILE);
        addCreature(IMP, KEEPER, 11, 10);
        addCreature(IMP, Player.NEUTRAL_PLAYER_ID, 12, 10);

        tick();

        assertTrue(shots.isEmpty());
    }

    @Test
    void wallBlocksTheSentrysSight() {
        addTrap(SENTRY, TRAP_TILE);
        addCreature(IMP, ENEMY, 13, 10);
        walls.add(new Point(12, 10));

        tick();

        assertTrue(shots.isEmpty());
    }

    @Test
    void sentryAimsAtTheNearestEnemy() {
        addTrap(SENTRY, TRAP_TILE);
        addCreature(IMP, ENEMY, 14, 10);
        EntityId nearest = addCreature(IMP, ENEMY, 10, 8);

        tick();

        assertEquals(List.of(new Fired(SENTRY, nearest)), shots);
    }

    @Test
    void sentryWaitsForItsRecharge() {
        addTrap(SENTRY, TRAP_TILE);
        EntityId enemy = addCreature(IMP, ENEMY, 13, 10);

        tick();
        gameTime = 5.75;
        tick();
        assertEquals(1, shots.size());

        gameTime = 6;
        tick();
        assertEquals(List.of(new Fired(SENTRY, enemy), new Fired(SENTRY, enemy)), shots);
    }

    @Test
    void trapWaitsForItsChargeTime() {
        TrapFiringFixtures.withChargeTime(traps.get((int) SENTRY), 0.5f);
        addTrap(SENTRY, TRAP_TILE);
        addCreature(IMP, ENEMY, 13, 10);

        tick();
        gameTime = 0.25;
        tick();
        assertTrue(shots.isEmpty());

        gameTime = 0.5;
        tick();
        assertEquals(1, shots.size());
    }

    @Test
    void spikeFiresWhenEnemyStepsOnIt() {
        addTrap(SPIKE, TRAP_TILE);
        EntityId enemy = addCreature(IMP, ENEMY, 10, 10);

        tick();

        assertEquals(List.of(new Fired(SPIKE, enemy)), shots);
    }

    @Test
    void spikeIgnoresEnemyNextToIt() {
        addTrap(SPIKE, TRAP_TILE);
        addCreature(IMP, ENEMY, 11, 10);

        tick();

        assertTrue(shots.isEmpty());
    }

    @Test
    void blueprintNeverFires() {
        EntityId trap = addTrap(SPIKE, TRAP_TILE);
        entityData.setComponent(trap, new TrapViewState(SPIKE, true));
        addCreature(IMP, ENEMY, 10, 10);

        tick();

        assertTrue(shots.isEmpty());
    }

    @Test
    void creaturesThatCanNotSetOffTrapsAreIgnored() {
        addTrap(SPIKE, TRAP_TILE);
        EntityId unconscious = addCreature(IMP, ENEMY, 10, 10);
        entityData.setComponent(unconscious, new Unconscious(0));
        EntityId inHand = addCreature(IMP, ENEMY, 10, 10);
        entityData.setComponent(inHand, new InHand());

        // The data flag OpenKeeper calls IS_HORNY, only the Horned Reaper has it
        addCreature(REAPER, ENEMY, 10, 10);

        tick();

        assertTrue(shots.isEmpty());
    }

    @Test
    void lineOfSightIsBlockedByWallsBetween() {
        walls.add(new Point(11, 11));

        assertTrue(TrapSystem.hasLineOfSight(this::isWall, new Vector3f(10, 1, 10), new Vector3f(10, 1, 13)));
        assertFalse(TrapSystem.hasLineOfSight(this::isWall, new Vector3f(10, 1, 10), new Vector3f(12, 1, 12)));
    }

    private void tick() {
        if (trapSystem == null) {
            trapSystem = new TrapSystem(entityData, kwdFile(), () -> gameTime, this::isWall, playerControllers(),
                    effects());
        }
        trapSystem.processTick(0.25f);
    }

    private EntityId addTrap(short trapId, Point tile) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new TrapComponent(trapId));
        entityData.setComponent(entityId, new TrapViewState(trapId, false));
        entityData.setComponent(entityId, new Owner(KEEPER, KEEPER));
        entityData.setComponent(entityId, new Health(1000, 1000));
        Vector3f position = WorldUtils.pointToVector3f(tile);
        position.y = WorldUtils.FLOOR_HEIGHT;
        entityData.setComponent(entityId, new Position(0, position));
        return entityId;
    }

    private EntityId addCreature(short creatureId, short ownerId, float x, float y) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entityId, creatureComponent);
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Health(100, 100));
        entityData.setComponent(entityId, new Position(0, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, y)));
        return entityId;
    }

    private boolean isWall(int x, int y) {
        return walls.contains(new Point(x, y));
    }

    private ITrapEffects effects() {
        return (trap, ownerId, origin, target) -> shots.add(new Fired(trap.getTrapId(), target));
    }

    private Map<Short, IPlayerController> playerControllers() {
        PlayerManaControl manaControl = new PlayerManaControl(keeper, Map.of(Variable.MiscVariable.MiscType.MAXIMUM_MANA_THRESHOLD,
                TrapFiringFixtures.miscVariable(Variable.MiscVariable.MiscType.MAXIMUM_MANA_THRESHOLD, 200000)));
        IPlayerController playerController = (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" ->
                keeper;
            case "getManaControl" ->
                manaControl;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        return Map.of(KEEPER, playerController);
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getTrapById" ->
                traps.get((Integer) args[0]);
            case "getCreature" ->
                creatures.get((Short) args[0]);
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

}
