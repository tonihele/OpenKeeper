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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * What a summoned Horned Reaper does, as in DK II. Whenever he is free he
 * decides again: one time in three he roams about. Otherwise he goes for the
 * nearest enemy creature he can reach. With no such creature he goes for the
 * nearest enemy Dungeon Heart he can reach, and with no heart either he has
 * nothing left to do and leaves (DKII.EXE 0x4748E0, the search at 0x45E1B0).
 * He has no needs, so he never looks for a lair, food or work. Sent for the
 * Portal Gem, he walks to it and picks it up, and that is the end of his job.
 * On that walk he destroys the traps and unlocks his keeper's doors on his
 * tile and the four next to it (0x474F80).
 */
final class ReaperAi implements SummonedReaperSystem.ReaperDecisions {

    private static final int WANDER_CHANCE = 3;
    private static final int HEART_RADIUS = 2;

    private final EntityData entityData;
    private final Map<Short, Keeper> keepers;
    private final ICreaturesController creaturesController;
    private final INavigationService navigationService;
    private final IEntityPositionLookup entityPositionLookup;
    private final Random random;

    ReaperAi(EntityData entityData, Map<Short, Keeper> keepers, ICreaturesController creaturesController,
            INavigationService navigationService, IEntityPositionLookup entityPositionLookup, Random random) {
        this.entityData = entityData;
        this.keepers = keepers;
        this.creaturesController = creaturesController;
        this.navigationService = navigationService;
        this.entityPositionLookup = entityPositionLookup;
        this.random = random;
    }

    @Override
    public boolean think(EntityId reaper, short ownerId) {
        Keeper keeper = keepers.get(ownerId);
        ICreatureController reaperController = creaturesController.createController(reaper);
        if (keeper == null || isBusy(reaper, reaperController, keeper)) {
            return true;
        }

        StateMachine<ICreatureController, CreatureState> stateMachine = reaperController.getStateMachine();
        if (random.nextInt(WANDER_CHANCE) == 0) {
            stateMachine.changeState(CreatureState.WANDER);
            reaperController.navigateToRandomPoint();
            return true;
        }

        EntityId target = findNearestEnemy(reaper, reaperController, keeper);
        if (target != null) {
            entityData.setComponent(reaper, new AttackTarget(target));
            stateMachine.changeState(CreatureState.FIGHT);
            return true;
        }

        return goToNearestEnemyHeart(reaper, reaperController, keeper);
    }

    @Override
    public void fetchPortalGem(EntityId reaper, EntityId gem) {
        Position gemPosition = entityData.getComponent(gem, Position.class);
        if (gemPosition == null || entityData.getComponent(reaper, PortalGem.class) != null) {
            return;
        }

        Point from = WorldUtils.vectorToPoint(entityData.getComponent(reaper, Position.class).position);
        Point gemTile = WorldUtils.vectorToPoint(gemPosition.position);
        ICreatureController reaperController = creaturesController.createController(reaper);
        if (from.equals(gemTile)) {
            pickUpPortalGem(reaper, reaperController, gem);
            return;
        }

        clearTheWay(reaper, from);

        // Keep him on his way, whatever else he might think of
        Navigation navigation = entityData.getComponent(reaper, Navigation.class);
        if (navigation != null && gemTile.equals(navigation.target)) {
            return;
        }
        GraphPath<IMapTileInformation> path = navigationService.findPath(from, gemTile, reaperController);
        if (path != null) {
            reaperController.getStateMachine().changeState(CreatureState.WANDER);
            entityData.setComponent(reaper, new Navigation(gemTile, null, SteeringUtils.pathToList(path)));
        }
    }

