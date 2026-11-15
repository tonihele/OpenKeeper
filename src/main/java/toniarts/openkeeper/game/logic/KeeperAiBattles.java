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
import toniarts.openkeeper.game.component.KeeperAiNoPrisoner;
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
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
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
    private static final short ROOM_PRISON = 11;
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
    static final Set<CreatureState> FIGHTING = Set.of(CreatureState.FIGHT, CreatureState.MELEE_ATTACK, CreatureState.CAST_SPELL);

    /**
     * The states the hand leaves a creature alone in
     */
    static final Set<CreatureState> HELPLESS = Set.of(CreatureState.DEAD, CreatureState.ENTERING_DUNGEON, CreatureState.PICKED_UP,
            CreatureState.UNCONSCIOUS, CreatureState.STUNNED, CreatureState.IMPRISONED, CreatureState.TORTURED, CreatureState.DRAGGED,
            CreatureState.FALLEN, CreatureState.GETTING_UP, CreatureState.COMBAT_PIT);

    /**
     * DK II counts the enemy threat 10 tiles around an attack, its own 4
     * tiles around it, and drops on or carries from 5 tiles around it
     */
    private static final int ATTACK_ENEMY_THREAT_TILES = 10;
    private static final int ATTACK_OWN_THREAT_TILES = 4;
    private static final int ATTACK_DROP_TILES = 5;

    /**
     * DK II's rolls for a spell in a fight: a 4 sided die against the
     * settings, a step in the die of the spell for each 2499 mana it
     * costs, and 75 in 100 to look on for another target
     */
    private static final int BATTLE_SPELL_ROLL = 4;
    private static final int BATTLE_SPELL_MANA_STEP = 2499;
    private static final int BATTLE_SPELL_TARGET_ROLL = 75;

    /**
     * DK II weighs an attack up when the message of it comes in. Without
     * the messages the computer player looks for itself, this often
     */
    private static final int LOOK_FOR_ATTACK_EVERY_SECONDS = 10;

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
     * The creatures of a player that are on the map and in its power,
     * always in the same order
     */
    List<Entity> getCreatures(short playerId) {
        List<Entity> owned = new ArrayList<>();
        for (Entity entity : creatures) {
            Owner owner = entity.get(Owner.class);
            if (owner.ownerId == playerId && owner.controlId == playerId && entityData.getComponent(entity.getId(), InHand.class) == null) {
                owned.add(entity);
            }
        }
        owned.sort(Comparator.comparingLong(entity -> entity.getId().getId()));

        return owned;
    }

    /**
     * The creatures of other players that a player holds in its rooms, its
     * prisoners, always in the same order
     */
    List<Entity> getPrisoners(short playerId) {
        List<Entity> prisoners = new ArrayList<>();
        for (Entity entity : creatures) {
            Owner owner = entity.get(Owner.class);
            if (owner.ownerId != playerId && owner.controlId == playerId && entityData.getComponent(entity.getId(), InHand.class) == null) {
                prisoners.add(entity);
            }
        }
        prisoners.sort(Comparator.comparingLong(entity -> entity.getId().getId()));

        return prisoners;
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
     * Casts a spell for the first of its creatures in a fight that it
     * means to help, DK II rolls for each. A defensive spell goes on one of
     * its creatures within 5 tiles of the fighter that is down to less
     * than half its health, an attacking spell on a creature of the
     * opponent's keeper within 5 tiles. The settings say how often it
     * casts, how often it defends rather than attacks, and how dear a
     * spell it takes.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param ai its settings
     * @return true if a spell was cast
     */
    boolean castBattleSpell(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        for (Entity fighter : getCreatures(keeperAi.playerId)) {
            if (!isFighting(fighter) || ai.getUseSpellsInBattle() > Utils.getRandom().nextInt(BATTLE_SPELL_ROLL)) {
                continue;
            }

            boolean defensive = Utils.getRandom().nextInt(BATTLE_SPELL_ROLL) < ai.getSpellStyle();
            KeeperSpell spell = getBattleSpell(playerController, ai, defensive);
            Owner opponent = entityData.getComponent(entityData.getComponent(fighter.getId(), AttackTarget.class).entityId, Owner.class);
            if (spell == null || (!defensive && opponent == null)) {
                return false;
            }

            // Any of them can be the one, the later ones more likely
            Entity target = null;
            for (Entity creature : getCreatures(defensive ? keeperAi.playerId : opponent.ownerId)) {
                Health health = creature.get(Health.class);
                if (isWithin(creature.get(Position.class).position, fighter.get(Position.class).position, ATTACK_DROP_TILES)
                        && (!defensive || health.health < health.maxHealth / 2)) {
                    target = creature;
                    if (Utils.getRandom().nextInt(100) > BATTLE_SPELL_TARGET_ROLL) {
                        break;
                    }
                }
            }
            if (target == null) {
                return false;
            }

            Vector3f position = target.get(Position.class).position;
            Point tile = WorldUtils.vectorToPoint(position);
            int mana = playerController.getKeeper().getMana();
            gameWorldController.castKeeperSpell(spell.getKeeperSpellId(), target.getId(), tile, new Vector2f(position.x, position.z), keeperAi.playerId);

            // The cast has its own rules, the mana tells if it went through
            if (playerController.getKeeper().getMana() < mana) {
                decisionLog.log(keeperAi, "cast " + spell.getName() + " on creature " + target.getId().getId(), tile,
                        (defensive ? "hurt" : "an enemy") + " near the fight of creature " + fighter.getId().getId());
                return true;
            }

            return false;
        }

        return false;
    }

    /**
     * The spell for a fight. Of the attacking spells the first one, by
     * id, that makes its roll: the dearer the spell, the better its odds.
     * Of the defensive spells also the last one when none makes its roll.
     */
    private KeeperSpell getBattleSpell(IPlayerController playerController, AI ai, boolean defensive) {
        List<KeeperSpell> spells = new ArrayList<>(kwdFile.getKeeperSpells());
        spells.sort(Comparator.comparingInt(KeeperSpell::getKeeperSpellId));
        KeeperSpell last = null;
        for (KeeperSpell spell : spells) {
            if (spell.getFlags().contains(defensive ? KeeperSpell.KeeperSpellFlag.DEFENSIVE : KeeperSpell.KeeperSpellFlag.ATTACKING)
                    && !spell.getFlags().contains(KeeperSpell.KeeperSpellFlag.DONT_CAST_ON_CREATURES)
                    && playerController.getSpellControl().isAvailable(spell)) {
                last = spell;
                if (BATTLE_SPELL_ROLL - ai.getSpellsPowerPreference()
                        <= Utils.getRandom().nextInt(spell.getManaCost() / BATTLE_SPELL_MANA_STEP + (defensive ? 1 : 2))) {
                    return spell;
                }
            }
        }

        return defensive ? last : null;
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
    int getThreat(short playerId, Vector3f position, int tiles) {
        int threat = 0;
        for (Entity creature : getCreatures(playerId)) {
            Threat creatureThreat = entityData.getComponent(creature.getId(), Threat.class);
            if (creatureThreat != null && isWithin(creature.get(Position.class).position, position, tiles)) {
                threat += creatureThreat.threat;
            }
        }

        return threat;
    }

    static boolean isWithin(Vector3f position, Vector3f centre, int tiles) {
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
            } else if (health.health < retreatHealth * health.maxHealth / 100 && !HELPLESS.contains(creature.get(CreatureAi.class).getCreatureState())) {
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

    /**
     * Looks for an attack on the computer player, every few seconds: one of
     * its creatures in a fight, or an enemy creature that goes for something
     * of its own. It weighs the attack up and queues it to be answered
     *
     * @param keeperAi the computer player
     * @param playerControllers all the players
     * @param ai its settings
     * @param ticksPerSecond the game ticks in a second
     */
    void lookForAttack(KeeperAi keeperAi, Map<Short, IPlayerController> playerControllers, AI ai, int ticksPerSecond) {
        if (keeperAi.attackLookTicks > 0 || keeperAi.attacks >= keeperAi.attackNeeds.length) {
            return;
        }

        Keeper keeper = playerControllers.get(keeperAi.playerId).getKeeper();
        Point place = null;
        for (Entity creature : getCreatures(keeperAi.playerId)) {
            if (isFighting(creature)) {
                place = WorldUtils.vectorToPoint(creature.get(Position.class).position);
                break;
            }
        }
        for (IPlayerController enemy : playerControllers.values()) {
            if (place != null || !keeper.isEnemy(enemy.getKeeper().getId())) {
                continue;
            }
            for (Entity creature : getCreatures(enemy.getKeeper().getId())) {
                AttackTarget attackTarget = entityData.getComponent(creature.getId(), AttackTarget.class);
                Owner owner = attackTarget != null ? entityData.getComponent(attackTarget.entityId, Owner.class) : null;
                if (owner != null && owner.ownerId == keeperAi.playerId) {
                    place = WorldUtils.vectorToPoint(creature.get(Position.class).position);
                    break;
                }
            }
        }
        if (place != null) {
            keeperAi.attackLookTicks = LOOK_FOR_ATTACK_EVERY_SECONDS * ticksPerSecond;
            weighUpAttack(keeperAi, playerControllers, ai, place);
        }
    }

    /**
     * Weighs an attack up, like DK II does for an attack message. With a
     * Prison it first decides anew for each enemy creature whether to take
     * it prisoner. Then it sets the enemy threat within 10 tiles against
     * the threat it has within 4 tiles. Where it has land within 5 tiles it
     * queues the threat it still needs there. Where it has none, and it is
     * outnumbered, it queues its own threat to be carried away, if its
     * settings say so.
     *
     * @param keeperAi the computer player
     * @param playerControllers all the players
     * @param ai its settings
     * @param place the tile of the attack
     */
    void weighUpAttack(KeeperAi keeperAi, Map<Short, IPlayerController> playerControllers, AI ai, Point place) {
        IPlayerController playerController = playerControllers.get(keeperAi.playerId);
        Keeper keeper = playerController.getKeeper();
        boolean prison = playerController.getRoomControl().getTypeCount(kwdFile.getRoomById(ROOM_PRISON)) > 0;
        Vector3f position = WorldUtils.pointToVector3f(place);
        int enemyThreat = 0;
        int ownThreat = 0;
        for (IPlayerController other : playerControllers.values()) {
            short otherId = other.getKeeper().getId();
            if (keeper.isEnemy(otherId)) {
                if (prison) {
                    for (Entity creature : getCreatures(otherId)) {
                        KeeperAiNoPrisoner noPrisoner = entityData.getComponent(creature.getId(), KeeperAiNoPrisoner.class);
                        int players = noPrisoner != null ? noPrisoner.players : 0;
                        boolean imprison = Utils.getRandom().nextInt(100) < ai.getAttemptToImprisonPercentageOfEnemyCreatures();
                        entityData.setComponent(creature.getId(), new KeeperAiNoPrisoner(
                                imprison ? players & ~(1 << keeperAi.playerId) : players | 1 << keeperAi.playerId));
                    }
                }
                int threat = getThreat(otherId, position, ATTACK_ENEMY_THREAT_TILES);
                enemyThreat += threat;
                if (threat > 0) {
                    keeperAi.attackerId = otherId;
                }
            }
            if (keeper.isAlly(otherId)) {
                ownThreat += getThreat(otherId, position, ATTACK_OWN_THREAT_TILES);
            }
        }

        int need = ai.getThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue() * enemyThreat / 100 - ownThreat;
        Point dropTile = getNearestOwnOpenTile(keeperAi.playerId, place, ATTACK_DROP_TILES);
        String reason = "enemy threat " + enemyThreat + ", own threat " + ownThreat;
        int attack = (keeperAi.firstAttack + keeperAi.attacks) % keeperAi.attackNeeds.length;
        if (dropTile != null && need >= 0) {
            decisionLog.log(keeperAi, "answer an attack with a threat of " + need, dropTile, reason);
            keeperAi.attackNeeds[attack] = need;
            keeperAi.attackTiles[attack] = dropTile;
            keeperAi.attacks++;
        } else if (dropTile == null && need > 0 && ai.isPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements()) {
            decisionLog.log(keeperAi, "pull a threat of " + ownThreat + " out of a fight", place, reason + ", none of its land near");
            keeperAi.attackNeeds[attack] = -ownThreat;
            keeperAi.attackTiles[attack] = place;
            keeperAi.attacks++;
        }
    }

    /**
     * Answers the first attack in the queue, with one creature a turn. It
     * drops fighters that are not busy and have more than half their health
     * on the attack until their threat makes up what it needs there. Or it
     * carries its creatures within 5 tiles of the attack to the Dungeon
     * Heart, until it has carried away what it had there.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @return true if there was an attack to answer, that takes the turn
     */
    boolean respondToAttack(KeeperAi keeperAi, IPlayerController playerController) {
        if (keeperAi.attacks == 0) {
            return false;
        }

        int attack = keeperAi.firstAttack;
        boolean retreat = keeperAi.attackNeeds[attack] < 0;
        Vector3f position = WorldUtils.pointToVector3f(keeperAi.attackTiles[attack]);
        Entity picked = null;
        for (Entity creature : getCreatures(keeperAi.playerId)) {
            CreatureState state = creature.get(CreatureAi.class).getCreatureState();
            Health health = creature.get(Health.class);
            if (!kwdFile.getCreature(creature.get(CreatureComponent.class).creatureId).getFlags().contains(Creature.CreatureFlag.IS_HORNY)
                    && !HELPLESS.contains(state) && !isLeftAlone(creature)
                    && (retreat ? isWithin(creature.get(Position.class).position, position, ATTACK_DROP_TILES)
                            : !creature.get(CreatureComponent.class).worker && health.health * 2 > health.maxHealth
                            && !FIGHTING.contains(state) && state != CreatureState.FLEE)) {
                picked = creature;
                break;
            }
        }
        if (picked == null) {
            decisionLog.log(keeperAi, "attack answered", keeperAi.attackTiles[attack], "no creature left to " + (retreat ? "carry away" : "drop"));
            endAttack(keeperAi);
            return true;
        }

        Threat creatureThreat = entityData.getComponent(picked.getId(), Threat.class);
        int threat = creatureThreat != null ? creatureThreat.threat : 0;
        if (retreat) {
            Point heart = playerController.getKeeper().getDungeonHeartLocation();
            decisionLog.log(keeperAi, "carry creature " + picked.getId().getId() + " to the Dungeon Heart", heart, "outnumbered in a fight away from its land");
            carry(picked.getId(), heart, playerController);
            keeperAi.attackNeeds[attack] += threat;
            if (keeperAi.attackNeeds[attack] >= 0) {
                endAttack(keeperAi);
            }
        } else {

            // On its own land, the nearest there is when the land was lost
            if (mapController.getMapData().getTile(keeperAi.attackTiles[attack]).getOwnerId() != keeperAi.playerId) {
                keeperAi.attackTiles[attack] = getNearestOwnOpenTile(keeperAi.playerId, keeperAi.attackTiles[attack],
                        Math.max(mapController.getMapData().getWidth(), mapController.getMapData().getHeight()));
            }
            if (keeperAi.attackTiles[attack] != null) {
                decisionLog.log(keeperAi, "drop creature " + picked.getId().getId() + " on an attack", keeperAi.attackTiles[attack],
                        "its threat of " + threat + " of the " + keeperAi.attackNeeds[attack] + " still needed");
                carry(picked.getId(), keeperAi.attackTiles[attack], playerController);
            }
            keeperAi.attackNeeds[attack] -= threat;
            if (keeperAi.attackNeeds[attack] < 1 || keeperAi.attackTiles[attack] == null) {
                endAttack(keeperAi);
            }
        }

        return true;
    }

    private static void endAttack(KeeperAi keeperAi) {
        keeperAi.attackNeeds[keeperAi.firstAttack] = 0;
        keeperAi.attackTiles[keeperAi.firstAttack] = null;
        keeperAi.firstAttack = (keeperAi.firstAttack + 1) % keeperAi.attackNeeds.length;
        keeperAi.attacks--;
    }

    /**
     * The player's tile nearest to a place that is not solid, no further
     * than some tiles along either axis
     */
    Point getNearestOwnOpenTile(short playerId, Point place, int tiles) {
        Point nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (int y = place.y - tiles; y <= place.y + tiles; y++) {
            for (int x = place.x - tiles; x <= place.x + tiles; x++) {
                Point p = new Point(x, y);
                IMapTileInformation tile = mapController.getMapData().getTile(p);
                int distance = (x - place.x) * (x - place.x) + (y - place.y) * (y - place.y);
                if (tile != null && distance < nearestDistance && tile.getOwnerId() == playerId && !mapController.isSolid(p)) {
                    nearest = p;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    boolean isLeftAlone(Entity creature) {
        KeeperAiDropped dropped = entityData.getComponent(creature.getId(), KeeperAiDropped.class);

        return dropped != null && gameTimer.getGameTime() - dropped.time < LEAVE_ALONE_AFTER_DROP_SECONDS;
    }

    /**
     * Picks a creature up and drops it on a tile, with the hand. The
     * creature is left alone for a while after that, also when the hand
     * could not take it
     *
     * @return true if the creature was dropped
     */
    boolean carry(EntityId creature, Point tile, IPlayerController playerController) {
        short playerId = playerController.getKeeper().getId();
        entityData.setComponent(creature, new KeeperAiDropped(gameTimer.getGameTime()));
        gameWorldController.pickUp(creature, playerId);
        if (!creature.equals(playerController.getHandControl().peek())) {
            return false;
        }
        gameWorldController.drop(creature, tile, WorldUtils.pointToVector2f(tile), null, playerId);

        return !creature.equals(playerController.getHandControl().peek());
    }

}
