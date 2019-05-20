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
import toniarts.openkeeper.game.data.IGuiSoundable;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.game.data.ISoundable;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * Container class for KeeperSpells.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KeeperSpell implements Comparable<KeeperSpell>, ISoundable, IIndexable, IGuiSoundable {

    /**
     * Keeper spell flags
     */
    public enum KeeperSpellFlag implements IFlagEnum {

        CREATE_WORKER(0x0002),
        ATTACKING(0x0004),
        DEFENSIVE(0x0008),
        DONT_CAST_ON_CREATURES(0x0010);
        private final long flagValue;

        private KeeperSpellFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum HandAnimId implements IValueEnum {

        NULL(0),
        POINT(1);

        private HandAnimId(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum CastRule implements IValueEnum {

        NONE(0),
        OWN_LAND(1),
        OWN_AND_NEUTRAL_LAND(2),
        ENEMY_LAND(3),
        ANY_LAND(4),
        ANYWHERE(5);

        private CastRule(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum TargetRule implements IValueEnum {

        NONE(0),
        OWN_CREATURES(1),
        ENEMY_CREATURES(2),
        ALL_CREATURES(3),
        LAND(4),
        ALL(5),
        POSESSION(6);

        private TargetRule(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
//    struct KeeperSpellBlock {
//        char name[32];
//        ArtResource ref1;
//        ArtResource ref3;
//        int xc8;
//        int xcc;
//        int xd0;
//        int xd4;
//        uint16_t xd8;
//        uint8_t xda;
//        uint8_t xdb;
//        int xdc;
//        uint16_t xe0Unreferenced; /* e0 */
//        uint16_t xe2;
//        uint16_t xe4;
//        uint16_t xe6;
//        uint16_t xe8;
//        uint16_t xea;
//        uint16_t xec;
//        uint8_t id; /* ee */
//        uint8_t xef;
//        uint8_t xf0;
//        char yname[32]; /* xf1 */
//        uint16_t x111;
//        uint8_t x113;
//        int x114;
//        int x118;
//        int x11c;
//        ArtResource ref2; /* 120 */
//        char xname[32]; /* 174 */
//        uint8_t x194;
//        uint8_t x195;
//        };
    private String name;
    private ArtResource guiIcon;
    private ArtResource editorIcon; // ??
    private int xc8;
    private float rechargeTime; // In turns??
    private int shotData1;
    private int shotData2;
    private int researchTime;
    private TargetRule targetRule;
    private short orderInEditor; // introductionIndex in editor;
    private EnumSet<KeeperSpellFlag> flags;
    private int xe0Unreferenced; // e0
    private int manaDrain;
    private int tooltipStringId;
    private int nameStringId;
    private int generalDescriptionStringId;
    private int strengthStringId;
    private int weaknessStringId;
    private short keeperSpellId; // ee
    private CastRule castRule;
    private short shotTypeId; // Shot
    private String soundCategory; // xf1
    // Bonus update
    private int bonusRTime;
    private short bonusShotTypeId; // Shot
    private int bonusShotData1;
    private int bonusShotData2;
    private int manaCost;
    private ArtResource bonusIcon; // 120
    private String soundCategoryGui; // 174
    private HandAnimId handAnimId;
    private HandAnimId noGoHandAnimId;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
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

    public int getXc8() {
        return xc8;
    }

    protected void setXc8(int xc8) {
        this.xc8 = xc8;
    }

    public float getRechargeTime() {
        return rechargeTime;
    }

    protected void setRechargeTime(float rechargeTime) {
        this.rechargeTime = rechargeTime;
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

    public int getResearchTime() {
        return researchTime;
    }

    protected void setResearchTime(int researchTime) {
        this.researchTime = researchTime;
    }

    public TargetRule getTargetRule() {
        return targetRule;
    }

    protected void setTargetRule(TargetRule targetRule) {
        this.targetRule = targetRule;
    }

    public short getOrderInEditor() {
        return orderInEditor;
    }

    protected void setOrderInEditor(short orderInEditor) {
        this.orderInEditor = orderInEditor;
    }

    public EnumSet<KeeperSpellFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<KeeperSpellFlag> flags) {
        this.flags = flags;
    }

    public int getXe0Unreferenced() {
        return xe0Unreferenced;
    }

    protected void setXe0Unreferenced(int xe0Unreferenced) {
        this.xe0Unreferenced = xe0Unreferenced;
    }

    public int getManaDrain() {
        return manaDrain;
    }

    protected void setManaDrain(int manaDrain) {
        this.manaDrain = manaDrain;
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

    public short getKeeperSpellId() {
        return keeperSpellId;
    }

    protected void setKeeperSpellId(short keeperSpellId) {
        this.keeperSpellId = keeperSpellId;
    }

    public CastRule getCastRule() {
        return castRule;
    }

    protected void setCastRule(CastRule castRule) {
        this.castRule = castRule;
    }

    public short getShotTypeId() {
        return shotTypeId;
    }

    protected void setShotTypeId(short shotTypeId) {
        this.shotTypeId = shotTypeId;
    }

    @Override
    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundGategory) {
        this.soundCategory = soundGategory;
    }

    public int getBonusRTime() {
        return bonusRTime;
    }

    protected void setBonusRTime(int bonusRTime) {
        this.bonusRTime = bonusRTime;
    }

    public short getBonusShotTypeId() {
        return bonusShotTypeId;
    }

    protected void setBonusShotTypeId(short bonusShotTypeId) {
        this.bonusShotTypeId = bonusShotTypeId;
    }

    public int getBonusShotData1() {
        return bonusShotData1;
    }

    protected void setBonusShotData1(int bonusShotData1) {
        this.bonusShotData1 = bonusShotData1;
    }

    public int getBonusShotData2() {
        return bonusShotData2;
    }

    protected void setBonusShotData2(int bonusShotData2) {
        this.bonusShotData2 = bonusShotData2;
    }

    public int getManaCost() {
        return manaCost;
    }

    protected void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public ArtResource getBonusIcon() {
        return bonusIcon;
    }

    protected void setBonusIcon(ArtResource bonusIcon) {
        this.bonusIcon = bonusIcon;
    }

    @Override
    public String getSoundCategoryGui() {
        return soundCategoryGui;
    }

    protected void setSoundCategoryGui(String soundGategoryGui) {
        this.soundCategoryGui = soundGategoryGui;
    }

    public HandAnimId getHandAnimId() {
        return handAnimId;
    }

    protected void setHandAnimId(HandAnimId handAnimId) {
        this.handAnimId = handAnimId;
    }

    public HandAnimId getNoGoHandAnimId() {
        return noGoHandAnimId;
    }

    protected void setNoGoHandAnimId(HandAnimId noGoHandAnimId) {
        this.noGoHandAnimId = noGoHandAnimId;
    }

    @Override
    public short getId() {
        return keeperSpellId;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(KeeperSpell o) {
        return Short.compare(orderInEditor, o.orderInEditor);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.keeperSpellId;
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
        final KeeperSpell other = (KeeperSpell) obj;
        if (this.keeperSpellId != other.keeperSpellId) {
            return false;
        }
        return true;
    }
}
