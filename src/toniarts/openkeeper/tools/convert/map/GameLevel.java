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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 *
 * @author ArchDemon
 */
public class GameLevel {
    // KWD data
    //    struct LevelInfoBlock {
    //        ucs2le_t m_wsName[64]; /* 134 */
    //        ucs2le_t m_wsDescription[1024]; /* 1b4 */
    //        ucs2le_t m_wsAuthor[64]; /* 9b4 */
    //        ucs2le_t m_wsEmail[64]; /* a34 */
    //        ucs2le_t m_wsInformation[1024]; /* ab4 */
    //        uint16_t m_wShortId0;
    //        uint16_t m_wShortId1;
    //        uint8_t x01184[520];
    //        ucs2le_t m_wsUnknown0138c[20][512];
    //        uint16_t x0638c;
    //        char x0638e[32];
    //        uint8_t x063ae;
    //        uint8_t x063af[4];
    //        uint8_t x063b3[4];
    //        uint8_t x063b7;
    //        uint8_t x063b8;
    //        uint16_t x063b9;
    //        uint16_t x063bb;
    //        uint16_t x063bd;
    //        uint16_t x063bf;
    //        uint16_t x063c3;
    //        uint16_t x063c5;
    //        uint16_t x063c7;
    //        uint16_t x063c9;
    //        uint16_t x063ca;
    //        uint8_t x063cb[8];
    //        uint16_t x063d3[8];
    //        char x063e3[32];
    //        uint8_t x06403;
    //        uint8_t x06404;
    //        uint8_t rewardPrevious5;
    //        uint8_t rewardNext5;
    //        uint16_t x06407;
    //        uint16_t x06409[5];
    //        ucs2le_t x06413[32];
    //   };

    public enum LevFlag implements IFlagEnum {

        UNKNOWN(0x0004), // FIXME unknown flag. Always on in maps
        ALWAYS_IMPRISON_ENEMIES(0x0008), // Always imprison enemies
        ONE_SHOT_HORNY(0x0010), // Set if one shot Horny spell is available
        IS_SECRET_LEVEL(0x0020), // The map is Secret level
        IS_SPECIAL_LEVEL(0x0040), // The map is Special level
        SHOW_HERO_KILLS(0x0080), // Display "Heroes killed" tally
        AUTO_OBJECTIVE_BOX(0x0100), // Automatic show objective box
        HEART_MAKES_GEM(0x0200), // Last heart generates Portal Gem
        IS_MULTIPLAYER_LEVEL(0x0400), // The map is Multiplayer level
        IS_SKIRMISH_LEVEL(0x0800), // The map is Skirmish level
        FREEZE_OPTIONS(0x1000), // Freeze game options
        IS_MY_PET_DUNGEON_LEVEL(0x2000); // The map is My Pet Dungeon level
        private final long flagValue;