    /**
     * On each tick of his walk to the gem the Reaper clears his tile and the
     * four next to it, like DK II's state for that walk (0x474F80): a built
     * trap is destroyed, whoever owns it, and a locked door of his own keeper
     * is unlocked. A blueprint is left alone.
     */
    private void clearTheWay(EntityId reaper, Point from) {
        short ownerId = entityData.getComponent(reaper, Owner.class).ownerId;
        for (Point tile : new Point[]{from, new Point(from.x - 1, from.y), new Point(from.x + 1, from.y),
            new Point(from.x, from.y - 1), new Point(from.x, from.y + 1)}) {
            for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
                Health health = entityData.getComponent(entityId, Health.class);
                if (entityData.getComponent(entityId, TrapComponent.class) != null && health != null) {
                    entityData.setComponent(entityId, new Damage(health.health));
                    continue;
                }
                DoorComponent door = entityData.getComponent(entityId, DoorComponent.class);
                if (door != null && door.locked && !door.blueprint && entityData.getComponent(entityId, Owner.class).ownerId == ownerId) {
                    entityData.setComponent(entityId, new DoorComponent(door.doorId, false, false));
                    DoorViewState viewState = entityData.getComponent(entityId, DoorViewState.class);
                    if (viewState != null) {
                        entityData.setComponent(entityId, new DoorViewState(viewState.doorId, false, viewState.blueprint, viewState.open));
                    }
                }
            }
        }
    }

    /**
     * The gem is gone from the floor, the Reaper holds it and stands still
     */
    private void pickUpPortalGem(EntityId reaper, ICreatureController reaperController, EntityId gem) {
        entityData.removeEntity(gem);
        entityData.setComponent(reaper, new PortalGem());
        reaperController.unassingCurrentTask();
        entityData.removeComponent(reaper, AttackTarget.class);
        entityData.removeComponent(reaper, CreatureAi.class);
        entityData.removeComponent(reaper, Navigation.class);
    }

    /**
     * He finishes what he is doing before he decides again
     */
    private boolean isBusy(EntityId reaper, ICreatureController reaperController, Keeper keeper) {
        CreatureState state = reaperController.getStateMachine().getCurrentState();
        if (state == null) {
            return true;
        }
        return switch (state) {
            case IDLE, WORK, FOLLOW, FLEE, SLEEPING, RECUPERATING, EATING ->
                false;
            case WANDER ->
                !reaperController.isStopped();
            case FIGHT -> {
                AttackTarget attackTarget = entityData.getComponent(reaper, AttackTarget.class);
                yield attackTarget != null && isEnemyToAttack(reaper, attackTarget.entityId, keeper);
            }
            default ->
                true;
        };
    }

    private EntityId findNearestEnemy(EntityId reaper, ICreatureController reaperController, Keeper keeper) {
        Vector3f reaperPosition = entityData.getComponent(reaper, Position.class).position;
        List<EntityId> enemies = new ArrayList<>();
        for (EntityId entityId : entityData.findEntities(null, CreatureComponent.class, Owner.class, Position.class, Health.class)) {
            if (isEnemyToAttack(reaper, entityId, keeper)) {
                enemies.add(entityId);
            }
        }
        enemies.sort(Comparator.comparingDouble(entityId -> entityData.getComponent(entityId, Position.class).position.distanceSquared(reaperPosition)));

        Point from = WorldUtils.vectorToPoint(reaperPosition);
        for (EntityId enemy : enemies) {
            Point to = WorldUtils.vectorToPoint(entityData.getComponent(enemy, Position.class).position);
            if (navigationService.isReachable(from, to, reaperController)) {
                return enemy;
            }
        }

        return null;
    }

    /**
     * Can the creature be attacked. Creatures that are out of the fight, or
     * held in a hand, or another Reaper on his way out, can't be.
     */
    private boolean isEnemyToAttack(EntityId reaper, EntityId entityId, Keeper keeper) {
        if (entityId.equals(reaper)) {
            return false;
        }
        Owner owner = entityData.getComponent(entityId, Owner.class);
        Health health = entityData.getComponent(entityId, Health.class);
        if (owner == null || health == null || health.health <= 0 || !keeper.isEnemy(owner.ownerId)
                || entityData.getComponent(entityId, Position.class) == null) {
            return false;
        }
        if (entityData.getComponent(entityId, Unconscious.class) != null
                || entityData.getComponent(entityId, InHand.class) != null
                || entityData.getComponent(entityId, CreatureImprisoned.class) != null
                || entityData.getComponent(entityId, CreatureTortured.class) != null) {
            return false;
        }
        SummonedReaper otherReaper = entityData.getComponent(entityId, SummonedReaper.class);
        return otherReaper == null || !otherReaper.isLeaving();
    }

    /**
     * Attacking the heart isn't there yet, so he just goes to it. DK II
     * measures the way to a heart as dx * dx + 2 * dy in tiles, compared
     * unsigned, so a heart at a smaller y can count as far away (0x45E1B0).
     */
    private boolean goToNearestEnemyHeart(EntityId reaper, ICreatureController reaperController, Keeper keeper) {
        Point from = WorldUtils.vectorToPoint(entityData.getComponent(reaper, Position.class).position);
        List<Point> hearts = new ArrayList<>();
        for (Keeper other : keepers.values()) {
            if (keeper.isEnemy(other.getId()) && !other.isDestroyed() && other.getDungeonHeartLocation() != null) {
                hearts.add(other.getDungeonHeartLocation());
            }
        }
        hearts.sort((a, b) -> Integer.compareUnsigned(heartDistance(from, a), heartDistance(from, b)));

        for (Point heart : hearts) {
            if (from.equals(heart)) {
                return true;
            }

            // The heart itself may be in the way, then go next to it
            Point destination = heart;
            GraphPath<IMapTileInformation> path = navigationService.findPath(from, destination, reaperController);
            if (path == null) {
                destination = navigationService.findRandomAccessibleTile(heart, HEART_RADIUS, reaperController);
                path = destination != null ? navigationService.findPath(from, destination, reaperController) : null;
            }
            if (path != null) {
                reaperController.getStateMachine().changeState(CreatureState.WANDER);
                entityData.setComponent(reaper, new Navigation(destination, null, SteeringUtils.pathToList(path)));
                return true;
            }
        }

        return false;
    }

    private static int heartDistance(Point from, Point heart) {
        int dx = heart.x - from.x;
        return dx * dx + 2 * (heart.y - from.y);
    }

}
