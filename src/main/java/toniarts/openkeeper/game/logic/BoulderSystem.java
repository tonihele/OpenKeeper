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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Map;
import toniarts.openkeeper.game.component.Boulder;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.PrisonController;
import toniarts.openkeeper.game.controller.room.TempleController;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;

/**
 * Rolls the boulders of the Boulder Traps, every game tick like the original
 * (DKII.EXE 0x4A1390).
 * <br>
 * A boulder rolls the way it was sent. Each time its middle is on the middle
 * line of a tile it looks at the tile ahead, and when that is blocked it turns:
 * to the side that is open, with both sides open to the side it first set off
 * to (at random when it set off straight), and back the way it came when both
 * are blocked. A turn costs it a tenth of its full health and ends its move
 * for the tick (0x4A0E30, 0x4A0050, 0x4A0210, 0x4A03E0, 0x4A05B0, 0x4A0770).
 * Solid tiles block it, the Prison's cage, the 2 x 2 Hero Gate and the objects
 * a boulder can't roll through (0x453C10).
 * <br>
 * It slows down as it wears out: of the level's boulder speed the share of
 * the speed to health ratio goes by the health it has left, and the rest
 * counts five times (0x4A0E30). It breaks when its health is gone.
 * <br>
 * Before it moves it runs over the creatures it touches on its tile and the
 * eight around it, whoever they belong to: each loses half its full health
 * divided by the level's boulder to creature damage percentage, 50 in nearly
 * every level, so all of it, but no more than the boulder has health left.
 * What the creature loses the boulder loses too, and the creature is knocked
 * off its feet. Creatures that are out cold, in a hand or already down are
 * left alone (0x4A0930).
 * <br>
 * It smashes what stands in its way the same way, paying with its health: a
 * built door it touches takes up to the door type's full health a tick, a
 * built trap a quarter of the trap type's health, and an object that boulders
 * destroy, like a bed or a chicken, its type's health. Only its own keeper's
 * Trigger Traps are spared (0x4A0930).
 * <br>
 * After its move the ground wears it: each tick it loses a sixth of its full
 * health on lava, a tenth in water and a third down in the Temple's pool or
 * the Combat Pit (0x4A10E0).
 */
public final class BoulderSystem implements IGameLogicUpdatable {

    /**
     * DK II counts positions in 4096ths of a tile, and so does the rolling
     * here, to turn on the same spots
     */
    private static final int TILE = 4096;
    private static final int HALF_TILE = TILE / 2;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final ICreaturesController creaturesController;
    private final int speed;
    private final int speedToHealthRatio;
    private final int creatureDamagePercentage;
    private final EntitySet boulders;

    /**
     * The boulder system
     *
     * @param entityData the entity data
     * @param kwdFile the level data
     * @param gameSettings the level variables, for the boulder's speed and
     * damage
     * @param mapController the map, for what blocks a boulder
     * @param entityPositionLookup finds the things on a tile
     * @param creaturesController knocks the creatures down
     */
    public BoulderSystem(EntityData entityData, IKwdFile kwdFile, Map<MiscType, Variable.MiscVariable> gameSettings,
            IMapController mapController, IEntityPositionLookup entityPositionLookup, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;
        this.creaturesController = creaturesController;
        speed = (int) (gameSettings.get(MiscType.BOULDER_SPEED_TILES_PER_SECOND).getValue() * TILE)
                / kwdFile.getGameLevel().getTicksPerSec();
        speedToHealthRatio = (int) gameSettings.get(MiscType.BOULDER_SPEED_TO_HEALTH_RATIO).getValue();
        creatureDamagePercentage = (int) gameSettings.get(MiscType.BOULDER_TO_CREATURE_DAMAGE_PERCENTAGE).getValue();

        boulders = entityData.getEntities(Boulder.class, Position.class, Health.class);
    }