        private LevFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum TextTable implements IValueEnum {

        NONE(0, null, null),
        LEVEL_1(1, "LEVEL1", "LEVEL1_BRIEFING"),
        LEVEL_2(2, "LEVEL2", "LEVEL2_BRIEFING"),
        LEVEL_3(3, "LEVEL3", "LEVEL3_BRIEFING"),
        LEVEL_4(4, "LEVEL4", "LEVEL4_BRIEFING"),
        LEVEL_5(5, "LEVEL5", "LEVEL5_BRIEFING"),
        LEVEL_6A(6, "LEVEL6A", "LEVEL6A_BRIEFING"),
        LEVEL_6B(7, "LEVEL6B", "LEVEL6B_BRIEFING"),
        LEVEL_7(8, "LEVEL7", "LEVEL7_BRIEFING"),
        LEVEL_8(9, "LEVEL8", "LEVEL8_BRIEFING"),
        LEVEL_9(10, "LEVEL9", "LEVEL9_BRIEFING"),
        LEVEL_10(11, "LEVEL10", "LEVEL10_BRIEFING"),
        LEVEL_11A(12, "LEVEL11A", "LEVEL11A_BRIEFING"),
        LEVEL_11B(13, "LEVEL11B", "LEVEL11B_BRIEFING"),
        LEVEL_11C(14, "LEVEL11C", "LEVEL11C_BRIEFING"),
        LEVEL_12(15, "LEVEL12", "LEVEL12_BRIEFING"),
        LEVEL_13(16, "LEVEL13", "LEVEL13_BRIEFING"),
        LEVEL_14(17, "LEVEL14", "LEVEL14_BRIEFING"),
        LEVEL_15A(18, "LEVEL15A", "LEVEL15A_BRIEFING"),
        LEVEL_15B(19, "LEVEL15B", "LEVEL15B_BRIEFING"),
        LEVEL_16(20, "LEVEL16", "LEVEL16_BRIEFING"),
        LEVEL_17(21, "LEVEL17", "LEVEL17_BRIEFING"),
        LEVEL_18(22, "LEVEL18", "LEVEL18_BRIEFING"),
        LEVEL_19(23, "LEVEL19", "LEVEL19_BRIEFING"),
        LEVEL_20(24, "LEVEL20", "LEVEL20_BRIEFING"),
        MULTI_PLAYER_1(25, "MULTIPLAYER", "LEVELM1_BRIEFING"),
        MY_PET_DUNGEON_1(26, "MYPETDUNGEON", "LEVELMPD1_BRIEFING"),
        SECRET_1(27, "SECRET1", "LEVELS1_BRIEFING"),
        SECRET_2(28, "SECRET2", "LEVELS2_BRIEFING"),
        SECRET_3(29, "SECRET3", "LEVELS3_BRIEFING"),
        SECRET_4(30, "SECRET4", "LEVELS4_BRIEFING"),
        SECRET_5(31, "SECRET5", "LEVELS5_BRIEFING"),
        DEMO_1(32, "DEMO1", null),
        DEMO_2(33, "DEMO2", null),
        DEMO_3(34, "DEMO3", null),
        MY_PET_DUNGEON_2(35, "MYPETDUNGEON", "LEVELMPD2_BRIEFING"),
        MY_PET_DUNGEON_3(36, "MYPETDUNGEON", "LEVELMPD3_BRIEFING"),
        MY_PET_DUNGEON_4(37, "MYPETDUNGEON", "LEVELMPD4_BRIEFING"),
        MY_PET_DUNGEON_5(38, "MYPETDUNGEON", "LEVELMPD5_BRIEFING"),
        MY_PET_DUNGEON_6(39, "MYPETDUNGEON", "LEVELMPD6_BRIEFING"),
        MY_PET_DUNGEON_7(40, "MYPETDUNGEON", "LEVELMPD7_BRIEFING"),
        MULTI_PLAYER_2(41, "MULTIPLAYER", "LEVELM2_BRIEFING"),
        MULTI_PLAYER_3(42, "MULTIPLAYER", "LEVELM3_BRIEFING"),
        MULTI_PLAYER_4(43, "MULTIPLAYER", "LEVELM4_BRIEFING"),
        MULTI_PLAYER_5(44, "MULTIPLAYER", "LEVELM5_BRIEFING"),
        MULTI_PLAYER_6(45, "MULTIPLAYER", "LEVELM6_BRIEFING"),
        MULTI_PLAYER_7(46, "MULTIPLAYER", "LEVELM7_BRIEFING"),
        MULTI_PLAYER_8(47, "MULTIPLAYER", "LEVELM8_BRIEFING"),
        MULTI_PLAYER_9(48, "MULTIPLAYER", "LEVELM9_BRIEFING"),
        MULTI_PLAYER_10(49, "MULTIPLAYER", "LEVELM10_BRIEFING"),
        MULTI_PLAYER_11(50, "MULTIPLAYER", "LEVELM11_BRIEFING"),
        MULTI_PLAYER_12(51, "MULTIPLAYER", "LEVELM12_BRIEFING"),
        MULTI_PLAYER_13(52, "MULTIPLAYER", "LEVELM13_BRIEFING"),
        MULTI_PLAYER_14(53, "MULTIPLAYER", "LEVELM14_BRIEFING"),
        MULTI_PLAYER_15(54, "MULTIPLAYER", "LEVELM15_BRIEFING"),
        MULTI_PLAYER_16(55, "MULTIPLAYER", "LEVELM16_BRIEFING"),
        MULTI_PLAYER_17(56, "MULTIPLAYER", "LEVELM17_BRIEFING"),
        MULTI_PLAYER_18(57, "MULTIPLAYER", "LEVELM18_BRIEFING"),
        MULTI_PLAYER_19(58, "MULTIPLAYER", "LEVELM19_BRIEFING"),
        MULTI_PLAYER_20(59, "MULTIPLAYER", "LEVELM20_BRIEFING");

        private TextTable(int id, String levelDictFile, String levelBriefingDictFile) {
            this.id = id;
            this.levelDictFile = levelDictFile;
            this.levelBriefingDictFile = levelBriefingDictFile;
        }

        @Override
        public int getValue() {
            return id;
        }

        public String getLevelDictFile() {
            return levelDictFile;
        }

        public String getLevelBriefingDictFile() {
            return levelBriefingDictFile;
        }

        private final int id;
        private final String levelDictFile;
        private final String levelBriefingDictFile;
    }

