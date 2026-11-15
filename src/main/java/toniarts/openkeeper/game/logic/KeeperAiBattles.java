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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * How the computer player looks after its creatures in a fight. It uses the
 * hand and the spells like a human player does: it picks creatures up, drops
 * them and casts Call to Arms.
 */
final class KeeperAiBattles {

    /**
     * Writes a line to the computer player's decision log
     */
    interface DecisionLog {

        void log(KeeperAi keeperAi, String decision, Point target, String reason);
    }

    private static final short ROOM_LAIR = 2;
    private static final short SPELL_CALL_TO_ARMS = 3;

    /**
     * The enemy threat that calls for a Call to Arms is counted this many
     * tiles around the fighting creature
     */
    private static final int CALL_TO_ARMS_THREAT_TILES = 12;

    /**
     * DK II looks to cast Call to Arms on every 40th game tick, at 4 ticks
     * a second
     */
    private static final int CALL_TO_ARMS_EVERY_SECONDS = 10;

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

    /**
     * @param playerId the player
     * @return true if one of the player's creatures is in a fight
     */
    boolean isFighting(short playerId) {
        for (Entity creature : getCreatures(playerId)) {
            if (isFighting(creature)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Casts Call to Arms on the first fighting creature that has more enemy
     * threat around it than the computer player takes on without the flag.
     * Not while a flag is up, nor for a while after one was lifted, nor on
     * lava. It needs more mana than the spell costs.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param ai its settings
     * @param ticksPerSecond the game ticks in a second
     * @return true if the spell was cast
     */
    boolean callToArms(KeeperAi keeperAi, IPlayerController playerController, AI ai, int ticksPerSecond) {
        KeeperSpell callToArms = kwdFile.getKeeperSpellById(SPELL_CALL_TO_ARMS);
        if (keeperAi.callToArms || keeperAi.cannotAttackTicks > 0 || callToArms == null) {
            return false;
        }

        for (Entity creature : getCreatures(keeperAi.playerId)) {
            AttackTarget attackTarget = entityData.getComponent(creature.getId(), AttackTarget.class);
            Owner opponent = attackTarget != null ? entityData.getComponent(attackTarget.entityId, Owner.class) : null;
            if (!isFighting(creature) || opponent == null) {
                continue;
            }

            Vector3f position = creature.get(Position.class).position;
            Point tile = WorldUtils.vectorToPoint(position);
            int threat = getThreat(opponent.ownerId, position, CALL_TO_ARMS_THREAT_TILES);
            if (threat <= ai.getPlaceCallToArmsWhereThreatValueIsGreaterThan()
                    || mapController.getTerrain(mapController.getMapData().getTile(tile)).getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                continue;
            }

            int mana = playerController.getKeeper().getMana();
            if (mana > callToArms.getManaCost()
                    && Math.round(gameTimer.getGameTime() * ticksPerSecond) % (CALL_TO_ARMS_EVERY_SECONDS * ticksPerSecond) == 0
                    && playerController.getSpellControl().isAvailable(callToArms)) {
                gameWorldController.castKeeperSpell(SPELL_CALL_TO_ARMS, null, tile, new Vector2f(position.x, position.z), keeperAi.playerId);

                // The cast has its own rules, the mana tells if it went through
                if (playerController.getKeeper().getMana() < mana) {
                    decisionLog.log(keeperAi, "cast Call to Arms", tile, "creature " + creature.getId().getId() + " fights against a threat of "
                            + threat + ", more than " + ai.getPlaceCallToArmsWhereThreatValueIsGreaterThan());
                    keeperAi.callToArms = true;
                    keeperAi.callToArmsTile = tile;
                    keeperAi.callToArmsTicks = ai.getRemoveCallToArmsAfterSeconds();

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Lifts the Call to Arms when it has been up for its time, when too few
     * enemies are left around the flag, an enemy Dungeon Heart counts as
     * one, or when the computer player has too few fighters left. After
     * that it doesn't call again for a while.
     *
     * @param keeperAi the computer player
     * @param playerControllers all the players
     * @param ai its settings
     * @param ticksPerSecond the game ticks in a second
     */
    void liftCallToArms(KeeperAi keeperAi, Map<Short, IPlayerController> playerControllers, AI ai, int ticksPerSecond) {
        if (!keeperAi.callToArms) {
            return;
        }

        String reason = keeperAi.callToArmsTicks == 0 ? "it has been up for its time" : null;
        if (reason == null) {
            Keeper keeper = playerControllers.get(keeperAi.playerId).getKeeper();
            int range = ai.getRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles();
            Vector3f flag = WorldUtils.pointToVector3f(keeperAi.callToArmsTile);
            int enemies = 0;
            for (IPlayerController enemy : playerControllers.values()) {
                short enemyId = enemy.getKeeper().getId();
                if (!keeper.isEnemy(enemyId)) {
                    continue;
                }
                for (Entity creature : getCreatures(enemyId)) {
                    if (isWithin(creature.get(Position.class).position, flag, range)) {
                        enemies++;
                    }
                }
                IRoomController heart = enemy.getRoomControl().getDungeonHeart();
                if (heart != null && isWithin(WorldUtils.pointToVector3f(heart.getRoomInstance().getCenter()), flag, range)) {
                    enemies++;
                }
            }
            if (enemies < ai.getRemoveCallToArmsIfLessThanEnemyCreatures()) {
                reason = enemies + " enemies within " + range + " tiles of it";
            }
        }
        if (reason == null) {
            int fighters = 0;
            for (Entity creature : getCreatures(keeperAi.playerId)) {
                if (!creature.get(CreatureComponent.class).worker) {
                    fighters++;
                }
            }
            if (fighters < ai.getRemoveCallToArmsIfTotalCreaturesLessThan()) {
                reason = "only " + fighters + " fighters left";
            }
        }
        if (reason != null) {
            decisionLog.log(keeperAi, "lift Call to Arms", keeperAi.callToArmsTile, reason);
            gameWorldController.getObjectsController().cancelCallToArms(keeperAi.playerId);
            keeperAi.callToArms = false;
            keeperAi.callToArmsTicks = 0;
            keeperAi.cannotAttackTicks = ai.getWaitSecondsAfterPreviousAttackBeforeAttackingAgain() * ticksPerSecond;
        }
    }

    /**
     * The threat of the creatures of a player around a place
     */
    private int getThreat(short playerId, Vector3f position, int tiles) {
        int threat = 0;
        for (Entity creature : getCreatures(playerId)) {
            Threat creatureThreat = entityData.getComponent(creature.getId(), Threat.class);
            if (creatureThreat != null && isWithin(creature.get(Position.class).position, position, tiles)) {
                threat += creatureThreat.threat;
            }
        }

        return threat;
    }

    private static boolean isWithin(Vector3f position, Vector3f centre, int tiles) {
        float dx = (position.x - centre.x) / WorldUtils.TILE_WIDTH;
        float dy = (position.z - centre.z) / WorldUtils.TILE_WIDTH;

        return dx * dx + dy * dy < tiles * tiles;
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
