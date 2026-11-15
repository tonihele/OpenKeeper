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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.KeeperAiEnemy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * How the computer player invades an enemy, DK II's 0x4FAEF0, 0x4FECE0 and
 * 0x4FE9C0. When its army is ready it picks an enemy and digs tunnels up to
 * the enemy's land, as many as its settings say. Then it breaks through
 * them, one a turn. Once a creature can walk to the enemy's Dungeon Heart it
 * calls its creatures to arms there, or step by step on the way there.
 */
final class KeeperAiInvasions {

    private static final short SPELL_CALL_TO_ARMS = 3;
    private static final short ROOM_DUNGEON_HEART = 5;

    /**
     * DK II never digs from or to a bridge
     */
    private static final Set<Short> BRIDGES = Set.of((short) 8, (short) 17, (short) 22);

    /**
     * DK II weighs up how well a room is defended by the threat within 5
     * tiles of it
     */
    private static final int ROOM_DEFENCE_TILES = 5;

    /**
     * The flag goes on a corner of the enemy's Dungeon Heart, 2 tiles from
     * its centre on both axes
     */
    private static final int RALLY_FROM_HEART = 2;

    /**
     * On the way to the enemy the flag moves 2 tiles on once 4 in 5 of the
     * creatures are within 10 tiles of it
     */
    private static final int RALLY_GATHER_TILES = 10;
    private static final int RALLY_STEP_TILES = 2;

    /**
     * Creatures to the flag are dropped within 5 tiles of it
     */
    private static final int RALLY_DROP_TILES = 5;

    /**
     * DK II keeps 4 bits of the number of tunnels to dig at the same time
     */
    private static final int TUNNELS_MASK = 0xf;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameWorldController gameWorldController;
    private final IMapController mapController;
    private final INavigationService navigationService;
    private final KeeperAiBattles battles;
    private final KeeperAiBattles.DecisionLog decisionLog;

