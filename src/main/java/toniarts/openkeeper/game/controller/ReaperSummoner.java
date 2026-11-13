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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.util.Random;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Summons the Horned Reaper, what the Summon Reaper shot does. The Reaper
 * appears where the spell was cast. The shot data tells how long he stays: 0
 * means he stays as long as the keeper pays for him, as with the Summon Reaper
 * spell. Otherwise he stays for that many seconds for free, as with the Reaper
 * Talisman.
 * <p>
 * Also sends the Reaper for the Portal Gem when a keeper wins a campaign level,
 * as in DK II: the keeper's own Reaper goes if he has one and can get to the
 * gem, otherwise a Reaper appears on a free tile five to eight tiles from the
 * gem, from where he can walk to it.</p>
 */
public final class ReaperSummoner {

    /**
     * The Reaper of the Summon Reaper spell is always this strong
     */
    public static final int SPELL_REAPER_LEVEL = 10;

    private static final Logger logger = System.getLogger(ReaperSummoner.class.getName());

    private final IKwdFile kwdFile;
    private final EntityData entityData;
    private static final int ENTRANCE_TRIES = 500;
    private static final int TRIES_BEFORE_ANY_DISTANCE = 128;
    private static final int ENTRANCE_OFFSET = 8;
    private static final int MIN_DISTANCE_SQUARED = 25;

    private final IGameTimer gameTimer;
    private final ICreaturesController creaturesController;

    public ReaperSummoner(IKwdFile kwdFile, EntityData entityData, IGameTimer gameTimer, ICreaturesController creaturesController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.creaturesController = creaturesController;
    }

    /**
     * Summons the Horned Reaper
     *
     * @param shotTypeId the Summon Reaper shot
     * @param shotData1 how many seconds the Reaper stays, 0 if he stays for as
     * long as the keeper pays for him
     * @param playerId the summoning keeper
     * @param position where to summon the Reaper
     * @return the Reaper, {@code null} if the level has no Reaper
     */
    public EntityId summon(short shotTypeId, int shotData1, short playerId, Vector2f position) {
        Creature reaper = getReaper(kwdFile);
        if (reaper == null) {
            logger.log(Logger.Level.WARNING, "No Horned Reaper to summon");
            return null;
        }

        // The Talisman Reaper has the level of the map and no upkeep
        boolean timed = shotData1 > 0;
        int level = timed ? Math.max(1, kwdFile.getGameLevel().getOneShotHornyLev()) : SPELL_REAPER_LEVEL;
        int manaDrain = timed ? 0 : getManaDrain(shotTypeId);

        EntityId entityId = creaturesController.spawnCreature(reaper.getId(), playerId, level, position, ICreaturesController.SpawnType.CONJURE);
        entityData.setComponent(entityId, new SummonedReaper(gameTimer.getGameTime(), timed ? shotData1 : 0, manaDrain));

        return entityId;
    }

    /**
     * Sends the Horned Reaper for the Portal Gem, if the level has one. The
     * Reaper is marked with the gem, so he is sent only once.
     *
     * @param playerId the keeper who won
     * @param objectsController the objects, to drop a gem still carried
     * @param navigationService finds the way to the gem
     * @param mapInformation tells the solid tiles
     * @param random picks the tile the Reaper appears on
     * @return the Reaper, {@code null} if there is no gem, no Reaper or he has
     * been sent already
     */
    public EntityId sendForPortalGem(short playerId, IObjectsController objectsController, INavigationService navigationService,
            IMapInformation<?> mapInformation, Random random) {
        Creature reaperCreature = getReaper(kwdFile);
        if (reaperCreature == null || isGemFetched()) {
            return null;
        }

        EntityId gem = findGem(objectsController);
        if (gem == null) {
            return null;
        }

        INavigable navigable = new ReaperNavigable(playerId, reaperCreature);
        Point gemTile = WorldUtils.vectorToPoint(entityData.getComponent(gem, Position.class).position);
        EntityId reaper = findOwnReaper(playerId, gemTile, navigable, navigationService);
        SummonedReaper summonedReaper;
        if (reaper == null) {
            Point entrance = findEntrance(gemTile, navigable, navigationService, mapInformation, random);
            int level = Math.max(1, kwdFile.getGameLevel().getOneShotHornyLev());
            reaper = creaturesController.spawnCreature(reaperCreature.getId(), playerId, level,
                    WorldUtils.pointToVector2f(entrance), ICreaturesController.SpawnType.CONJURE);
            summonedReaper = new SummonedReaper(gameTimer.getGameTime(), 0, 0);
        } else {

            // A Reaper of the map is looked after with the summoned ones from now on
            SummonedReaper old = entityData.getComponent(reaper, SummonedReaper.class);
            summonedReaper = old != null ? new SummonedReaper(old.summonTime, old.lifetime, old.manaDrain, old.manaCheckTime, old.leaveTime)
                    : new SummonedReaper(gameTimer.getGameTime(), 0, 0);
            entityData.removeComponent(reaper, AttackTarget.class);
            entityData.removeComponent(reaper, Navigation.class);
        }
        summonedReaper.gem = gem;
        entityData.setComponent(reaper, summonedReaper);

        return reaper;
    }

