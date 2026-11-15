/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.KeeperAiDropped;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * How the computer player looks after its creatures in a fight. It uses the
 * hand like a human player does: it picks creatures up and drops them.
 */
final class KeeperAiBattles {

    /**
     * Writes a line to the computer player's decision log
     */
    interface DecisionLog {

        void log(KeeperAi keeperAi, String decision, Point target, String reason);
    }

    private static final short ROOM_LAIR = 2;

    /**
     * DK II leaves a creature alone for 300 game ticks after a drop, at 4
     * ticks a second
     */
    private static final int LEAVE_ALONE_AFTER_DROP_SECONDS = 75;

    /**
     * A Combat Pit fighter is taken out of the pit below this much health
     */
    private static final int PIT_FIGHTER_RETREAT_HEALTH_PERCENTAGE = 50;
    private static final Set<CreatureState> FIGHTING = Set.of(CreatureState.FIGHT, CreatureState.MELEE_ATTACK, CreatureState.CAST_SPELL);

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameWorldController gameWorldController;
    private final IMapController mapController;
    private final IGameTimer gameTimer;
    private final DecisionLog decisionLog;
    private final EntitySet creatures;

    KeeperAiBattles(EntityData entityData, IKwdFile kwdFile, IGameWorldController gameWorldController, IGameTimer gameTimer,
            DecisionLog decisionLog) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameWorldController = gameWorldController;
        this.mapController = gameWorldController.getMapController();
        this.gameTimer = gameTimer;
        this.decisionLog = decisionLog;
        creatures = entityData.getEntities(CreatureComponent.class, Owner.class, Health.class, CreatureAi.class, Position.class);
    }

    /**
     * Takes in what happened to the creatures since the last game tick
     */
    void applyChanges() {
        creatures.applyChanges();
    }

    void release() {
        creatures.release();
    }

    /**
     * The creatures of a player that are on the map, always in the same
     * order
     */
    private List<Entity> getCreatures(short playerId) {
        List<Entity> owned = new ArrayList<>();
        for (Entity entity : creatures) {
            if (entity.get(Owner.class).ownerId == playerId && entityData.getComponent(entity.getId(), InHand.class) == null) {
                owned.add(entity);
            }
        }
        owned.sort(Comparator.comparingLong(entity -> entity.getId().getId()));

        return owned;
    }

    private boolean isFighting(Entity creature) {
        return FIGHTING.contains(creature.get(CreatureAi.class).getCreatureState())
                && entityData.getComponent(creature.getId(), AttackTarget.class) != null;
    }

    /**
     * Carries the first creature that is too hurt to the Lair, or to the
     * Dungeon Heart when there is no Lair. A creature in a fight is taken
     * at once. One that is not fighting is only taken when it is awake,
     * outside the rooms and not just dropped. Combat Pit fighters are taken
     * out when they are down to half their health, or at the level the
     * computer player trains its creatures to.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param ai its settings
     * @param creatureLevel the level it trains its creatures to
     * @return true if it spent its action on a creature
     */
    boolean retreatInjured(KeeperAi keeperAi, IPlayerController playerController, AI ai, int creatureLevel) {
        int retreatHealth = ai.getIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple();
        for (Entity creature : getCreatures(keeperAi.playerId)) {
            Health health = creature.get(Health.class);
            String reason = null;
            if (entityData.getComponent(creature.getId(), CombatPitFighter.class) != null) {
                CreatureExperience experience = entityData.getComponent(creature.getId(), CreatureExperience.class);
                if (health.health < health.maxHealth * PIT_FIGHTER_RETREAT_HEALTH_PERCENTAGE / 100) {
                    reason = "a Combat Pit fighter below " + PIT_FIGHTER_RETREAT_HEALTH_PERCENTAGE + " % health";
                } else if (experience != null && experience.level >= creatureLevel) {
                    reason = "a Combat Pit fighter at level " + experience.level;
                }
            } else if (health.health < retreatHealth * health.maxHealth / 100) {
                if (isFighting(creature)) {
                    reason = "fighting below " + retreatHealth + " % health";
                } else if (!isLeftAlone(creature) && creature.get(CreatureAi.class).getCreatureState() != CreatureState.SLEEPING
                        && mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(creature.get(Position.class).position)) == null) {
                    reason = "below " + retreatHealth + " % health outside the rooms";
                }
            }
            if (reason == null) {
                continue;
            }

            IRoomController lair = null;
            for (IRoomController room : playerController.getRoomControl().getRoomControllers().getOrDefault(kwdFile.getRoomById(ROOM_LAIR), Set.of())) {
                lair = room;
                break;
            }
            Point tile = lair != null ? lair.getRoomInstance().getCenter() : playerController.getKeeper().getDungeonHeartLocation();
            decisionLog.log(keeperAi, "carry creature " + creature.getId().getId() + " to the " + (lair != null ? "Lair" : "Dungeon Heart"), tile, reason);
            carry(creature.getId(), tile, playerController);

            return true;
        }

        return false;
    }

    private boolean isLeftAlone(Entity creature) {
        KeeperAiDropped dropped = entityData.getComponent(creature.getId(), KeeperAiDropped.class);

        return dropped != null && gameTimer.getGameTime() - dropped.time < LEAVE_ALONE_AFTER_DROP_SECONDS;
    }

    /**
     * Picks a creature up and drops it on a tile, with the hand
     *
     * @return true if the creature was dropped
     */
    private boolean carry(EntityId creature, Point tile, IPlayerController playerController) {
        short playerId = playerController.getKeeper().getId();
        gameWorldController.pickUp(creature, playerId);
        if (!creature.equals(playerController.getHandControl().peek())) {
            return false;
        }
        gameWorldController.drop(creature, tile, WorldUtils.pointToVector2f(tile), null, playerId);
        entityData.setComponent(creature, new KeeperAiDropped(gameTimer.getGameTime()));

        return !creature.equals(playerController.getHandControl().peek());
    }

}