    KeeperAiInvasions(EntityData entityData, IKwdFile kwdFile, IGameWorldController gameWorldController,
            INavigationService navigationService, KeeperAiBattles battles, KeeperAiBattles.DecisionLog decisionLog) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameWorldController = gameWorldController;
        this.mapController = gameWorldController.getMapController();
        this.navigationService = navigationService;
        this.battles = battles;
        this.decisionLog = decisionLog;
    }

    /**
     * Sets out against an enemy when the army is ready. Not when it never
     * attacks, nor while it waits after an attack, nor while its Call to
     * Arms flag is up. It goes for the nearest enemy that its threat matches
     * by its settings, or with its settings only for the last keeper that
     * attacked it. A tunnel that was dug up to the enemy's land is broken
     * through first, otherwise a new tunnel is dug.
     *
     * @param keeperAi the computer player
     * @param playerControllers all the players
     * @param ai its settings
     * @param creatureLevel the level it wants its creatures at
     * @param allRoomsPlaced whether it has built every room it builds
     * @param ticksPerSecond the game ticks in a second
     * @return true if it digs a tunnel to an enemy
     */
    boolean planInvasion(KeeperAi keeperAi, Map<Short, IPlayerController> playerControllers, AI ai, int creatureLevel,
            boolean allRoomsPlaced, int ticksPerSecond) {
        if (ai.isNeverAttack() || keeperAi.cannotAttackTicks > 0 || keeperAi.callToArms) {
            return false;
        }

        IPlayerController playerController = playerControllers.get(keeperAi.playerId);
        boolean anyEnemy = false;
        short attackerId = 0;
        if (isArmyReady(keeperAi.playerId, ai, creatureLevel) && (!ai.isRequireAllRoomTypesPlaced() || allRoomsPlaced)) {
            if (ai.isRequireAllKeeperSpellsResearched() && hasSpellsToResearch(playerController)) {
                return false;
            }
            if (!ai.isOnlyAttackAttackers()) {
                anyEnemy = true;
            } else {
                attackerId = keeperAi.attackerId;
                keeperAi.attackerId = 0;
            }
        }
        if (!anyEnemy) {
            return attackerId != 0 && invadeAttacker(keeperAi, playerControllers, ai, attackerId);
        }

        // A tunnel ready to break through, otherwise the nearest enemy it can take on
        int ownThreat = getThreat(keeperAi.playerId);
        Point heart = playerController.getKeeper().getDungeonHeartLocation();
        IPlayerController target = null;
        int targetDistance = Integer.MAX_VALUE;
        for (IPlayerController enemy : getEnemies(playerControllers, playerController.getKeeper())) {
            KeeperAiEnemy keeperAiEnemy = getEnemy(keeperAi, enemy.getKeeper().getId(), ai);
            if (keeperAiEnemy.tunnelsLeft == 0) {
                if (breakThrough(keeperAi, keeperAiEnemy)) {
                    keeperAi.cannotAttackTicks = hasBreachPoint(keeperAiEnemy) ? 0
                            : ai.getWaitSecondsAfterPreviousAttackBeforeAttackingAgain() * ticksPerSecond;
                    return true;
                }
                continue;
            }
            int distance = getDistance(heart, enemy.getKeeper().getDungeonHeartLocation());
            if (getThreat(enemy.getKeeper().getId()) * ai.getTotalThreatGreaterThanTheEnemy() / 100 <= ownThreat
                    && distance < targetDistance) {
                target = enemy;
                targetDistance = distance;
            }
        }
        if (target == null) {
            return false;
        }

        KeeperAiEnemy keeperAiEnemy = getEnemy(keeperAi, target.getKeeper().getId(), ai);
        keeperAiEnemy.invaded = true;
        IRoomController breachRoom;
        IRoomController startRoom;
        if (!keeperAiEnemy.breached) {
            keeperAiEnemy.breached = true;
            breachRoom = getBreachRoom(target, ai.getFirstAttemptToBreachRoom());
            startRoom = breachRoom != null ? getStartRoom(playerController, ai.getFirstDigToEnemyPoint(), breachRoom) : null;
        } else {
            startRoom = getRandomRoom(playerController);
            breachRoom = getRandomRoom(target);
        }
        if (breachRoom == null || startRoom == null) {
            entityData.setComponent(findEnemy(keeperAi.playerId, keeperAiEnemy.enemyId), keeperAiEnemy);
            return false;
        }

        digTunnel(keeperAi, ai, keeperAiEnemy, startRoom, breachRoom);

        return true;
    }

    /**
     * Only the keeper that attacked it last is invaded, from its room
     * nearest to that keeper's room nearest to its own Dungeon Heart
     */
    private boolean invadeAttacker(KeeperAi keeperAi, Map<Short, IPlayerController> playerControllers, AI ai, short attackerId) {
        IPlayerController attacker = playerControllers.get(attackerId);
        if (attacker == null || attacker.getRoomControl().getDungeonHeart() == null) {
            return false;
        }
        KeeperAiEnemy keeperAiEnemy = getEnemy(keeperAi, attackerId, ai);
        if (keeperAiEnemy.tunnelsLeft == 0) {
            return breakThrough(keeperAi, keeperAiEnemy);
        }

        keeperAiEnemy.invaded = true;
        entityData.setComponent(findEnemy(keeperAi.playerId, attackerId), keeperAiEnemy);
        if (getThreat(keeperAi.playerId) < getThreat(attackerId) * ai.getTotalThreatGreaterThanTheEnemy() / 100) {
            return false;
        }
        IPlayerController playerController = playerControllers.get(keeperAi.playerId);
        IRoomController breachRoom = getNearestRoom(attacker, playerController.getKeeper().getDungeonHeartLocation());
        IRoomController startRoom = breachRoom != null ? getNearestRoom(playerController, breachRoom.getRoomInstance().getCenter()) : null;
        if (breachRoom == null || startRoom == null) {
            return false;
        }

        digTunnel(keeperAi, ai, keeperAiEnemy, startRoom, breachRoom);

        return true;
    }

    private void digTunnel(KeeperAi keeperAi, AI ai, KeeperAiEnemy keeperAiEnemy, IRoomController startRoom, IRoomController breachRoom) {
        Point start = startRoom.getRoomInstance().getCenter();
        Point target = breachRoom.getRoomInstance().getCenter();
        keeperAiEnemy.tunnelsLeft--;
        entityData.setComponent(findEnemy(keeperAi.playerId, keeperAiEnemy.enemyId), keeperAiEnemy);
        startDig(keeperAi, start, target, "a tunnel to room " + breachRoom.getRoomInstance().getRoom().getRoomId()
                + " of player " + keeperAiEnemy.enemyId + ", " + keeperAiEnemy.tunnelsLeft + " more to dig");
        keeperAi.invasionTargetId = keeperAiEnemy.enemyId;

        // On the way to the enemy the first call to arms is at the room the last tunnel starts from
        if (ai.getUseCallToArms() == AI.CallToArmsUsagePolicy.TOWARDS_HEART && keeperAiEnemy.tunnelsLeft == 0 && !keeperAi.callToArms) {
            keeperAi.rallyTile = start;
        }
    }

    /**
     * Digs on from where the first tunnel kept stopped, into the enemy's
     * land
     */
    private boolean breakThrough(KeeperAi keeperAi, KeeperAiEnemy keeperAiEnemy) {
        for (int i = 0; i < KeeperAiEnemy.BREACH_POINTS; i++) {
            if (keeperAiEnemy.breachTiles[i] != null) {
                Point start = keeperAiEnemy.breachTiles[i];
                Point target = keeperAiEnemy.breachTargets[i];
                keeperAiEnemy.breachTiles[i] = null;
                keeperAiEnemy.breachTargets[i] = null;
                entityData.setComponent(findEnemy(keeperAi.playerId, keeperAiEnemy.enemyId), keeperAiEnemy);
                startDig(keeperAi, start, target, "break through to player " + keeperAiEnemy.enemyId);
                keeperAi.invasionTargetId = 0;

                return true;
            }
        }

        return false;
    }

    private static boolean hasBreachPoint(KeeperAiEnemy keeperAiEnemy) {
        for (Point tile : keeperAiEnemy.breachTiles) {
            if (tile != null) {
                return true;
            }
        }

        return false;
    }

    private void startDig(KeeperAi keeperAi, Point start, Point target, String reason) {
        keeperAi.current = start;
        keeperAi.target = target;
        keeperAi.task = KeeperAi.Task.DIG_PATH;
        keeperAi.nextTask = KeeperAi.Task.FINISH;
        keeperAi.taskTicks = 0;
        decisionLog.log(keeperAi, "next DIG_PATH from " + start.x + "," + start.y, target, reason);
    }

    /**
     * Stops the tunnel being dug when it comes next to an enemy's land, and
     * keeps where it stopped to break through later
     *
     * @param keeperAi the computer player
     * @param keeper its keeper
     * @return true if the tunnel stops here
     */
    boolean stopTunnel(KeeperAi keeperAi, Keeper keeper) {
        Point p = keeperAi.current;
        boolean enemyLand = false;
        for (int x = p.x - 1; x <= p.x + 1 && !enemyLand; x++) {
            for (int y = p.y - 1; y <= p.y + 1 && !enemyLand; y++) {
                IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                enemyLand = tile != null && tile.getOwnerId() != 0 && keeper.isEnemy(tile.getOwnerId());
            }
        }
        if (!enemyLand) {
            return false;
        }

        EntityId entityId = findEnemy(keeperAi.playerId, keeperAi.invasionTargetId);
        KeeperAiEnemy keeperAiEnemy = entityId != null ? new KeeperAiEnemy(entityData.getComponent(entityId, KeeperAiEnemy.class)) : null;
        for (int i = 0; keeperAiEnemy != null && i < KeeperAiEnemy.BREACH_POINTS; i++) {
            if (keeperAiEnemy.breachTiles[i] == null) {
                keeperAiEnemy.breachTiles[i] = p;
                keeperAiEnemy.breachTargets[i] = keeperAi.target;
                entityData.setComponent(entityId, keeperAiEnemy);
                break;
            }
        }
        decisionLog.log(keeperAi, "stop the tunnel to player " + keeperAi.invasionTargetId, p, "next to an enemy's land");
        keeperAi.invasionTargetId = 0;

        return true;
    }

    /**
     * Calls its creatures to arms against an enemy it has set out against,
     * once a creature can walk to that enemy's Dungeon Heart. Not while it
     * answers an attack or waits after one. With its settings it also drops
     * half its threat near the flag, if it has land there.
     *
     * @param keeperAi the computer player
     * @param playerControllers all the players
     * @param ai its settings
     * @return true if it spent its action
     */
    boolean callToArms(KeeperAi keeperAi, Map<Short, IPlayerController> playerControllers, AI ai) {
        AI.CallToArmsUsagePolicy policy = ai.getUseCallToArms();
        if (keeperAi.attacks > 0 || (keeperAi.callToArms && policy != AI.CallToArmsUsagePolicy.TOWARDS_HEART)
                || keeperAi.cannotAttackTicks > 0) {
            return false;
        }
        Point rally = getRallyTile(keeperAi, playerControllers, policy);
        if (rally == null) {
            return false;
        }

        IPlayerController playerController = playerControllers.get(keeperAi.playerId);
        KeeperSpell callToArms = kwdFile.getKeeperSpellById(SPELL_CALL_TO_ARMS);
        if (callToArms == null || !playerController.getSpellControl().isAvailable(callToArms)) {
            return true;
        }
        int mana = playerController.getKeeper().getMana();
        gameWorldController.castKeeperSpell(SPELL_CALL_TO_ARMS, null, rally, WorldUtils.pointToVector2f(rally), keeperAi.playerId);

        // The cast has its own rules, the mana tells if it went through
        if (playerController.getKeeper().getMana() >= mana) {
            return true;
        }
        decisionLog.log(keeperAi, "cast Call to Arms", rally, "on the way to an enemy it invades, " + policy);
        keeperAi.callToArms = true;
        keeperAi.callToArmsTile = rally;
        keeperAi.callToArmsTicks = ai.getRemoveCallToArmsAfterSeconds();
        keeperAi.rallyTile = rally;
        if (policy == AI.CallToArmsUsagePolicy.AT_HEART_WITH_CREATURES && keeperAi.attacks < keeperAi.attackNeeds.length) {
            Point drop = battles.getNearestOwnOpenTile(keeperAi.playerId, rally, RALLY_DROP_TILES);
            if (drop != null) {
                int need = getThreat(keeperAi.playerId) / 2;
                int attack = (keeperAi.firstAttack + keeperAi.attacks) % keeperAi.attackNeeds.length;
                decisionLog.log(keeperAi, "drop a threat of " + need + " at the flag", drop, "half of its threat");
                keeperAi.attackNeeds[attack] = need;
                keeperAi.attackTiles[attack] = drop;
                keeperAi.attacks++;
            }
        }

        return true;
    }

    /**
     * Where to call the creatures to arms: a corner of the Dungeon Heart of
     * the first enemy it invades that a creature can walk to. On the way
     * there the flag starts at its own room and moves 2 tiles on each time
     * enough of its creatures have come.
     */
    private Point getRallyTile(KeeperAi keeperAi, Map<Short, IPlayerController> playerControllers, AI.CallToArmsUsagePolicy policy) {
        List<Entity> creatures = battles.getCreatures(keeperAi.playerId);
        for (EntityId entityId : entityData.findEntities(new FieldFilter<>(KeeperAiEnemy.class, "playerId", keeperAi.playerId), KeeperAiEnemy.class)) {
            KeeperAiEnemy keeperAiEnemy = entityData.getComponent(entityId, KeeperAiEnemy.class);
            if (!keeperAiEnemy.invaded) {
                continue;
            }
            IPlayerController enemy = playerControllers.get(keeperAiEnemy.enemyId);
            if (enemy == null || enemy.getKeeper().isDestroyed()) {
                KeeperAiEnemy gone = new KeeperAiEnemy(keeperAiEnemy);
                gone.invaded = false;
                entityData.setComponent(entityId, gone);
                continue;
            }
            IRoomController heart = enemy.getRoomControl().getDungeonHeart();
            Entity walker = getWalker(creatures);
            if (heart == null || heart.getRoomInstance().isDestroyed() || walker == null) {
                continue;
            }

            Point centre = heart.getRoomInstance().getCenter();
            Point corner = new Point(centre.x + (Utils.getRandom().nextBoolean() ? -RALLY_FROM_HEART : RALLY_FROM_HEART),
                    centre.y + (Utils.getRandom().nextBoolean() ? -RALLY_FROM_HEART : RALLY_FROM_HEART));
            Point from = WorldUtils.vectorToPoint(walker.get(Position.class).position);
            var navigable = gameWorldController.getCreaturesController().createController(walker.getId());
            if (navigationService.findPath(from, corner, navigable) == null) {
                continue;
            }
            if (policy != AI.CallToArmsUsagePolicy.TOWARDS_HEART) {
                return corner;
            }
            if (!keeperAi.callToArms) {
                return keeperAi.rallyTile;
            }

            // Most of its creatures have to be at the flag before it moves on
            int fighters = 0;
            int near = 0;
            for (Entity creature : creatures) {
                if (!creature.get(CreatureComponent.class).worker) {
                    fighters++;
                    if (keeperAi.rallyTile != null && KeeperAiBattles.isWithin(creature.get(Position.class).position,
                            WorldUtils.pointToVector3f(keeperAi.rallyTile), RALLY_GATHER_TILES)) {
                        near++;
                    }
                }
            }
            GraphPath<IMapTileInformation> path = keeperAi.rallyTile != null
                    ? navigationService.findPath(keeperAi.rallyTile, corner, navigable) : null;
            if (path != null && path.getCount() > 0 && fighters <= near * 5 / 4) {
                return path.get(Math.min(RALLY_STEP_TILES, path.getCount() - 1)).getLocation();
            }
        }

        return null;
    }

    /**
     * The creature DK II asks the way with: the first one that can't walk
     * on both water and lava, otherwise the first one
     */
    private Entity getWalker(List<Entity> creatures) {
        for (Entity creature : creatures) {
            Set<Creature.CreatureFlag> flags = kwdFile.getCreature(creature.get(CreatureComponent.class).creatureId).getFlags();
            if (!flags.contains(Creature.CreatureFlag.CAN_WALK_ON_WATER) || !flags.contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA)) {
                return creature;
            }
        }

        return creatures.isEmpty() ? null : creatures.getFirst();
    }

    /**
     * The army is ready with more fighters than its settings ask for, all of
     * them at the level it wants. When only a few are below that level, as
     * many as the level or fewer, they go up one level first.
     */
    private boolean isArmyReady(short playerId, AI ai, int creatureLevel) {
        List<Entity> fighters = new ArrayList<>();
        List<Entity> below = new ArrayList<>();
        for (Entity creature : battles.getCreatures(playerId)) {
            if (!creature.get(CreatureComponent.class).worker) {
                fighters.add(creature);
                if (getLevel(creature.getId()) < creatureLevel) {
                    below.add(creature);
                }
            }
        }
        if (fighters.size() <= ai.getAttackWhenNumberOfCreaturesIsAtLeast()) {
            return false;
        }
        if (below.size() <= creatureLevel && below.size() < fighters.size()) {
            for (Entity creature : below) {
                gameWorldController.getCreaturesController().levelUpCreature(creature.getId(), getLevel(creature.getId()) + 1, 0);
            }
        }
        for (Entity creature : fighters) {
            if (getLevel(creature.getId()) < creatureLevel) {
                return false;
            }
        }

        return true;
    }

    private int getLevel(EntityId entityId) {
        CreatureExperience experience = entityData.getComponent(entityId, CreatureExperience.class);

        return experience != null ? experience.level : 1;
    }

    private boolean hasSpellsToResearch(IPlayerController playerController) {
        for (ResearchableEntity spell : playerController.getSpellControl().getTypes().values()) {
            if (!spell.isDiscovered()) {
                return true;
            }
        }

        return false;
    }

    private int getThreat(short playerId) {
        int threat = 0;
        for (Entity creature : battles.getCreatures(playerId)) {
            Threat creatureThreat = entityData.getComponent(creature.getId(), Threat.class);
            threat += creatureThreat != null ? creatureThreat.threat : 0;
        }

        return threat;
    }

    /**
     * The keepers it is at war with that still have their Dungeon Heart, by
     * id
     */
    private static List<IPlayerController> getEnemies(Map<Short, IPlayerController> playerControllers, Keeper keeper) {
        List<IPlayerController> enemies = new ArrayList<>();
        for (IPlayerController other : playerControllers.values()) {
            Keeper enemy = other.getKeeper();
            IRoomController heart = other.getRoomControl().getDungeonHeart();
            if (enemy.getId() >= Player.KEEPER1_ID && keeper.isEnemy(enemy.getId()) && !enemy.isDestroyed()
                    && heart != null && !heart.getRoomInstance().isDestroyed()) {
                enemies.add(other);
            }
        }
        enemies.sort(Comparator.comparingInt(other -> other.getKeeper().getId()));

        return enemies;
    }

    private EntityId findEnemy(short playerId, short enemyId) {
        for (EntityId entityId : entityData.findEntities(new FieldFilter<>(KeeperAiEnemy.class, "playerId", playerId), KeeperAiEnemy.class)) {
            if (entityData.getComponent(entityId, KeeperAiEnemy.class).enemyId == enemyId) {
                return entityId;
            }
        }

        return null;
    }

    /**
     * What it has done against an enemy, a copy to change and store again.
     * The first time it starts with the tunnels its settings say
     */
    private KeeperAiEnemy getEnemy(KeeperAi keeperAi, short enemyId, AI ai) {
        EntityId entityId = findEnemy(keeperAi.playerId, enemyId);
        if (entityId == null) {
            KeeperAiEnemy keeperAiEnemy = new KeeperAiEnemy(keeperAi.playerId, enemyId, ai.getBreachAtPointsSimultaneously() & TUNNELS_MASK);
            entityData.setComponent(entityData.createEntity(), keeperAiEnemy);

            return keeperAiEnemy;
        }

        return new KeeperAiEnemy(entityData.getComponent(entityId, KeeperAiEnemy.class));
    }

    /**
     * The enemy room the first tunnel goes to: the Dungeon Heart, a random
     * room, or the room with the least or the most of the enemy's threat
     * around it. A room is only the most defended with some threat around it
     */
    private IRoomController getBreachRoom(IPlayerController enemy, AI.BreachRoomPolicy policy) {
        switch (policy) {
            case ENEMY_DUNGEON_HEART -> {
                return enemy.getRoomControl().getDungeonHeart();
            }
            case RANDOM -> {
                return getRandomRoom(enemy);
            }
            default -> {
                boolean weakest = policy == AI.BreachRoomPolicy.WEAK_ROOM;
                IRoomController found = null;
                int foundThreat = weakest ? Integer.MAX_VALUE : 0;
                for (IRoomController room : getRooms(enemy)) {
                    int threat = battles.getThreat(enemy.getKeeper().getId(), WorldUtils.pointToVector3f(room.getRoomInstance().getCenter()),
                            ROOM_DEFENCE_TILES);
                    if (weakest ? threat < foundThreat : threat > foundThreat) {
                        found = room;
                        foundThreat = threat;
                    }
                }

                return found;
            }
        }
    }

    /**
     * Its own room the first tunnel starts from: its room furthest from its
     * Dungeon Heart, a random room, its Dungeon Heart, or its room nearest
     * to the room the tunnel goes to
     */
    private IRoomController getStartRoom(IPlayerController playerController, AI.DigToPolicy policy, IRoomController breachRoom) {

        // DK II keeps two bits of the setting
        switch (policy.getValue() & 3) {
            case 0 -> {
                Point heart = playerController.getKeeper().getDungeonHeartLocation();
                IRoomController furthest = null;
                int furthestDistance = 0;
                for (IRoomController room : getRooms(playerController)) {
                    int distance = getDistance(room.getRoomInstance().getCenter(), heart);
                    if (distance > furthestDistance) {
                        furthest = room;
                        furthestDistance = distance;
                    }
                }

                return furthest;
            }
            case 1 -> {
                return getRandomRoom(playerController);
            }
            case 2 -> {
                return playerController.getRoomControl().getDungeonHeart();
            }
            default -> {
                return getNearestRoom(playerController, breachRoom.getRoomInstance().getCenter());
            }
        }
    }

    private static List<IRoomController> getRooms(IPlayerController playerController) {
        List<IRoomController> rooms = new ArrayList<>();
        for (Set<IRoomController> roomsOfType : playerController.getRoomControl().getRoomControllers().values()) {
            for (IRoomController room : roomsOfType) {
                if (!BRIDGES.contains(room.getRoomInstance().getRoom().getRoomId()) && !room.getRoomInstance().isDestroyed()) {
                    rooms.add(room);
                }
            }
        }

        // The heart first
        rooms.sort(Comparator.comparingInt(room -> room.getRoomInstance().getRoom().getRoomId() == ROOM_DUNGEON_HEART ? 0 : 1));

        return rooms;
    }

    private static IRoomController getRandomRoom(IPlayerController playerController) {
        List<IRoomController> rooms = getRooms(playerController);

        return rooms.isEmpty() ? null : Utils.getRandomItem(rooms);
    }

    private static IRoomController getNearestRoom(IPlayerController playerController, Point p) {
        IRoomController nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (IRoomController room : getRooms(playerController)) {
            int distance = getDistance(room.getRoomInstance().getCenter(), p);
            if (distance < nearestDistance) {
                nearest = room;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static int getDistance(Point from, Point to) {
        return (from.x - to.x) * (from.x - to.x) + (from.y - to.y) * (from.y - to.y);
    }

}