    private boolean isGemFetched() {
        for (EntityId entityId : entityData.findEntities(null, SummonedReaper.class)) {
            if (entityData.getComponent(entityId, SummonedReaper.class).gem != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * The Portal Gem on the floor, or if it is still carried by a creature,
     * dropped where the creature is
     */
    private EntityId findGem(IObjectsController objectsController) {
        Short gemObjectId = null;
        for (GameObject object : kwdFile.getObjectList()) {
            if (object.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_LEVEL_GEM)) {
                gemObjectId = object.getObjectId();
                break;
            }
        }
        if (gemObjectId == null) {
            return null;
        }

        for (EntityId entityId : entityData.findEntities(null, ObjectComponent.class, Position.class)) {
            if (entityData.getComponent(entityId, ObjectComponent.class).objectId == gemObjectId) {
                return entityId;
            }
        }
        for (EntityId carrier : entityData.findEntities(null, PortalGem.class, Position.class)) {
            return objectsController.dropPortalGem(carrier);
        }
        return null;
    }

    private EntityId findOwnReaper(short playerId, Point gemTile, INavigable navigable, INavigationService navigationService) {
        for (EntityId entityId : entityData.findEntities(null, CreatureComponent.class, Owner.class, Health.class)) {
            if (entityData.getComponent(entityId, Owner.class).ownerId != playerId
                    || entityData.getComponent(entityId, Death.class) != null
                    || entityData.getComponent(entityId, InHand.class) != null
                    || entityData.getComponent(entityId, Unconscious.class) != null
                    || entityData.getComponent(entityId, CreatureImprisoned.class) != null
                    || entityData.getComponent(entityId, CreatureTortured.class) != null) {
                continue;
            }
            Creature creature = kwdFile.getCreature(entityData.getComponent(entityId, CreatureComponent.class).creatureId);
            SummonedReaper summonedReaper = entityData.getComponent(entityId, SummonedReaper.class);
            if (creature != null && creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)
                    && (summonedReaper == null || !summonedReaper.isLeaving())
                    && navigationService.findPath(WorldUtils.vectorToPoint(entityData.getComponent(entityId, Position.class).position),
                            gemTile, navigable) != null) {
                return entityId;
            }
        }
        return null;
    }

    /**
     * A random free tile around the gem. Close tiles are taken only when
     * nothing farther has turned up for a while, and the gem tile itself if
     * nothing is found at all.
     */
    private Point findEntrance(Point gemTile, INavigable reaper, INavigationService navigationService,
            IMapInformation<?> mapInformation, Random random) {
        for (int i = 0; i < ENTRANCE_TRIES; i++) {
            int dx = random.nextInt(ENTRANCE_OFFSET * 2 + 1) - ENTRANCE_OFFSET;
            int dy = random.nextInt(ENTRANCE_OFFSET * 2 + 1) - ENTRANCE_OFFSET;
            if (i < TRIES_BEFORE_ANY_DISTANCE && dx * dx + dy * dy < MIN_DISTANCE_SQUARED) {
                continue;
            }
            Point tile = new Point(gemTile.x + dx, gemTile.y + dy);

            // The cheap checks first, the search is the expensive part
            if (!mapInformation.isSolid(tile) && !hasCreature(tile) && navigationService.findPath(gemTile, tile, reaper) != null) {
                return tile;
            }
        }
        return gemTile;
    }

    private boolean hasCreature(Point tile) {
        for (EntityId entityId : entityData.findEntities(null, CreatureComponent.class, Position.class)) {
            if (tile.equals(WorldUtils.vectorToPoint(entityData.getComponent(entityId, Position.class).position))) {
                return true;
            }
        }
        return false;
    }

    /**
     * How the Reaper moves, before there is a Reaper
     */
    private record ReaperNavigable(short ownerId, Creature creature) implements INavigable {

        @Override
        public short getOwnerId() {
            return ownerId;
        }

        @Override
        public boolean canFly() {
            return creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY);
        }

        @Override
        public boolean canWalkOnWater() {
            return creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_WATER);
        }

        @Override
        public boolean canWalkOnLava() {
            return creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA);
        }

        @Override
        public boolean canMoveDiagonally() {
            return true;
        }

    }

    private static Creature getReaper(IKwdFile kwdFile) {
        for (Creature creature : kwdFile.getCreatureList()) {
            if (creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                return creature;
            }
        }
        return null;
    }

    /**
     * The upkeep is the mana drain of the spell that summons the Reaper for
     * good
     */
    private int getManaDrain(short shotTypeId) {
        for (KeeperSpell keeperSpell : kwdFile.getKeeperSpells()) {
            if (keeperSpell.getShotTypeId() == shotTypeId && keeperSpell.getShotData1() == 0) {
                return keeperSpell.getManaDrain();
            }
        }
        return 0;
    }

}