    public enum LevelReward implements IValueEnum {

        NONE(0),
        ALARM(1),
        BARRICADE(2),
        BOULDER(3),
        BRACED(4),
        BRIDGE_STONE(5),
        BRIDGE_WOODEN(6),
        CALL_TO_ARMS(7),
        CASINO(8),
        DARK_LIBRARY(12),
        FEAR(14),
        FIREBURST(15),
        FREEZE(16),
        GAS(17),
        GRAVEYARD(18),
        GUARD_ROOM(19),
        GUARD_POST(20),
        HATCHERY(21),
        LAIR(24),
        LIGHTNING(25),
        MAGIC_DOOR(26),
        PIT(27),
        PRISON(29),
        REAPER_TALISMAN_4(30),
        SECRET_DOOR(32),
        SENTRY(33),
        SPIKE(35),
        STEEL_DOOR(36),
        TEMPLE(39),
        TORTURE(41),
        TRAINING(42),
        TREASURY(43),
        TRIGGER(44),
        WOOD_DOOR(47),
        WORK_SHOP(48),
        REAPER_TALISMAN_1(49),
        REAPER_TALISMAN_2(50),
        REAPER_TALISMAN_3(51);

        private LevelReward(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    protected String name;
    protected String description;
    protected String author;
    protected String email;
    protected String information;
    protected int triggerId; // Associated trigger
    protected int ticksPerSec;
    protected short x01184[];
    protected List<String> messages;
    protected EnumSet<LevFlag> lvlFlags;
    protected String speechStr;
    protected short talismanPieces;
    protected List<LevelReward> rewardPrev = new ArrayList<>();
    protected List<LevelReward> rewardNext = new ArrayList<>();
    protected short soundTrack;
    protected TextTable textTableId;
    protected int textTitleId;
    protected int textPlotId;
    protected int textDebriefId;
    protected int textObjectvId;
    protected int x063c3; //this may be first text_subobjctv_id - not sure
    protected int textSubobjctvId1;
    protected int textSubobjctvId2;
    protected int textSubobjctvId3;
    protected int speclvlIdx;
    protected Map<Short, Integer> introductionOverrideTextIds; // Creature ID, TextID
    protected String terrainPath;
    protected short oneShotHornyLev;
    protected short playerCount;
    protected int speechHornyId;
    protected int speechPrelvlId;
    protected int speechPostlvlWin;
    protected int speechPostlvlLost;
    protected int speechPostlvlNews;
    protected int speechPrelvlGenr;
    protected String heroName;
    //
    protected List<FilePath> paths;
    protected int unknown[];

    //
    protected final static String TEXT_DIR = "Interface/Texts/";
    private ResourceBundle resourceBundle;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    protected void setAuthor(String author) {
        this.author = author;
    }

    public String getEmail() {
        return email;
    }

    protected void setEmail(String email) {
        this.email = email;
    }

    public String getInformation() {
        return information;
    }

    protected void setInformation(String information) {
        this.information = information;
    }

    /**
     * Get level flags
     *
     * @return level flags
     */
    public EnumSet<LevFlag> getLvlFlags() {
        return lvlFlags;
    }

    protected void setLvlFlags(EnumSet<LevFlag> flags) {
        this.lvlFlags = flags;
    }

    /**
     * Get number of players supported by the map
     *
     * @return player count
     */
    public short getPlayerCount() {
        return playerCount;
    }

    protected void setPlayerCount(short playerCount) {
        this.playerCount = playerCount;
    }

    public int getTriggerId() {
        return triggerId;
    }

    protected void setTriggerId(int triggerId) {
        this.triggerId = triggerId;
    }

    public int getTicksPerSec() {
        return ticksPerSec;
    }

    protected void setTicksPerSec(int ticksPerSec) {
        this.ticksPerSec = ticksPerSec;
    }

    public short[] getX01184() {
        return x01184;
    }

    protected void setX01184(short[] x01184) {
        this.x01184 = x01184;
    }

    public List<String> getMessages() {
        return messages;
    }

    protected void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getSpeechStr() {
        return speechStr;
    }

    protected void setSpeechStr(String speechStr) {
        this.speechStr = speechStr;
    }

    public short getTalismanPieces() {
        return talismanPieces;
    }

    protected void setTalismanPieces(short talismanPieces) {
        this.talismanPieces = talismanPieces;
    }

    public List<LevelReward> getRewardPrev() {
        return rewardPrev;
    }

    protected void addRewardPrev(LevelReward reward) {
        if (reward != null && !reward.equals(LevelReward.NONE)) {
            this.rewardPrev.add(reward);
        }
    }

    public List<LevelReward> getRewardNext() {
        return rewardNext;
    }

    protected void addRewardNext(LevelReward reward) {
        if (reward != null && !reward.equals(LevelReward.NONE)) {
            this.rewardNext.add(reward);
        }
    }

    public short getSoundTrack() {
        return soundTrack;
    }

    protected void setSoundTrack(short soundTrack) {
        this.soundTrack = soundTrack;
    }

    public TextTable getTextTableId() {
        return textTableId;
    }

    protected void setTextTableId(TextTable textTableId) {
        this.textTableId = textTableId;
    }

    public int getTextTitleId() {
        return textTitleId;
    }

    protected void setTextTitleId(int textTitleId) {
        this.textTitleId = textTitleId;
    }

    public int getTextPlotId() {
        return textPlotId;
    }

    protected void setTextPlotId(int textPlotId) {
        this.textPlotId = textPlotId;
    }

    public int getTextDebriefId() {
        return textDebriefId;
    }

    protected void setTextDebriefId(int textDebriefId) {
        this.textDebriefId = textDebriefId;
    }

    public int getTextObjectvId() {
        return textObjectvId;
    }

    protected void setTextObjectvId(int textObjectvId) {
        this.textObjectvId = textObjectvId;
    }

    public int getX063c3() {
        return x063c3;
    }

    protected void setX063c3(int x063c3) {
        this.x063c3 = x063c3;
    }

    public int getTextSubobjctvId1() {
        return textSubobjctvId1;
    }

    protected void setTextSubobjctvId1(int textSubobjctvId1) {
        this.textSubobjctvId1 = textSubobjctvId1;
    }

    public int getTextSubobjctvId2() {
        return textSubobjctvId2;
    }

    protected void setTextSubobjctvId2(int textSubobjctvId2) {
        this.textSubobjctvId2 = textSubobjctvId2;
    }

    public int getTextSubobjctvId3() {
        return textSubobjctvId3;
    }

    protected void setTextSubobjctvId3(int textSubobjctvId3) {
        this.textSubobjctvId3 = textSubobjctvId3;
    }

    public int getSpeclvlIdx() {
        return speclvlIdx;
    }

    protected void setSpeclvlIdx(int speclvlIdx) {
        this.speclvlIdx = speclvlIdx;
    }

    public Map<Short, Integer> getIntroductionOverrideTextIds() {
        return introductionOverrideTextIds;
    }

    protected void setIntroductionOverrideTextIds(Map<Short, Integer> introductionOverrideTextIds) {
        this.introductionOverrideTextIds = introductionOverrideTextIds;
    }

    public String getTerrainPath() {
        return terrainPath;
    }

    protected void setTerrainPath(String terrainPath) {
        this.terrainPath = terrainPath;
    }

    public short getOneShotHornyLev() {
        return oneShotHornyLev;
    }

    protected void setOneShotHornyLev(short oneShotHornyLev) {
        this.oneShotHornyLev = oneShotHornyLev;
    }

    public int getSpeechHornyId() {
        return speechHornyId;
    }

    protected void setSpeechHornyId(int speechHornyId) {
        this.speechHornyId = speechHornyId;
    }

    public int getSpeechPrelvlId() {
        return speechPrelvlId;
    }

    protected void setSpeechPrelvlId(int speechPrelvlId) {
        this.speechPrelvlId = speechPrelvlId;
    }

    public int getSpeechPostlvlWin() {
        return speechPostlvlWin;
    }

    protected void setSpeechPostlvlWin(int speechPostlvlWin) {
        this.speechPostlvlWin = speechPostlvlWin;
    }

    public int getSpeechPostlvlLost() {
        return speechPostlvlLost;
    }

    protected void setSpeechPostlvlLost(int speechPostlvlLost) {
        this.speechPostlvlLost = speechPostlvlLost;
    }

    public int getSpeechPostlvlNews() {
        return speechPostlvlNews;
    }

    protected void setSpeechPostlvlNews(int speechPostlvlNews) {
        this.speechPostlvlNews = speechPostlvlNews;
    }

    public int getSpeechPrelvlGenr() {
        return speechPrelvlGenr;
    }

    protected void setSpeechPrelvlGenr(int speechPrelvlGenr) {
        this.speechPrelvlGenr = speechPrelvlGenr;
    }

    public String getHeroName() {
        return heroName;
    }

    protected void setHeroName(String heroName) {
        this.heroName = heroName;
    }

    public List<FilePath> getPaths() {
        return paths;
    }

    protected void setPaths(List<FilePath> paths) {
        this.paths = paths;
    }

    public int[] getUnknown() {
        return unknown;
    }

    protected void setUnknown(int[] unknown) {
        this.unknown = unknown;
    }

    public String getFile(MapDataTypeEnum type) {
        for (FilePath file : paths) {
            if (file.getId() == type) {
                return file.getPath();
            }
        }
        return null;
    }

    /**
     * Reads the resource bundle from disc and stores it
     *
     * @return the resource bundle
     */
    private ResourceBundle readResourceBundle() {
        if (this.getName().equalsIgnoreCase("mpd7")) {
            // it was hardcoded in dk2 too
            return Main.getResourceBundle(TEXT_DIR.concat("LEVELMPD7_BRIEFING"));
        } else if (this.getTextTableId() != null && this.getTextTableId() != GameLevel.TextTable.NONE && this.getTextTableId().getLevelBriefingDictFile() != null) {
            return Main.getResourceBundle(TEXT_DIR.concat(this.getTextTableId().getLevelBriefingDictFile()));
        } else {
            final String briefing = this.getName().concat("_BRIEFING");
            try {
                return Main.getResourceBundle(TEXT_DIR.concat(briefing));
            } catch (Exception e) {
                // stack is already thrown by getResourceBundle
            }
        }
        // return empty resource bundle otherwise
        return new ResourceBundle() {
            @Override
            protected java.lang.Object handleGetObject(String string) {
                return "";
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(new HashMap().keySet());
            }
        };
    }

    /**
     * Get the selected level title (value 0 and value 1 combined)
     *
     * @return level title
     */
    public String getTitle() {
        ResourceBundle dict = getResourceBundle();
        StringBuilder sb = new StringBuilder();
        String levelName = dict.getString("0");
        if (!levelName.isEmpty()) {
            // is empty on secret levels
            sb.append("\"");
            sb.append(levelName);
            sb.append("\" - ");
        }
        sb.append(dict.getString("1"));
        return sb.toString();
    }

    public String getLevelName() {
        return getResourceBundle().getString("0");
    }

    public String getMainObjective() {
        return getResourceBundle().getString("2");
    }

    public String getSubObjective1() {
        return getResourceBundle().getString("3");
    }

    public String getSubObjective2() {
        return getResourceBundle().getString("4");
    }

    public String getSubObjective3() {
        return getResourceBundle().getString("5");
    }

    /**
     * Gets the selected level briefing resource bundle
     *
     * @return the resource bundle
     */
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            this.resourceBundle = readResourceBundle();
        }
        return this.resourceBundle;
    }

    public boolean hasBriefing() {
        return !this.getResourceBundle().keySet().isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
