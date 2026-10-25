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
import java.util.EnumSet;
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
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapFiring;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.IShotsController;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapSystemTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final short SENTRY = 3;
    private static final short SPIKE = 10;
    private static final short FEAR = 8;
    private static final short TRIGGER = 9;
    private static final short GUARD_POST = 11;
    private static final short GAS = 1;
    private static final short CANNON_BALL = 44;
    private static final short SPIKE_SHOT = 27;
    private static final short FEAR_SHOT = 25;
    private static final short TRIGGER_SHOT = 39;
    private static final short GAS_SHOT = 58;
    private static final short IMP = 1;
    private static final short REAPER = 2;
    private static final short THIEF = 18;
    private static final Point TRAP_TILE = new Point(10, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Integer, Trap> traps = new HashMap<>();
    private final Map<Short, Creature> creatures = new HashMap<>();
    private final Set<Point> walls = new HashSet<>();
    private final List<Fired> shots = new ArrayList<>();
    private final List<Object[]> flyingShots = new ArrayList<>();
    private final List<EntityId> entities = new ArrayList<>();
    private boolean shotsFly = false;
    private final Keeper keeper = new Keeper(false, KEEPER);
    private double gameTime = 0;
    private TrapSystem trapSystem;

    private record Fired(short trapId, EntityId target) {

    }

    TrapSystemTest() {
        traps.put((int) SENTRY, TrapFiringFixtures.withShotOffset(TrapFiringFixtures.withManaCost(
                TrapFiringFixtures.trap(SENTRY, Trap.TriggerType.LINE_OF_SIGHT, 5, 6, CANNON_BALL), 250), 0.5f, 0.53f));
        traps.put((int) SPIKE, TrapFiringFixtures.trap(SPIKE, Trap.TriggerType.PRESSURE, 1, 4, SPIKE_SHOT));
        traps.put((int) FEAR, TrapFiringFixtures.withThreat(
                TrapFiringFixtures.trap(FEAR, Trap.TriggerType.LINE_OF_SIGHT, 3, 6, FEAR_SHOT), 600, 60));
        traps.put((int) TRIGGER, TrapFiringFixtures.trap(TRIGGER, Trap.TriggerType.TRIGGER, 1, 4, TRIGGER_SHOT));
        traps.put((int) GUARD_POST, TrapFiringFixtures.trap(GUARD_POST, Trap.TriggerType.PRESSURE, 6, 1, (short) 0));
        traps.put((int) GAS, TrapFiringFixtures.trap(GAS, Trap.TriggerType.PRESSURE, 1, 4, GAS_SHOT));
        traps.get((int) GAS).getFlags().addAll(EnumSet.of(Trap.TrapFlag.INVISIBLE, Trap.TrapFlag.REVEAL_WHEN_FIRED));
        creatures.put(IMP, TrapFiringFixtures.creature(IMP, Creature.CreatureFlag.IS_WORKER));
        creatures.put(REAPER, TrapFiringFixtures.creature(REAPER, Creature.CreatureFlag.IS_HORNY));
        creatures.put(THIEF, TrapFiringFixtures.creature(THIEF, Creature.CreatureFlag.REVEALS_ADJACENT_TRAPS));
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
    void sentrySendsAFlyingShotAfterTheEnemy() {
        shotsFly = true;
        EntityId sentry = addTrap(SENTRY, TRAP_TILE);
        EntityId enemy = addCreature(IMP, ENEMY, 13, 10);

        tick();

        assertTrue(shots.isEmpty());
        assertEquals(1, flyingShots.size());
        Object[] shot = flyingShots.get(0);
        assertEquals(CANNON_BALL, shot[0]);
        assertEquals(new Vector3f(10.5f, WorldUtils.FLOOR_HEIGHT + 0.53f, 10), shot[4]);
        assertEquals(enemy, shot[5]);
        assertEquals(sentry, shot[6]);
        assertEquals(0f, shot[7]);
    }

    @Test
    void pressureTrapShotHitsAtOnce() {
        shotsFly = true;
        addTrap(SPIKE, TRAP_TILE);
        EntityId enemy = addCreature(IMP, ENEMY, 10, 10);

        tick();

        assertTrue(flyingShots.isEmpty());
        assertEquals(List.of(new Fired(SPIKE, enemy)), shots);
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
    void fearTrapThreatensOnlyAfterItFires() {
        EntityId trap = addTrap(FEAR, TRAP_TILE);

        tick();
        assertNull(entityData.getComponent(trap, Threat.class));

        addCreature(IMP, ENEMY, 12, 10);
        gameTime = 1;
        tick();
        assertEquals(600, entityData.getComponent(trap, Threat.class).threat);
    }

    @Test
    void threatLastsItsDurationFromTheLastShot() {
        EntityId trap = addTrap(FEAR, TRAP_TILE);
        EntityId enemy = addCreature(IMP, ENEMY, 12, 10);

        tick();

        // Fires again after the recharge, which renews the threat
        gameTime = 6;
        tick();
        entityData.removeComponent(enemy, Position.class);
        gameTime = 65.75;
        tick();
        assertEquals(600, entityData.getComponent(trap, Threat.class).threat);

        gameTime = 66;
        tick();
        assertNull(entityData.getComponent(trap, Threat.class));
    }

    @Test
    void trapsWithoutThreatMakeNone() {
        EntityId trap = addTrap(SENTRY, TRAP_TILE);
        addCreature(IMP, ENEMY, 13, 10);

        tick();

        assertNull(entityData.getComponent(trap, Threat.class));
    }

    @Test
    void trapNextToTriggerTrapLeavesTheTriggeringToIt() {
        addTrap(TRIGGER, TRAP_TILE);
        addTrap(SPIKE, new Point(11, 10));
        addCreature(IMP, ENEMY, 11, 10);

        tick();

        assertTrue(shots.isEmpty());
    }

    @Test
    void trapDiagonalToTriggerTrapStillTriggersItself() {
        addTrap(TRIGGER, TRAP_TILE);
        addTrap(SPIKE, new Point(11, 11));
        EntityId enemy = addCreature(IMP, ENEMY, 11, 11);

        tick();

        assertEquals(List.of(new Fired(SPIKE, enemy)), shots);
    }

    @Test
    void triggerTrapSetsOffTrapsAsItsWaveReachesThem() {
        addTrap(TRIGGER, TRAP_TILE);
        addTrap(SPIKE, new Point(12, 10));
        addTrap(SPIKE, new Point(10, 13));
        addTrap(SPIKE, new Point(15, 10));
        walls.add(new Point(10, 12));
        EntityId enemy = addCreature(IMP, ENEMY, 10, 10);

        tick();
        assertEquals(List.of(new Fired(TRIGGER, enemy)), shots);

        gameTime = 1.75;
        tick();
        assertEquals(1, shots.size());

        // The trap that sees the victim aims at it, the one behind the wall just fires
        gameTime = 2;
        tick();
        assertEquals(new Fired(SPIKE, enemy), shots.get(1));
        gameTime = 3;
        tick();
        assertEquals(new Fired(SPIKE, null), shots.get(2));

        // The wave ends at four tiles
        gameTime = 5;
        tick();
        assertEquals(2, shots.stream().filter(fired -> fired.trapId() == SPIKE).count());
    }

    @Test
    void triggerTrapLeavesOtherKeepersAndRechargingTrapsAlone() {
        addTrap(TRIGGER, TRAP_TILE);
        EntityId enemyTrap = addTrap(SPIKE, new Point(12, 10));
        entityData.setComponent(enemyTrap, new Owner(ENEMY, ENEMY));
        EntityId rechargingTrap = addTrap(SPIKE, new Point(8, 10));
        entityData.setComponent(rechargingTrap, new TrapFiring(10, TrapFiring.NOT_CHARGING, 0));
        EntityId enemy = addCreature(IMP, ENEMY, 10, 10);

        tick();
        entityData.removeComponent(enemy, Position.class);
        for (gameTime = 0.25; gameTime <= 5; gameTime += 0.25) {
            tick();
        }

        assertEquals(List.of(TRIGGER), shots.stream().map(Fired::trapId).toList());
    }

    @Test
    void trapWithoutShotNeverFires() {
        EntityId guardPost = addTrap(GUARD_POST, TRAP_TILE);
        addCreature(IMP, ENEMY, 10, 10);

        tick();

        assertTrue(shots.isEmpty());
        assertNull(entityData.getComponent(guardPost, TrapFiring.class));
    }

    @Test
    void hiddenTrapShowsItselfWhenItFires() {
        EntityId trap = addHiddenTrap(GAS, TRAP_TILE);
        addCreature(IMP, ENEMY, 10, 10);

        tick();

        assertFalse(entityData.getComponent(trap, TrapViewState.class).hidden);
    }

    @Test
    void hiddenTrapStaysHiddenUntilItFires() {
        EntityId trap = addHiddenTrap(GAS, TRAP_TILE);
        addCreature(IMP, ENEMY, 11, 10);

        tick();

        assertTrue(entityData.getComponent(trap, TrapViewState.class).hidden);
    }

    @Test
    void trapThatIsNotRevealedWhenFiredStaysHidden() {
        EntityId trap = addHiddenTrap(SPIKE, TRAP_TILE);
        addCreature(IMP, ENEMY, 10, 10);

        tick();

        assertEquals(1, shots.size());
        assertTrue(entityData.getComponent(trap, TrapViewState.class).hidden);
    }

    @Test
    void creatureThatRevealsTrapsRevealsTheEnemyTrapsAroundIt() {
        EntityId diagonal = addHiddenTrap(SPIKE, TRAP_TILE);
        EntityId below = addHiddenTrap(SPIKE, new Point(10, 12));
        EntityId twoTilesAway = addHiddenTrap(SPIKE, new Point(9, 13));
        addCreature(THIEF, ENEMY, 11, 11);

        tick();

        assertFalse(entityData.getComponent(diagonal, TrapViewState.class).hidden);
        assertFalse(entityData.getComponent(below, TrapViewState.class).hidden);
        assertTrue(entityData.getComponent(twoTilesAway, TrapViewState.class).hidden);
    }

    @Test
    void ownAndOrdinaryCreaturesRevealNoTraps() {
        EntityId trap = addHiddenTrap(SPIKE, TRAP_TILE);
        addCreature(THIEF, KEEPER, 11, 10);
        addCreature(IMP, ENEMY, 10, 11);

        tick();

        assertTrue(entityData.getComponent(trap, TrapViewState.class).hidden);
    }

    @Test
    void lineOfSightIsBlockedByWallsBetween() {
        walls.add(new Point(11, 11));

        assertTrue(TrapSystem.hasLineOfSight(this::isWall, new Vector3f(10, 1, 10), new Vector3f(10, 1, 13)));
        assertFalse(TrapSystem.hasLineOfSight(this::isWall, new Vector3f(10, 1, 10), new Vector3f(12, 1, 12)));
    }

    private void tick() {
        if (trapSystem == null) {
            trapSystem = new TrapSystem(entityData, kwdFile(), () -> gameTime, this::isWall, positionLookup(),
                    playerControllers(), effects(), shotsController(), 1);
        }
        trapSystem.processTick(0.25f);
    }

    private EntityId addTrap(short trapId, Point tile) {
        EntityId entityId = entityData.createEntity();
        entities.add(entityId);
        entityData.setComponent(entityId, new TrapComponent(trapId));
        entityData.setComponent(entityId, new TrapViewState(trapId, false));
        entityData.setComponent(entityId, new Owner(KEEPER, KEEPER));
        entityData.setComponent(entityId, new Health(1000, 1000));
        Vector3f position = WorldUtils.pointToVector3f(tile);
        position.y = WorldUtils.FLOOR_HEIGHT;
        entityData.setComponent(entityId, new Position(0, position));
        return entityId;
    }

    private EntityId addHiddenTrap(short trapId, Point tile) {
        EntityId entityId = addTrap(trapId, tile);
        entityData.setComponent(entityId, new TrapViewState(trapId, false, true));
        return entityId;
    }

    private EntityId addCreature(short creatureId, short ownerId, float x, float y) {
        EntityId entityId = entityData.createEntity();
        entities.add(entityId);
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

    private IEntityPositionLookup positionLookup() {
        return (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getEntitiesInLocation")) {
                        Point tile = args.length == 2 ? new Point((Integer) args[0], (Integer) args[1]) : (Point) args[0];
                        return entitiesOn(tile);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private List<EntityId> entitiesOn(Point tile) {
        List<EntityId> result = new ArrayList<>();
        for (EntityId entityId : entities) {
            Position position = entityData.getComponent(entityId, Position.class);
            if (position != null && tile.equals(WorldUtils.vectorToPoint(position.position))) {
                result.add(entityId);
            }
        }
        return result;
    }

    private IShotsController shotsController() {
        return (IShotsController) Proxy.newProxyInstance(IShotsController.class.getClassLoader(),
                new Class<?>[]{IShotsController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("fireShot")) {
                        if (shotsFly) {
                            flyingShots.add(args);
                        }
                        return shotsFly;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
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
