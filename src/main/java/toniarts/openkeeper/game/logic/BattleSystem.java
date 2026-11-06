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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.Battle;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;

/**
 * Groups the fighting creatures into battles, like DK II does for its Battle
 * tab. A creature that fights another creature joins the nearest battle within
 * five tiles, or starts a new one. It stays in the battle for at least four
 * seconds and leaves once it no longer fights, or at once when it dies or is
 * picked up. A battle without creatures ends.
 */
public final class BattleSystem implements IGameLogicUpdatable {

    private static final int MAX_BATTLES = 8;
    private static final float JOIN_DISTANCE_SQUARED = 5 * 5;
    private static final double MIN_TIME_IN_BATTLE = 4;

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final EntitySet creatures;

    /**
     * The battle entities, with the time each member can leave the battle
     */
    private final Map<EntityId, Map<EntityId, Double>> battles = new LinkedHashMap<>();

    public BattleSystem(EntityData entityData, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;

        creatures = entityData.getEntities(CreatureAi.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        creatures.applyChanges();
        double gameTime = gameTimer.getGameTime();

        Iterator<Map.Entry<EntityId, Map<EntityId, Double>>> iterator = battles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityId, Map<EntityId, Double>> battle = iterator.next();
            removeLeavers(battle.getValue(), gameTime);
            if (battle.getValue().isEmpty()) {
                entityData.removeEntity(battle.getKey());
                iterator.remove();
            } else {
                updateBattle(battle.getKey(), battle.getValue());
            }
        }

        for (Entity creature : creatures) {
            if (isFighting(creature) && !isInBattle(creature.getId())) {
                joinBattle(creature, gameTime);
            }
        }
    }

    private void removeLeavers(Map<EntityId, Double> members, double gameTime) {
        members.entrySet().removeIf(member -> {
            Entity creature = creatures.getEntity(member.getKey());
            return creature == null || (!isFighting(creature) && gameTime >= member.getValue());
        });
    }

    private boolean isFighting(Entity creature) {
        if (creature.get(CreatureAi.class).getCreatureState() != CreatureState.FIGHT) {
            return false;
        }
        AttackTarget attackTarget = entityData.getComponent(creature.getId(), AttackTarget.class);
        return attackTarget != null && entityData.getComponent(attackTarget.entityId, CreatureComponent.class) != null;
    }

    private boolean isInBattle(EntityId creature) {
        for (Map<EntityId, Double> members : battles.values()) {
            if (members.containsKey(creature)) {
                return true;
            }
        }
        return false;
    }

    private void joinBattle(Entity creature, double gameTime) {
        Vector3f position = creature.get(Position.class).position;
        EntityId nearestBattle = null;
        float nearestDistance = Float.MAX_VALUE;
        for (EntityId battle : battles.keySet()) {
            Vector3f centre = entityData.getComponent(battle, Battle.class).position;
            float distance = (centre.x - position.x) * (centre.x - position.x) + (centre.z - position.z) * (centre.z - position.z);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestBattle = battle;
            }
        }

        // When all the battles are taken, the nearest one takes the creature whatever the distance
        if (nearestBattle == null || (nearestDistance > JOIN_DISTANCE_SQUARED && battles.size() < MAX_BATTLES)) {
            nearestBattle = entityData.createEntity();
            battles.put(nearestBattle, new HashMap<>());
        }
        Map<EntityId, Double> members = battles.get(nearestBattle);
        members.put(creature.getId(), gameTime + MIN_TIME_IN_BATTLE);
        updateBattle(nearestBattle, members);
    }

    /**
     * Moves the battle to the centre of its creatures and lists their owners
     */
    private void updateBattle(EntityId battle, Map<EntityId, Double> members) {
        Vector3f centre = new Vector3f();
        Set<Short> playerIds = new HashSet<>();
        for (EntityId member : members.keySet()) {
            Entity creature = creatures.getEntity(member);
            centre.addLocal(creature.get(Position.class).position);
            playerIds.add(creature.get(Owner.class).ownerId);
        }
        entityData.setComponent(battle, new Battle(centre.divideLocal(members.size()), playerIds));
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        creatures.release();
    }

}
