/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import java.util.List;
import javax.vecmath.Vector3f;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.map.Thing.HeroParty.Objective;

/**
 * Container class for *Things.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Thing {

//    struct ActionPointBlock {
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int32_t x0c;
//        int32_t x10;
//        int16_t x14;
//        uint8_t id; /* 16 */
//        uint8_t x17;
//        char name[32]; /* 18 */
//        };
    public static class ActionPoint extends Thing implements Comparable<ActionPoint> {

        /**
         * ActionPoint flags
         */
        public enum ActionPointFlag implements IFlagEnum {

            HERO_LAIR(0x001),
            UNKNOWN(0x004),
            REVEAL_THROUGH_FOG_OF_WAR(0x010),
            TOOL_BOX(0x020),
            IGNORE_SOLID(0x040);
            private final long flagValue;

            private ActionPointFlag(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }
        };
        private int startX; // 0-based coordinate
        private int startY; // 0-based coordinate
        private int endX; // 0-based coordinate
        private int endY; // 0-based coordinate
        private int waitDelay;
        private EnumSet<ActionPointFlag> flags;
        private short id; // 16
        private short nextWaypointId; // Is always another ActionPoint?
        private String name; // 18 <- Always just shit

        public int getStartX() {
            return startX;
        }

        protected void setStartX(int startX) {
            this.startX = startX;
        }

        public int getStartY() {
            return startY;
        }

        protected void setStartY(int startY) {
            this.startY = startY;
        }

        public int getEndX() {
            return endX;
        }

        protected void setEndX(int endX) {
            this.endX = endX;
        }

        public int getEndY() {
            return endY;
        }

        protected void setEndY(int endY) {
            this.endY = endY;
        }

        public int getWaitDelay() {
            return waitDelay;
        }

        protected void setWaitDelay(int waitDelay) {
            this.waitDelay = waitDelay;
        }

        public EnumSet<ActionPointFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<ActionPointFlag> flags) {
            this.flags = flags;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public short getNextWaypointId() {
            return nextWaypointId;
        }

        protected void setNextWaypointId(short nextWaypointId) {
            this.nextWaypointId = nextWaypointId;
        }

        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "[ID " + id + "] Action Point " + id + " [" + (startX + 1) + "," + (startY + 1) + "] - [" + (endX + 1) + "," + (endY + 1) + "]";
        }

        @Override
        public int compareTo(ActionPoint o) {
            return Short.compare(id, o.id);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ActionPoint other = (ActionPoint) obj;
            if (this.id != other.id) {
                return false;
            }
            return true;
        }
    }

    public abstract static class Creature extends Thing {

        /**
         * Creature flags
         */
        public enum CreatureFlag implements IFlagEnum {
            // ELITE contains in creatureId

            WILL_FIGHT(0x001),
            LEADER(0x002),
            FOLLOWER(0x004),
            WILL_BE_ATTACKED(0x008),
            RETURN_TO_HERO_LAIR(0x010),
            FREE_FRIENDS_ON_JAIL_BREAK(0x020),
            ACT_AS_DROPPED(0x040),
            START_AS_DYING(0x080);
            private final long flagValue;

            private CreatureFlag(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }
        };

        /**
         * Creature flags
         */
        public enum CreatureFlag2 implements IFlagEnum {

            DESTROY_ROOMS(0x001),
            I_AM_A_TOOL(0x002),
            DIES_INSTANTLY(0x004),
            I_AM_A_MERCENARY(0x008);
            private final long flagValue;

            private CreatureFlag2(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }
        };
        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int posZ; // ???
        private int goldHeld; // Percent
        private short creatureId; // 1a

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getPosZ() {
            return posZ;
        }

        protected void setPosZ(int posZ) {
            this.posZ = posZ;
        }

        public int getGoldHeld() {
            return goldHeld;
        }

        protected void setGoldHeld(int goldHeld) {
            this.goldHeld = goldHeld;
        }

        public short getCreatureId() {
            return creatureId;
        }

        protected void setCreatureId(short creatureId) {
            this.creatureId = creatureId;
        }
    }

    //    struct Thing03Block {
