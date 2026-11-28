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
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.DungeonHeartTarget;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * What a summoned Horned Reaper does, as in DK II. Whenever he is free he
 * decides again: one time in three he roams about. Otherwise he goes for the
 * nearest enemy creature he can reach. With no such creature he attacks the
 * nearest enemy Dungeon Heart he can get to, and with no heart either he has
 * nothing left to do and leaves (DKII.EXE 0x4748E0, the search at 0x45E1B0).
 * He has no needs, so he never looks for a lair, food or work.
 */
final class ReaperAi implements SummonedReaperSystem.ReaperDecisions {

    private static final int WANDER_CHANCE = 3;

    private final EntityData entityData;
    private final Map<Short, Keeper> keepers;
    private final ICreaturesController creaturesController;
    private final INavigationService navigationService;
    private final Random random;

    ReaperAi(EntityData entityData, Map<Short, Keeper> keepers, ICreaturesController creaturesController,
            INavigationService navigationService, Random random) {
        this.entityData = entityData;
        this.keepers = keepers;
        this.creaturesController = creaturesController;
        this.navigationService = navigationService;
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

        return attackNearestEnemyHeart(reaper, reaperController, keeper);
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
     * DK II takes the heart object of a standing enemy Dungeon Heart as his
     * target, the same way as a creature, and the fight goes on from there. It
     * measures the way to a heart as dx * dx + 2 * dy in tiles, compared
     * unsigned, so a heart at a smaller y can count as far away (0x45E1B0).
     */
    private boolean attackNearestEnemyHeart(EntityId reaper, ICreatureController reaperController, Keeper keeper) {
        Point from = WorldUtils.vectorToPoint(entityData.getComponent(reaper, Position.class).position);
        List<EntityId> hearts = new ArrayList<>();
        for (EntityId entityId : entityData.findEntities(null, ObjectComponent.class, Owner.class, Position.class, Health.class)) {
            if (DungeonHeartTarget.isDungeonHeart(entityData, entityId) && entityData.getComponent(entityId, Health.class).health > 0
                    && keeper.isEnemy(entityData.getComponent(entityId, Owner.class).ownerId)) {
                hearts.add(entityId);
            }
        }
        hearts.sort((a, b) -> Integer.compareUnsigned(heartDistance(from, heartTile(a)), heartDistance(from, heartTile(b))));

        float meleeRange = entityData.getComponent(reaper, CreatureMeleeAttack.class).range;
        for (EntityId heart : hearts) {
            Point attackTile = DungeonHeartTarget.getAttackTile(from, heartTile(heart), meleeRange,
                    tile -> tile.equals(from) || navigationService.findPath(from, tile, reaperController) != null);
            if (attackTile != null) {
                entityData.setComponent(reaper, new AttackTarget(heart));
                reaperController.getStateMachine().changeState(CreatureState.FIGHT);
                return true;
            }
        }

        return false;
    }

    private Point heartTile(EntityId heart) {
        return WorldUtils.vectorToPoint(entityData.getComponent(heart, Position.class).position);
    }

    private static int heartDistance(Point from, Point heart) {
        int dx = heart.x - from.x;
        return dx * dx + 2 * (heart.y - from.y);
    }

}
