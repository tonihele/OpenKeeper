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
import javax.vecmath.Vector3f;

/**
 * Container class for the Trap
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com, Toni Helenius
 * <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public class Trap implements Comparable<Trap> {

    /**
     * Trap flags
     */
    public enum TrapFlag implements IFlagEnum {

        DIE_WHEN_TRIGGERED(0x00001),
        REVEAL_WHEN_FIRED(0x00002),
        UNKNOWN1(0x00008), // always exist?
        DISARMABLE(0x00010),
        INVISIBLE(0x00020),
        TRACK_NEAREST_TARGET(0x00080),
        REQUIRE_MANA(0x00100),
        LOOP_FIRE_ANIMATION(0x00200),
        UNKNOWN2(0x00400), // always exist?
        GUARD_POST(0x00800),
        OBSTACLE(0x01000),
        DOOR_TRAP(0x02000),
        IS_GOOD(0x08000),
        FIRST_PERSON_OBSTACLE(0x10000),
        SOLID_OBSTACLE(0x20000);

        private final long flagValue;

        private TrapFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum TriggerType implements IValueEnum {

        NONE(0),
        LINE_OF_SIGHT(1),
        PRESSURE(2),
        TRIGGER(3);

        private TriggerType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    private String name;
    private ArtResource meshResource;
    private ArtResource guiIcon;
    private ArtResource editorIcon; // Data\editor\Graphics\TrapIcons.bmp
    private ArtResource flowerIcon;
    private ArtResource fireResource;
    private float height;
    private float rechargeTime;
    private float chargeTime;
    private float threatDuration;
    private int manaCostToFire; // 4 bytes?
    private float idleEffectDelay;
    private int triggerData; // 2 bytes maybe enough, but 4 bytes is safe as well
    private int shotData1; // 2 bytes maybe enough, but 4 bytes is safe as well
    private int shotData2; // 2 bytes maybe enough, but 4 bytes is safe as well
    private short unknown3[]; // 2
    private int threat; // Short
    private EnumSet<TrapFlag> flags; // 4-bytes
    private int health; // Short
    private int manaCost; // Short
    private int powerlessEffectId; // Short
    private int idleEffectId; // Short
    private int deathEffectId; // Short
    private int manufToBuild; // Short
    private int generalDescriptionStringId;
    private int strengthStringId;
    private int weaknessStringId;
    private int manaUsage; // Short
    private short unknown4[]; // 2
    private int tooltipStringId;
    private int nameStringId;
    private short editorIconId;
    private TriggerType triggerType;
    private short trapId;
    private short shotTypeId;
    private short manufCrateObjectId;
    private String soundCategory;
    private Material material;
    private short orderInEditor; // Byte
    private Vector3f shotOffset; // 4 bytes fixed point, x - y - z
    private float shotDelay; // int
    private int healthGain; // Short

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

    public ArtResource getGuiIcon() {
        return guiIcon;
    }

    protected void setGuiIcon(ArtResource guiIcon) {
        this.guiIcon = guiIcon;
    }

    public ArtResource getEditorIcon() {
        return editorIcon;
    }

    protected void setEditorIcon(ArtResource editorIcon) {
        this.editorIcon = editorIcon;
    }

    public ArtResource getFlowerIcon() {
        return flowerIcon;
    }

    protected void setFlowerIcon(ArtResource flowerIcon) {
        this.flowerIcon = flowerIcon;
    }

    public ArtResource getFireResource() {
        return fireResource;
    }

    protected void setFireResource(ArtResource fireResource) {
        this.fireResource = fireResource;
    }

    public float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    public float getRechargeTime() {
        return rechargeTime;
    }

    protected void setRechargeTime(float rechargeTime) {
        this.rechargeTime = rechargeTime;
    }

    public float getChargeTime() {
        return chargeTime;
    }

    protected void setChargeTime(float chargeTime) {
        this.chargeTime = chargeTime;
    }

    public float getThreatDuration() {
        return threatDuration;
    }

    protected void setThreatDuration(float threatDuration) {
        this.threatDuration = threatDuration;
    }

    public int getManaCostToFire() {
        return manaCostToFire;
    }

    protected void setManaCostToFire(int manaCostToFire) {
        this.manaCostToFire = manaCostToFire;
    }

    public float getIdleEffectDelay() {
        return idleEffectDelay;
    }

    protected void setIdleEffectDelay(float idleEffectDelay) {
        this.idleEffectDelay = idleEffectDelay;
    }

    public int getTriggerData() {
        return triggerData;
    }

    protected void setTriggerData(int triggerData) {
        this.triggerData = triggerData;
    }

    public int getShotData1() {
        return shotData1;
    }

    protected void setShotData1(int shotData1) {
        this.shotData1 = shotData1;
    }

    public int getShotData2() {
        return shotData2;
    }

    protected void setShotData2(int shotData2) {
        this.shotData2 = shotData2;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }

    public int getThreat() {
        return threat;
    }

    protected void setThreat(int threat) {
        this.threat = threat;
    }

    public EnumSet<TrapFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<TrapFlag> flags) {
        this.flags = flags;
    }

    public int getHealth() {
        return health;
    }

    protected void setHealth(int health) {
        this.health = health;
    }

    public int getManaCost() {
        return manaCost;
    }

    protected void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public int getPowerlessEffectId() {
        return powerlessEffectId;
    }

    protected void setPowerlessEffectId(int powerlessEffectId) {
        this.powerlessEffectId = powerlessEffectId;
    }

    public int getIdleEffectId() {
        return idleEffectId;
    }

    protected void setIdleEffectId(int idleEffectId) {
        this.idleEffectId = idleEffectId;
    }

    public int getDeathEffectId() {
        return deathEffectId;
    }

    protected void setDeathEffectId(int deathEffectId) {
        this.deathEffectId = deathEffectId;
    }

    public int getManufToBuild() {
        return manufToBuild;
    }

    protected void setManufToBuild(int manufToBuild) {
        this.manufToBuild = manufToBuild;
    }

    public int getGeneralDescriptionStringId() {
        return generalDescriptionStringId;
    }

    protected void setGeneralDescriptionStringId(int generalDescriptionStringId) {
        this.generalDescriptionStringId = generalDescriptionStringId;
    }

    public int getStrengthStringId() {
        return strengthStringId;
    }

    protected void setStrengthStringId(int strengthStringId) {
        this.strengthStringId = strengthStringId;
    }

    public int getWeaknessStringId() {
        return weaknessStringId;
    }

    protected void setWeaknessStringId(int weaknessStringId) {
        this.weaknessStringId = weaknessStringId;
    }

    public int getManaUsage() {
        return manaUsage;
    }

    protected void setManaUsage(int manaUsage) {
        this.manaUsage = manaUsage;
    }

    public short[] getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(short[] unknown4) {
        this.unknown4 = unknown4;
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

    public short getEditorIconId() {
        return editorIconId;
    }

    protected void setEditorIconId(short editorIconId) {
        this.editorIconId = editorIconId;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    protected void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public short getTrapId() {
        return trapId;
    }

    protected void setTrapId(short trapId) {
        this.trapId = trapId;
    }

    public short getShotTypeId() {
        return shotTypeId;
    }

    protected void setShotTypeId(short shotTypeId) {
        this.shotTypeId = shotTypeId;
    }

    public short getManufCrateObjectId() {
        return manufCrateObjectId;
    }

    protected void setManufCrateObjectId(short manufCrateObjectId) {
        this.manufCrateObjectId = manufCrateObjectId;
    }

    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    public Material getMaterial() {
        return material;
    }

    protected void setMaterial(Material material) {
        this.material = material;
    }

    public short getOrderInEditor() {
        return orderInEditor;
    }

    protected void setOrderInEditor(short orderInEditor) {
        this.orderInEditor = orderInEditor;
    }

    public Vector3f getShotOffset() {
        return shotOffset;
    }

    protected void setShotOffset(float x, float y, float z) {
        this.shotOffset = new Vector3f(x, y, z);
    }

    public float getShotDelay() {
        return shotDelay;
    }

    protected void setShotDelay(float shotDelay) {
        this.shotDelay = shotDelay;
    }

    public int getHealthGain() {
        return healthGain;
    }

    protected void setHealthGain(int healthGain) {
        this.healthGain = healthGain;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Trap o) {
        return Short.compare(orderInEditor, o.orderInEditor);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.trapId;
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
        final Trap other = (Trap) obj;
        if (this.trapId != other.trapId) {
            return false;
        }
        return true;
    }
}
