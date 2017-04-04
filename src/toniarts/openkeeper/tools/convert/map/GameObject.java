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

import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import java.util.EnumSet;
import java.util.List;

/**
 * Container class for Objects.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameObject implements Comparable<GameObject> {

    /**
     * Object flags
     */
    public enum ObjectFlag implements IFlagEnum {

        DIE_OVER_TIME(0x0000001),
        DIE_OVER_TIME_IF_NOT_IN_ROOM(0x0000002),
        OBJECT_TYPE_SPECIAL(0x0000004),
        OBJECT_TYPE_SPELL_BOOK(0x0000008),
        OBJECT_TYPE_CRATE(0x0000010),
        OBJECT_TYPE_LAIR(0x0000020),
        OBJECT_TYPE_GOLD(0x0000040),
        OBJECT_TYPE_FOOD(0x0000080),
        CAN_BE_PICKED_UP(0x0000100),
        CAN_BE_SLAPPED(0x0000200),
        DIE_WHEN_SLAPPED(0x0000400),
        OBJECT_TYPE_LEVEL_GEM(0x0001000),
        CAN_BE_DROPPED_ON_ANY_LAND(0x0002000),
        OBSTACLE(0x0004000),
        BOUNCE(0x0008000),
        BOULDER_CAN_ROLL_THROUGH(0x0010000),
        BOULDER_DESTROYS(0x0020000),
        PILLAR(0x0040000),
        DOOR_KEY(0x0100000),
        DAMAGEABLE(0x0200000),
        HIGHLIGHTABLE(0x0400000),
        PLACEABLE(0x0800000),
        FIRST_PERSON_OBSTACLE(0x1000000),
        SOLID_OBSTACLE(0x2000000),
        CAST_SHADOWS(0x4000000);
        private final long flagValue;

        private ObjectFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum State implements IValueEnum {

        NONE(0),
        BEING_DROPPED(1),
        BEING_SLAPPED(2),
        PRISON_ARRIVE(3),
        TEMPLE_ARRIVE(4),
        GOLD_ARRIVE(5),
        HATCHERY_ARRIVE(6),
        LAIR_ARRIVE(7),
        PECKING(8),
        MOVE_TO(9),
        WANDER(10),
        WANDER_WITHIN_PLAYER(11),
        CREATE_CALL_TO_ARMS(12),
        CALL_TO_ARMS(13),
        RESEND_CALL_TO_ARMS(14),
        DESTROY_CALL_TO_ARMS(15),
        DO_NOTHING(16),
        BOULDER(17),
        SPINNING(18),
        DESTROY_CALL_TO_ARMS_COPY(19),
        DESTROY_CALL_TO_ARMS_COPY_INIT(20),
        ROTATE(21),
        BEING_PICKED_UP(22),
        DODGE(23),
        FREE_CHICKEN(24),
        ESCAPE_FROM_FIGHT(25),
        ACTIVATE_ANIM(26),
        WORKSHOP_ARRIVE(27),
        KICKED(28),
        SHAKE(29),
        TORTURE_WHEEL_SPIN(30),
        GOLD_PROCESS(31),
        HATCHING(32),
        FLARE(33),
        TOMBSTONE_SCALE_UP(34),
        TOMBSTONE_SCALE_DOWN(35),
        SPECIAL(36),
        HEART_CONSTRUCTION(37),
        DESTRUCTOR(38),
        MANA_VAULT(39),
        TORTURE_COALS(40), // Coals, not a typo?
        THREAT_BURN(41),
        IDLE_EFFECT(42),
        FRONTEND_LEVEL_GEM(43),
        PRISON_DOOR_OPEN(44),
        PRISON_DOOR_CLOSE(45),
        OPEN_PRISON_DOOR_BAR(46),
        CLOSE_PRISON_DOOR_BAR(47),
        PLAY_WIN_ANIM_INDICATOR_TO_SMILES(48),
        PLAY_WIN_ANIM_INDICATOR_TO_MONEY(49),
        PORTAL_GEM(50),
        MY_PET_DUNGEON_LEVEL_INDICATOR(51),
        ENTRANCE_ARRIVE(52),
        SPELL_BOOK_IDLE(53),
        MPD_LEVEL_UP(54), // My Pet Dungeon
        MPD_LEVEL_DOWN(55), // My Pet Dungeon
        JACK_IN_THE_BOX(56),
        CRYPT_DANCER(57),
        CRYPT_SPECIAL_DANCER(58),
        GEM_CHALLENGE_JEWEL(59),
        PLINTH(60),
        SPECIAL_PLINTH(61),
        DROP_OFF_POINT(62),
        GEM_EXIT_POINT(63);

        private State(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
//    struct ObjectBlock {
//        char name[32]; /* 0 */
//        ArtResource kMeshResource; /* 20 */
//        ArtResource kGuiIconResource; /* 74 */
//        ArtResource kInHandIconResource; /* c8 */
//        ArtResource kInHandMeshResource; /* 11c */
//        ArtResource kUnknownResource; /* 170 */
//        ArtResource kAdditionalResources[4]; /* 1c4 */
//        LightBlock light; /* 314 */
//        uint32_t width; /* 32c fixed 0x1000 */
//        uint32_t height; /* 330 */
//        uint32_t mass; /* 334 */
//        uint32_t unk1; /* 338 */
//        uint32_t unk2; /* 33c */
//        uint8_t material; /* 340 */
//        uint8_t unk3[3]; /* 341 */
//        uint32_t flags; /* 344 */
//        uint16_t hp; /* 348 */
//        uint16_t unk4;
//        uint16_t x34c;
//        uint16_t manaValue;
//        uint16_t x350;
//        uint16_t x352; /* 34a */
//        uint16_t slap_effect; /* 354 */
//        uint16_t death_effect; /* 356 */
//        uint16_t unk5; /* 358 */
//        uint8_t id; /* 35a */
//        uint8_t unk6; /* 35b */
//        uint8_t room_capacity; /* 35c */
//        uint8_t unk7; /* 35d */
//        char sound_category[32]; /* 35e */
//        };
    private String name; // 0
    private ArtResource meshResource; // 20
    private ArtResource guiIconResource; // 74
    private ArtResource inHandIconResource; // c8
    private ArtResource inHandMeshResource; // 11c
    private ArtResource unknownResource; // gui\spells\call_to_arms in Call To Arms only
    private List<ArtResource> additionalResources; // 1c4
    private Light light; // 314
    private float width; // 32c fixed 0x1000
    private float height; // 330
    private float mass; // 334
    private float speed; // 338
    private float airFriction; // 33c
    private Material material; // 340
    private short unknown3[];  // always 49,145,0
    private EnumSet<ObjectFlag> flags; // 344
    private int hp; // 348
    private int maxAngle;
    private int x34c;  // always 0
    private int manaValue;
    private int tooltipStringId;
    private int nameStringId; // 34a
    private int slapEffectId; // 354
    private int deathEffectId; // 356
    private int miscEffectId; // 358
    private short objectId; // 35a
    private State startState; // 35b
    private short roomCapacity; // 35c
    private short pickUpPriority; // 35d
    private String soundCategory; // 35e

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource getMeshResource() {
        return meshResource;
    }

    protected void setMeshResource(ArtResource meshResource) {
        this.meshResource = meshResource;
    }

    public ArtResource getGuiIconResource() {
        return guiIconResource;
    }

    protected void setGuiIconResource(ArtResource guiIconResource) {
        this.guiIconResource = guiIconResource;
    }

    public ArtResource getInHandIconResource() {
        return inHandIconResource;
    }

    protected void setInHandIconResource(ArtResource inHandIconResource) {
        this.inHandIconResource = inHandIconResource;
    }

    public ArtResource getInHandMeshResource() {
        return inHandMeshResource;
    }

    protected void setInHandMeshResource(ArtResource inHandMeshResource) {
        this.inHandMeshResource = inHandMeshResource;
    }

    public ArtResource getUnknownResource() {
        return unknownResource;
    }

    protected void setkUnknownResource(ArtResource unknownResource) {
        this.unknownResource = unknownResource;
    }

    public List<ArtResource> getAdditionalResources() {
        return additionalResources;
    }

    protected void setAdditionalResources(List<ArtResource> additionalResources) {
        this.additionalResources = additionalResources;
    }

    public Light getLight() {
        return light;
    }

    protected void setLight(Light light) {
        this.light = light;
    }

    public float getWidth() {
        return width;
    }

    protected void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    public float getMass() {
        return mass;
    }

    protected void setMass(float mass) {
        this.mass = mass;
    }

    public float getSpeed() {
        return speed;
    }

    protected void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getAirFriction() {
        return airFriction;
    }

    protected void setAirFriction(float airFriction) {
        this.airFriction = airFriction;
    }

    public Material getMaterial() {
        return material;
    }

    protected void setMaterial(Material material) {
        this.material = material;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }

    public EnumSet<ObjectFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<ObjectFlag> flags) {
        this.flags = flags;
    }

    public int getHp() {
        return hp;
    }

    protected void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxAngle() {
        return maxAngle;
    }

    protected void setMaxAngle(int maxAngle) {
        this.maxAngle = maxAngle;
    }

    public int getX34c() {
        return x34c;
    }

    protected void setX34c(int x34c) {
        this.x34c = x34c;
    }

    public int getManaValue() {
        return manaValue;
    }

    protected void setManaValue(int manaValue) {
        this.manaValue = manaValue;
    }

    public int getTooltipStringId() {
        return tooltipStringId;
    }

    protected void setTooltipStringId(int tooltipStringId) {
        this.tooltipStringId = tooltipStringId;
    }

    public int getNameStringId() {
        return nameStringId;
    }

    protected void setNameStringId(int nameStringId) {
        this.nameStringId = nameStringId;
    }

    public int getSlapEffectId() {
        return slapEffectId;
    }

    protected void setSlapEffectId(int slapEffectId) {
        this.slapEffectId = slapEffectId;
    }

    public int getDeathEffectId() {
        return deathEffectId;
    }

    protected void setDeathEffectId(int deathEffectId) {
        this.deathEffectId = deathEffectId;
    }

    public int getMiscEffectId() {
        return miscEffectId;
    }

    protected void setMiscEffectId(int miscEffectId) {
        this.miscEffectId = miscEffectId;
    }

    public short getObjectId() {
        return objectId;
    }

    protected void setObjectId(short objectId) {
        this.objectId = objectId;
    }

    public State getStartState() {
        return startState;
    }

    protected void setStartState(State startState) {
        this.startState = startState;
    }

    public short getRoomCapacity() {
        return roomCapacity;
    }

    protected void setRoomCapacity(short roomCapacity) {
        this.roomCapacity = roomCapacity;
    }

    public short getPickUpPriority() {
        return pickUpPriority;
    }

    protected void setPickUpPriority(short pickUpPriority) {
        this.pickUpPriority = pickUpPriority;
    }

    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(GameObject o) {
        return Short.compare(objectId, o.objectId);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + this.objectId;
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
        final GameObject other = (GameObject) obj;
        if (this.objectId != other.objectId) {
            return false;
        }
        return true;
    }
}