    @Override
    public void processTick(float tpf) {
        boulders.applyChanges();

        for (Entity boulder : boulders) {
            Health health = boulder.get(Health.class);
            Damage damage = entityData.getComponent(boulder.getId(), Damage.class);
            int healthLeft = health.health - (damage != null ? damage.damage : 0);
            if (healthLeft > 0) {
                new Roll(boulder, health.maxHealth, healthLeft).roll();
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        boulders.release();
    }

    /**
     * One tick of a boulder
     */
    private final class Roll {

        private final EntityId entityId;
        private final Boulder boulder;
        private final Position position;
        private final int health;
        private final int healthLeft;
        private int x;
        private int y;
        private int directionX;
        private int directionY;
        private int wear;

        private Roll(Entity entity, int health, int healthLeft) {
            entityId = entity.getId();
            boulder = entity.get(Boulder.class);
            position = entity.get(Position.class);
            this.health = health;
            this.healthLeft = healthLeft;

            // The middle of a tile is at a whole number in the world and half way in DK II's tiles
            x = Math.round(position.position.x * TILE) + HALF_TILE;
            y = Math.round(position.position.z * TILE) + HALF_TILE;
            directionX = Math.round(boulder.directionX * TILE);
            directionY = Math.round(boulder.directionY * TILE);
        }

        private void roll() {
            hitThings();
            move();
            wearOnTheGround();

            entityData.setComponents(entityId,
                    new Boulder((float) directionX / TILE, (float) directionY / TILE, boulder.turnX, boulder.turnY),
                    new Position(FastMath.atan2(directionX, directionY), new Vector3f((float) (x - HALF_TILE) / TILE,
                            position.position.y, (float) (y - HALF_TILE) / TILE)));
            if (wear > 0) {
                EntityController.setDamage(entityData, entityId, wear);
            }
        }

        private void hitThings() {
            int tileX = Math.floorDiv(x, TILE);
            int tileY = Math.floorDiv(y, TILE);
            for (int tileOffsetX = -1; tileOffsetX <= 1; tileOffsetX++) {
                for (int tileOffsetY = -1; tileOffsetY <= 1; tileOffsetY++) {
                    for (EntityId thing : entityPositionLookup.getEntitiesInLocation(new Point(tileX + tileOffsetX, tileY + tileOffsetY))) {
                        if (!thing.equals(entityId) && wear < healthLeft) {
                            hit(thing);
                        }
                    }
                }
            }
        }

        private void hit(EntityId thing) {
            Health thingHealth = entityData.getComponent(thing, Health.class);
            if (thingHealth == null) {
                return;
            }
            CreatureComponent creature = entityData.getComponent(thing, CreatureComponent.class);
            DoorComponent door = entityData.getComponent(thing, DoorComponent.class);
            TrapViewState trap = entityData.getComponent(thing, TrapViewState.class);
            ObjectComponent object = entityData.getComponent(thing, ObjectComponent.class);
            if (creature != null) {
                hitCreature(thing, creature, thingHealth);
            } else if (door != null) {
                if (!door.blueprint && isTouching(thing, 0)) {
                    smash(thing, thingHealth, kwdFile.getDoorById(door.doorId).getHealth());
                }
            } else if (trap != null) {
                Trap trapType = kwdFile.getTrapById(trap.trapId);
                if (!trap.blueprint && !isOwnTriggerTrap(thing, trapType) && isTouching(thing, 0)) {
                    smash(thing, thingHealth, trapType.getHealth() / 4);
                }
            } else if (object != null) {
                GameObject objectType = kwdFile.getObject(object.objectId);
                if (objectType.getFlags().contains(GameObject.ObjectFlag.BOULDER_DESTROYS) && isTouching(thing, objectType.getWidth())) {
                    smash(thing, thingHealth, objectType.getHp());
                }
            }
        }

        private boolean isOwnTriggerTrap(EntityId trap, Trap trapType) {
            Owner owner = entityData.getComponent(entityId, Owner.class);
            Owner trapOwner = entityData.getComponent(trap, Owner.class);

            return trapType.getTriggerType() == Trap.TriggerType.TRIGGER && owner != null && trapOwner != null
                    && owner.ownerId == trapOwner.ownerId;
        }

        private void smash(EntityId thing, Health thingHealth, int damage) {
            damage = Math.min(Math.min(damage, healthLeft - wear), thingHealth.health);
            EntityController.setDamage(entityData, thing, damage);
            wear += damage;
        }

        private void hitCreature(EntityId thing, CreatureComponent creature, Health creatureHealth) {
            CreatureAi creatureAi = entityData.getComponent(thing, CreatureAi.class);
            if (entityData.getComponent(thing, Unconscious.class) != null
                    || entityData.getComponent(thing, InHand.class) != null
                    || (creatureAi != null && creatureAi.getCreatureState() == CreatureState.FALLEN)
                    || !isTouching(thing, kwdFile.getCreature(creature.creatureId).getAttributes().getWidth())) {
                return;
            }

            int damage = Math.min(creatureHealth.maxHealth / 2 * 100 / Math.max(1, creatureDamagePercentage), healthLeft - wear);
            EntityController.setDamage(entityData, thing, damage);
            wear += Math.min(damage, creatureHealth.health);
            creaturesController.createController(thing).knockDown();
        }

        private boolean isTouching(EntityId thing, float thingWidth) {
            Position thingPosition = entityData.getComponent(thing, Position.class);
            if (thingPosition == null) {
                return false;
            }
            float reach = (kwdFile.getObject(ObjectsController.OBJECT_BOULDER_ID).getWidth() + thingWidth) / 2;
            float distanceX = thingPosition.position.x - position.position.x;
            float distanceY = thingPosition.position.z - position.position.z;

            return distanceX * distanceX + distanceY * distanceY < reach * reach;
        }

        private void wearOnTheGround() {
            Point tile = new Point(Math.floorDiv(x, TILE), Math.floorDiv(y, TILE));
            if (mapController.isLava(tile)) {
                wear += health / 6;
            } else if (mapController.isWater(tile)) {
                wear += health / 10;
            } else {
                IRoomController room = mapController.getRoomControllerByCoordinates(tile);
                if ((room instanceof TempleController temple && temple.isSacrificePool(tile))
                        || (room instanceof CombatPitController pit && pit.isFightingTile(tile))) {
                    wear += health / 3;
                }
            }
        }

        private void move() {
            int step = speed * speedToHealthRatio / 100 * healthLeft / health + (100 - speedToHealthRatio) * speed * 5 / 100;

            // On the middle line of its tile it looks ahead before it sets off
            if ((y & (TILE - 1)) == HALF_TILE && directionY != 0) {
                turnIfBlocked(false, Integer.signum(directionY));
            }
            if ((x & (TILE - 1)) == HALF_TILE && directionX != 0) {
                turnIfBlocked(true, Integer.signum(directionX));
            }

            int moveX = step * directionX / TILE;
            int moveY = step * directionY / TILE;
            while (moveX != 0 || moveY != 0) {

                // To the next middle line it crosses, or all the way
                int targetX = x + moveX;
                int targetY = y + moveY;
                Boolean crossedOnX = null;
                int middleX = getMiddleAhead(x, moveX);
                if (middleX != x && Integer.signum(x - middleX) != Integer.signum(targetX - middleX)) {
                    crossedOnX = true;
                    targetY = y + (middleX - x) * moveY / moveX;
                    targetX = middleX;
                }
                int middleY = getMiddleAhead(y, moveY);
                if (middleY != y && Integer.signum(y - middleY) != Integer.signum(targetY - middleY)) {
                    crossedOnX = false;
                    targetX = x + (middleY - y) * moveX / moveY;
                    targetY = middleY;
                }
                int signX = Integer.signum(moveX);
                int signY = Integer.signum(moveY);
                moveX -= targetX - x;
                moveY -= targetY - y;
                x = targetX;
                y = targetY;
                if (crossedOnX != null && turnIfBlocked(crossedOnX, crossedOnX ? signX : signY)) {
                    break;
                }
            }
        }

        private int getMiddleAhead(int coordinate, int move) {
            return Math.floorDiv(move >= 0 ? coordinate + HALF_TILE : coordinate - HALF_TILE, TILE) * TILE + HALF_TILE;
        }

        private boolean turnIfBlocked(boolean alongX, int sign) {
            int tileX = Math.floorDiv(x, TILE);
            int tileY = Math.floorDiv(y, TILE);
            if (!isBlocked(tileX + (alongX ? sign : 0), tileY + (alongX ? 0 : sign))) {
                return false;
            }

            boolean lowSideOpen = !isBlocked(tileX - (alongX ? 0 : 1), tileY - (alongX ? 1 : 0));
            boolean highSideOpen = !isBlocked(tileX + (alongX ? 0 : 1), tileY + (alongX ? 1 : 0));
            if (lowSideOpen || highSideOpen) {
                int turn = alongX ? boulder.turnY : boulder.turnX;
                if (!highSideOpen) {
                    turn = -1;
                } else if (!lowSideOpen) {
                    turn = 1;
                } else if (turn == 0) {
                    turn = Utils.getRandom().nextBoolean() ? 1 : -1;
                }
                directionX = alongX ? 0 : turn * TILE;
                directionY = alongX ? turn * TILE : 0;
            } else if (alongX) {
                directionX = -sign * TILE;
            } else {
                directionY = -sign * TILE;
            }
            wear += health / 10;

            return true;
        }

        private boolean isBlocked(int tileX, int tileY) {
            Point tile = new Point(tileX, tileY);
            if (mapController.getMapData().getTile(tile) == null || mapController.isSolid(tile)) {
                return true;
            }
            IRoomController room = mapController.getRoomControllerByCoordinates(tile);
            if (room != null && (room.getRoom().getTileConstruction() == Room.TileConstruction.HERO_GATE_2_BY_2
                    || (room instanceof PrisonController prison && prison.isCageTile(tile)))) {
                return true;
            }
            for (EntityId thing : entityPositionLookup.getEntitiesInLocation(tile)) {
                ObjectComponent objectComponent = entityData.getComponent(thing, ObjectComponent.class);
                if (objectComponent != null && !kwdFile.getObject(objectComponent.objectId).getFlags()
                        .contains(GameObject.ObjectFlag.BOULDER_CAN_ROLL_THROUGH)) {
                    return true;
                }
            }

            return false;
        }
    }

}
