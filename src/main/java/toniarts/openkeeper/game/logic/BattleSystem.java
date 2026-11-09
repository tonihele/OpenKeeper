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
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.Battle;
import toniarts.openkeeper.game.component.BattleMember;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;

/**
 * Groups the fighting creatures into battles, like DK II does for its Battle
 * tab. A creature that starts fighting another creature joins the nearest
 * battle within five tiles, or starts a new one. It stays in the battle for at
 * least four seconds and leaves once it no longer fights, or at once when it
 * dies or is picked up. A battle without creatures ends.<br>
 * A battle is an entity with a {@link Battle}, and each creature in it has a
 * {@link BattleMember}. Only the creatures with an attack target and the
 * creatures in a battle are watched, and a creature joins when its attack
 * target or state changes.
 */
public final class BattleSystem implements IGameLogicUpdatable {

    private static final int MAX_BATTLES = 8;
    private static final float JOIN_DISTANCE_SQUARED = 5 * 5;
    private static final double MIN_TIME_IN_BATTLE = 4;

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final EntitySet battles;
    private final EntitySet members;
    private final EntitySet attackers;

    public BattleSystem(EntityData entityData, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;

        battles = entityData.getEntities(Battle.class);
        members = entityData.getEntities(BattleMember.class);
        attackers = entityData.getEntities(AttackTarget.class, CreatureAi.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        double gameTime = gameTimer.getGameTime();
        battles.applyChanges();
        members.applyChanges();
        attackers.applyChanges();

        // A creature that died or was taken off the map leaves its battle at once,
        // one that no longer fights leaves once its time is up
        Map<EntityId, List<EntityId>> battleMembers = new HashMap<>();
        for (Entity creature : members) {
            BattleMember member = creature.get(BattleMember.class);
            CreatureAi creatureAi = entityData.getComponent(creature.getId(), CreatureAi.class);
            if (creatureAi == null || entityData.getComponent(creature.getId(), Position.class) == null
                    || (!isFighting(creature.getId(), creatureAi) && gameTime >= member.leaveTime)) {
                entityData.removeComponent(creature.getId(), BattleMember.class);
            } else {
                battleMembers.computeIfAbsent(member.battleId, battle -> new ArrayList<>()).add(creature.getId());
            }
        }

        for (Entity battle : battles) {
            List<EntityId> creatures = battleMembers.get(battle.getId());
            if (creatures == null) {
                entityData.removeEntity(battle.getId());
            } else {
                updateBattle(battle.getId(), creatures);
            }
        }

        joinBattles(attackers.getAddedEntities(), battleMembers, gameTime);
        joinBattles(attackers.getChangedEntities(), battleMembers, gameTime);
    }

    private boolean isFighting(EntityId creature, CreatureAi creatureAi) {

        // Fights in the Combat Pit are no battle
        if (creatureAi.getCreatureState() != CreatureState.FIGHT
                || entityData.getComponent(creature, CombatPitFighter.class) != null) {
            return false;
        }
        AttackTarget attackTarget = entityData.getComponent(creature, AttackTarget.class);
        return attackTarget != null && entityData.getComponent(attackTarget.entityId, CreatureComponent.class) != null;
    }

    private void joinBattles(Set<Entity> creatures, Map<EntityId, List<EntityId>> battleMembers, double gameTime) {
        for (Entity creature : creatures) {
            if (isFighting(creature.getId(), creature.get(CreatureAi.class))
                    && entityData.getComponent(creature.getId(), BattleMember.class) == null) {
                joinBattle(creature, battleMembers, gameTime);
            }
        }
    }

    private void joinBattle(Entity creature, Map<EntityId, List<EntityId>> battleMembers, double gameTime) {
        Vector3f position = creature.get(Position.class).position;
        EntityId nearestBattle = null;
        float nearestDistance = Float.MAX_VALUE;
        for (EntityId battle : battleMembers.keySet()) {
            Vector3f centre = entityData.getComponent(battle, Battle.class).position;
            float distance = (centre.x - position.x) * (centre.x - position.x) + (centre.z - position.z) * (centre.z - position.z);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestBattle = battle;
            }
        }

        // When all the battles are taken, the nearest one takes the creature whatever the distance
        if (nearestBattle == null || (nearestDistance > JOIN_DISTANCE_SQUARED && battleMembers.size() < MAX_BATTLES)) {
            nearestBattle = entityData.createEntity();
        }
        List<EntityId> creatures = battleMembers.computeIfAbsent(nearestBattle, battle -> new ArrayList<>());
        creatures.add(creature.getId());
        entityData.setComponent(creature.getId(), new BattleMember(nearestBattle, gameTime + MIN_TIME_IN_BATTLE));
        updateBattle(nearestBattle, creatures);
    }

    /**
     * Moves the battle to the centre of its creatures and lists their owners
     */
    private void updateBattle(EntityId battle, List<EntityId> creatures) {
        Vector3f centre = new Vector3f();
        Set<Short> playerIds = new HashSet<>();
        for (EntityId creature : creatures) {
            centre.addLocal(entityData.getComponent(creature, Position.class).position);
            playerIds.add(entityData.getComponent(creature, Owner.class).ownerId);
        }
        entityData.setComponent(battle, new Battle(centre.divideLocal(creatures.size()), playerIds));
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        battles.release();
        members.release();
        attackers.release();
    }

}