//        int32_t pos[3];
//        uint16_t x0c;
//        uint8_t x0e; /* level */
//        uint8_t x0f; /* likely flags */
//        int32_t x10;
//        int32_t x14;
//        uint16_t x18;
//        uint8_t id; /* 1a */
//        uint8_t x1b; /* player id */
//        };
    public static class KeeperCreature extends Creature {

        private short level;
        private EnumSet<Creature.CreatureFlag> flags; // Short, likely flags
        private int initialHealth; // Percent
        private int objectiveTargetActionPointId;
        private int x14;
        private int triggerId;
        private short playerId; // player id

        public short getLevel() {
            return level;
        }

        protected void setLevel(short level) {
            this.level = level;
        }

        public EnumSet<CreatureFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CreatureFlag> flags) {
            this.flags = flags;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        public int getObjectiveTargetActionPointId() {
            return objectiveTargetActionPointId;
        }

        protected void setObjectiveTargetActionPointId(int objectiveTargetActionPointId) {
            this.objectiveTargetActionPointId = objectiveTargetActionPointId;
        }

        public int getX14() {
            return x14;
        }

        protected void setX14(int x14) {
            this.x14 = x14;
        }

        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }
    }

//    struct Thing11Block {
//        Thing11Block()
//        : x00(0), x04(0), x08(0), x0c(0), x0e(0), x0f(0), x10(0), x12(0), x13(0) { }
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int16_t x0c;
//        uint8_t x0e;
//        uint8_t x0f; /* maybe padding */
//        int16_t x10;
//        uint8_t x12;
//        uint8_t x13;
//        };
    public static class Room extends Thing {

        public enum RoomType implements IValueEnum {

            PORTAL(12),
            DUNGEON_HEART(14),
            HERO_GATE_2X2(33),
            HERO_GATE_FRONT_END(34), // ??, in the 3D menu
            HERO_GATE_3X1(37),
            HERO_PORTAL(40);

            private RoomType(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }

        public enum Direction implements IValueEnum {

            NORTH(0),
            EAST(2),
            SOUTH(4),
            WEST(6);

            private Direction(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }
        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int x08;
        private int x0c;
        private Direction direction; // Hero gates have direction
        private short x0f; // maybe padding
        private int initialHealth; // Percentage
        private RoomType roomType; // 14 dh, 12 portal, 33 hero gate 2x2, 37 hero gate 3x1, 40 hero portal
        private short playerId;

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public Direction getDirection() {
            return direction;
        }

        protected void setDirection(Direction direction) {
            this.direction = direction;
        }

        public short getX0f() {
            return x0f;
        }

        protected void setX0f(short x0f) {
            this.x0f = x0f;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        public RoomType getRoomType() {
            return roomType;
        }

        protected void setRoomType(RoomType roomType) {
            this.roomType = roomType;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }

        @Override
        public String toString() {
            return roomType + " [" + (posX + 1) + "," + (posY + 1) + "]";
        }
    }
//    struct Thing12Block {
//        Position3D x00;
//        Position3D x0c;
//        Position3D x18;
//        int32_t x24;
//        int32_t x28;
//        int32_t x2c;
//        int32_t x30;
//        int32_t x34;
//        int32_t x38;
//        int32_t x3c;
//        int32_t x40;
//        int32_t x44;
//        int32_t x48; /* flags */
//        uint16_t x4c;
//        uint16_t x4e;
//        uint16_t x50;
//        uint8_t x52;
//        };

    // FIXME: these were camera positions, but found in variables???
    // Contains in GlobalVariables.kwd only
    public static class Camera extends Thing {
        
        public enum CameraFlag implements IFlagEnum {
            DISABLE_CHANGE(0x80), // Never used. camera not enter and leave possession.
            DISABLE_MOVE(0x40),
            UNKNOWN_20(0x20),
            UNKNOWN_10(0x10),
            DISABLE_ZOOM(0x08),
            DISABLE_PITCH(0x04),
            DISABLE_ROLL(0x02),
            DISABLE_YAW(0x01);
            
            private CameraFlag(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }
            
            private final long flagValue;
        };
        
        private Vector3f x00;
        private Vector3f x0c;
        private Vector3f x18;
        private float x24; // fog from
        private float x28; // fog or distance of view
        private float x2c; // fog to
        private float heightDefault; // in game default zoom
        private float heightMin; // in game min zoom
        private float heightMax; // in game max zoom
        private float x3c; // frustumRightTop maybe
        private float x40; // frustumLeftBottom maybe
        private float near; // change field of view
        private EnumSet<CameraFlag> flags; // 
        private int x4c; // maybe angle yaw. always 0
        private int angleRoll; // rotate camera around direction vector.
        private int anglePitch; // rotate camera around Left vector. 512 = front.
        
        /* 2 - possession camera
         * 3 - game camera
         * other never used ?
         */
        private short id;

        public Vector3f getX00() {
            return x00;
        }

        protected void setX00(Vector3f x00) {
            this.x00 = x00;
        }

        public Vector3f getX0c() {
            return x0c;
        }

        protected void setX0c(Vector3f x0c) {
            this.x0c = x0c;
        }

        public Vector3f getX18() {
            return x18;
        }

        protected void setX18(Vector3f x18) {
            this.x18 = x18;
        }

        public float getX24() {
            return x24;
        }

        protected void setX24(float x24) {
            this.x24 = x24;
        }

        public float getX28() {
            return x28;
        }

        protected void setX28(float x28) {
            this.x28 = x28;
        }

        public float getX2c() {
            return x2c;
        }

        protected void setX2c(float x2c) {
            this.x2c = x2c;
        }

        public float getHeightDefault() {
            return heightDefault;
        }

        protected void setHeightDefault(float height) {
            this.heightDefault = height;
        }

        public float getHeightMin() {
            return heightMin;
        }

        protected void setHeightMin(float height) {
            this.heightMin = height;
        }

        public float getHeightMax() {
            return heightMax;
        }

        protected void setHeightMax(float height) {
            this.heightMax = height;
        }

        public float getX3c() {
            return x3c;
        }

        protected void setX3c(float x3c) {
            this.x3c = x3c;
        }

        public float getX40() {
            return x40;
        }

        protected void setX40(float x40) {
            this.x40 = x40;
        }

        public float getNear() {
            return near;
        }

        protected void setNear(float near) {
            this.near = near;
        }

        public EnumSet<CameraFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CameraFlag> flags) {
            this.flags = flags;
        }

        public int getX4c() {
            return x4c;
        }

        protected void setX4c(int x4c) {
            this.x4c = x4c;
        }

        public int getAngleRoll() {
            return angleRoll;
        }

        protected void setAngleRoll(int angle) {
            this.angleRoll = angle;
        }

        public int getAnglePitch() {
            return anglePitch;
        }

        protected void setAnglePitch(int angle) {
            this.anglePitch = angle;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Camera{" + "x00=" + x00 + ", x0c=" + x0c + ", x18=" + x18 + ", x24=" + x24 + ", x28=" + x28 
                    + ", x2c=" + x2c + ", heightNormal=" + heightDefault + ", heightMin=" + heightMin + 
                    ", heightMax=" + heightMax + ", x3c=" + x3c + ", x40=" 
                    + x40 + ", near=" + near + ", flags=" + flags + ", x4c=" + x4c + 
                    ", angleRoll=" + angleRoll + ", anglePitch=" + anglePitch + ", id=" + id + '}';
        }
    }

//struct Thing08Block { /* hero party */
//    char name[32];
//    int16_t x20;
//    uint8_t x22;
//    int32_t x23; /* these two are unreferenced... */
//    int32_t x27;
//    HeroPartyData x2b[16];
//    };
    public static class HeroParty extends Thing implements Comparable<HeroParty> {

        public enum Objective implements IValueEnum {

            NONE(0),
            DESTROY_ROOMS(11),
            DESTROY_WALLS(12),
            STEAL_GOLD(13),
            STEAL_SPELLS(14),
            STEAL_MAFUFACTURE_CRATES(17),
            KILL_CREATURES(18),
            KILL_PLAYER(19),
            WAIT(22),
            SEND_TO_ACTION_POINT(23),
            JAIL_BREAK(27);

            private Objective(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }
        private String name; // Always shit
        private int triggerId;
        private short id; // I assume, autoincremental 0-based
        private int x23; // these two are unreferenced...
        private int x27;
        private List<GoodCreature> heroPartyMembers; // 16 at max

        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }

        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public int getX23() {
            return x23;
        }

        protected void setX23(int x23) {
            this.x23 = x23;
        }

        public int getX27() {
            return x27;
        }

        protected void setX27(int x27) {
            this.x27 = x27;
        }

        public List<GoodCreature> getHeroPartyMembers() {
            return heroPartyMembers;
        }

        protected void setHeroPartyMembers(List<GoodCreature> heroPartyMembers) {
            this.heroPartyMembers = heroPartyMembers;
        }

        @Override
        public String toString() {
            return "Hero Party " + (id + 1);
        }

        @Override
        public int compareTo(HeroParty o) {
            return Short.compare(id, o.id);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final HeroParty other = (HeroParty) obj;
            if (this.id != other.id) {
                return false;
            }
            return true;
        }

//        struct HeroPartyData {
//            int32_t x00;
//            int32_t x04;
//            int32_t x08;
//            int16_t goldHeld;
//            uint8_t x0e; /* level */
//            uint8_t x0f;
//            int32_t x10;
//            int32_t initialHealth;
//            int16_t x18; /* trigger root */
//            uint8_t x1a;
//            uint8_t x1b;
//            uint8_t x1c;
//            uint8_t x1d;
//            uint8_t x1e;
//            uint8_t x1f;
//            };
        /**
         * Represents the party members
         */
        public class HeroPartyData {

            private int x00;
            private int x04;
            private int x08;
            private int goldHeld;
            private short level; // level
            private short x0f;
            private int objectiveTargetActionPointId;
            private int initialHealth;
            private int triggerId; // trigger root
            private short objectiveTargetPlayerId;
            private Objective objective;
            private short creatureId;
            private short x1d;
            private short x1e;
            private short x1f;

            public int getX00() {
                return x00;
            }

            protected void setX00(int x00) {
                this.x00 = x00;
            }

            public int getX04() {
                return x04;
            }

            protected void setX04(int x04) {
                this.x04 = x04;
            }

            public int getX08() {
                return x08;
            }

            protected void setX08(int x08) {
                this.x08 = x08;
            }

            public int getGoldHeld() {
                return goldHeld;
            }

            protected void setGoldHeld(int goldHeld) {
                this.goldHeld = goldHeld;
            }

            public short getLevel() {
                return level;
            }

            protected void setLevel(short level) {
                this.level = level;
            }

            public short getX0f() {
                return x0f;
            }

            protected void setX0f(short x0f) {
                this.x0f = x0f;
            }

            public int getObjectiveTargetActionPointId() {
                return objectiveTargetActionPointId;
            }

            protected void setObjectiveTargetActionPointId(int objectiveTargetActionPointId) {
                this.objectiveTargetActionPointId = objectiveTargetActionPointId;
            }

            public int getInitialHealth() {
                return initialHealth;
            }

            protected void setInitialHealth(int initialHealth) {
                this.initialHealth = initialHealth;
            }

            public int getTriggerId() {
                return triggerId;
            }

            protected void setTriggerId(int triggerId) {
                this.triggerId = triggerId;
            }

            public short getObjectiveTargetPlayerId() {
                return objectiveTargetPlayerId;
            }

            protected void setObjectiveTargetPlayerId(short objectiveTargetPlayerId) {
                this.objectiveTargetPlayerId = objectiveTargetPlayerId;
            }

            public Objective getObjective() {
                return objective;
            }

            protected void setObjective(Objective objective) {
                this.objective = objective;
            }

            public short getCreatureId() {
                return creatureId;
            }

            protected void setCreatureId(short creatureId) {
                this.creatureId = creatureId;
            }

            public short getX1d() {
                return x1d;
            }

            protected void setX1d(short x1d) {
                this.x1d = x1d;
            }

            public short getX1e() {
                return x1e;
            }

            protected void setX1e(short x1e) {
                this.x1e = x1e;
            }

            public short getX1f() {
                return x1f;
            }

            protected void setX1f(short x1f) {
                this.x1f = x1f;
            }
        }
    }

//    struct Thing10Block {
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int32_t x0c;
//        int16_t x10;
//        int16_t x12;
//        int16_t x14[4];
//        uint8_t x1c;
//        uint8_t x1d;
//        uint8_t pad[6];
//        };
    public static class EffectGenerator extends Thing {

        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int x08;
        private int x0c;
        private int x10;
        private int x12;
        private List<Integer> effectIds; // 4
        private short frequency;
        private short id;
        private short pad[];

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public int getX10() {
            return x10;
        }

        protected void setX10(int x10) {
            this.x10 = x10;
        }

        public int getX12() {
            return x12;
        }

        protected void setX12(int x12) {
            this.x12 = x12;
        }

        public List<Integer> getEffectIds() {
            return effectIds;
        }

        protected void setEffectIds(List<Integer> effectIds) {
            this.effectIds = effectIds;
        }

        public short getFrequency() {
            return frequency;
        }

        protected void setFrequency(short frequency) {
            this.frequency = frequency;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public short[] getPad() {
            return pad;
        }

        protected void setPad(short[] pad) {
            this.pad = pad;
        }
    }

    public static class DeadBody extends Creature {

        private short playerId;

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }
    }

    public static class NeutralCreature extends Creature {

        private short level; // level
        private EnumSet<CreatureFlag> flags; // Short, likely flags
        private int initialHealth; // Percent
        private int triggerId;
        private short unknown1;

        public short getLevel() {
            return level;
        }

        protected void setLevel(short level) {
            this.level = level;
        }

        public EnumSet<CreatureFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CreatureFlag> flags) {
            this.flags = flags;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(short unknown1) {
            this.unknown1 = unknown1;
        }
    }

    public static class Door extends Thing {

        public enum DoorFlag implements IValueEnum {

            NONE(0),
            LOCKED(1),
            BLUEPRINT(2);

            private DoorFlag(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }
        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int unknown1; // 4
        private int triggerId;
        private short doorId;
        private short playerId;
        private DoorFlag flag;
        private short unknown2[];  // 3

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(int unknown1) {
            this.unknown1 = unknown1;
        }

        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getDoorId() {
            return doorId;
        }

        protected void setDoorId(short doorId) {
            this.doorId = doorId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }

        public DoorFlag getFlag() {
            return flag;
        }

        protected void setFlag(DoorFlag flag) {
            this.flag = flag;
        }

        public short[] getUnknown2() {
            return unknown2;
        }

        protected void setUnknown2(short[] unknown2) {
            this.unknown2 = unknown2;
        }
    }

    public static class Object extends Thing {

        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private short unknown1[]; // 12
        private int triggerId;
        private short objectId;
        private short playerId;

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public short[] getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(short[] unknown1) {
            this.unknown1 = unknown1;
        }

        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getObjectId() {
            return objectId;
        }

        protected void setObjectId(short objectId) {
            this.objectId = objectId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }
    }

    public static class Trap extends Thing {

        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int unknown1; // 4
        private short numberOfShots;
        private short trapId;
        private short playerId;
        private short unknown2; // 1

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(int unknown1) {
            this.unknown1 = unknown1;
        }

        public short getNumberOfShots() {
            return numberOfShots;
        }

        protected void setNumberOfShots(short numberOfShots) {
            this.numberOfShots = numberOfShots;
        }

        public short getTrapId() {
            return trapId;
        }

        protected void setTrapId(short trapId) {
            this.trapId = trapId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }

        public short getUnknown2() {
            return unknown2;
        }

        protected void setUnknown2(short unknown2) {
            this.unknown2 = unknown2;
        }
    }

    public static class GoodCreature extends Creature {

        private short level; // level
        private EnumSet<CreatureFlag> flags; // Short, likely flags
        private int objectiveTargetActionPointId;
        private int initialHealth; // Percent
        private int triggerId;
        private short objectiveTargetPlayerId; // target objective
        private HeroParty.Objective objective;
        private short unknown1[];
        private EnumSet<Thing.Creature.CreatureFlag2> flags2;

        public short getLevel() {
            return level;
        }

        protected void setLevel(short level) {
            this.level = level;
        }

        public EnumSet<CreatureFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CreatureFlag> flags) {
            this.flags = flags;
        }

        public int getObjectiveTargetActionPointId() {
            return objectiveTargetActionPointId;
        }

        protected void setObjectiveTargetActionPointId(int objectiveTargetActionPointId) {
            this.objectiveTargetActionPointId = objectiveTargetActionPointId;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getObjectiveTargetPlayerId() {
            return objectiveTargetPlayerId;
        }

        protected void setObjectiveTargetPlayerId(short objectiveTargetPlayerId) {
            this.objectiveTargetPlayerId = objectiveTargetPlayerId;
        }

        public Objective getObjective() {
            return objective;
        }

        protected void setObjective(Objective objective) {
            this.objective = objective;
        }

        public short[] getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(short unknown1[]) {
            this.unknown1 = unknown1;
        }

        public EnumSet<CreatureFlag2> getFlags2() {
            return flags2;
        }

        protected void setFlags2(EnumSet<CreatureFlag2> flags2) {
            this.flags2 = flags2;
        }
    }
}
