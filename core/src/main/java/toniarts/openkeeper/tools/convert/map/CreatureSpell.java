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

/**
 * Container class for CreatureSpells.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureSpell implements Comparable<CreatureSpell> {

    /**
     * Creature spell flags
     */
    public enum CreatureSpellFlag implements IFlagEnum {

        IS_ATTACKING(0x0001),
        IS_DEFENSIVE(0x0002),
        CREATURE(0x0004),
        CAST_ONLY_ON_CREATURES(0x0008),
        IMP_TELEPORT(0x0020),
        IMP_HASTE(0x0040),
        CAST_ON_SELF(0x0080),
        CAST_ON_OTHERS(0x0100);
        private final long flagValue;

        private CreatureSpellFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum AlternativeShot implements IValueEnum {

        NONE(0),
        HASTE(1),
        HEAL(2),
        ARMOUR(3),
        INVULNERABLE(4),
        INVISIBLE(5);

        private AlternativeShot(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
//    struct CreatureSpellBlock {
//        char name[32];
//        uint8_t data[234];
//        };
    private String name;
    private ArtResource editorIcon; //??
    private ArtResource guiIcon;
    private int shotData1; // uint32 ?
    private int shotData2; // uint32 ?
    private float range;
    private EnumSet<CreatureSpellFlag> flags;
    private int combatPoints; // 6
    private int soundEvent;
    private int nameStringId;
    private int tooltipStringId;
    private int generalDescriptionStringId;
    private int strengthStringId;
    private int weaknessStringId;
    private short creatureSpellId;
    private short shotTypeId;
    private short alternativeShotId;
    private short alternativeRoomId;
    private float rechargeTime;
    private AlternativeShot alternativeShot; // When creature flag is on, this plays some part
    private short[] unused;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource getEditorIcon() {
        return editorIcon;
    }

    protected void setEditorIcon(ArtResource editorIcon) {
        this.editorIcon = editorIcon;
    }

    public ArtResource getGuiIcon() {
        return guiIcon;
    }

    protected void setGuiIcon(ArtResource guiIcon) {
        this.guiIcon = guiIcon;
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

    public float getRange() {
        return range;
    }

    protected void setRange(float range) {
        this.range = range;
    }

    public EnumSet<CreatureSpellFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<CreatureSpellFlag> flags) {
        this.flags = flags;
    }

    public int getCombatPoints() {
        return combatPoints;
    }

    protected void setCombatPoints(int combatPoints) {
        this.combatPoints = combatPoints;
    }

    public int getSoundEvent() {
        return soundEvent;
    }

    protected void setSoundEvent(int soundEvent) {
        this.soundEvent = soundEvent;
    }

    public int getNameStringId() {
        return nameStringId;
    }

    protected void setNameStringId(int nameStringId) {
        this.nameStringId = nameStringId;
    }

    public int getTooltipStringId() {
        return tooltipStringId;
    }

    protected void setTooltipStringId(int tooltipStringId) {
        this.tooltipStringId = tooltipStringId;
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

    public short getCreatureSpellId() {
        return creatureSpellId;
    }

    protected void setCreatureSpellId(short creatureSpellId) {
        this.creatureSpellId = creatureSpellId;
    }

    public short getShotTypeId() {
        return shotTypeId;
    }

    protected void setShotTypeId(short shotTypeId) {
        this.shotTypeId = shotTypeId;
    }

    public short getAlternativeShotId() {
        return alternativeShotId;
    }

    protected void setAlternativeShotId(short alternativeShotId) {
        this.alternativeShotId = alternativeShotId;
    }

    public short getAlternativeRoomId() {
        return alternativeRoomId;
    }

    protected void setAlternativeRoomId(short alternativeRoomId) {
        this.alternativeRoomId = alternativeRoomId;
    }

    public float getRechargeTime() {
        return rechargeTime;
    }

    protected void setRechargeTime(float rechargeTime) {
        this.rechargeTime = rechargeTime;
    }

    public AlternativeShot getAlternativeShot() {
        return alternativeShot;
    }

    protected void setAlternativeShot(AlternativeShot alternativeShot) {
        this.alternativeShot = alternativeShot;
    }

    public short[] getUnused() {
        return unused;
    }

    protected void setUnused(short[] unused) {
        this.unused = unused;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(CreatureSpell o) {
        return Short.compare(creatureSpellId, o.creatureSpellId);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.creatureSpellId;
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
        final CreatureSpell other = (CreatureSpell) obj;
        if (this.creatureSpellId != other.creatureSpellId) {
            return false;
        }
        return true;
    }
}
