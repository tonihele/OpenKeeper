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
    //        uint8_t x06405;
    //        uint8_t x06406;
    //        uint16_t x06407;
    //        uint16_t x06409[5];
    //        ucs2le_t x06413[32];
    //   };

    public enum LevFlag implements IFlagEnum {

        //UNKNOWN(0x0004), // unknown; always on in maps
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

        NONE(0),
        LEVEL_1(1),
        LEVEL_2(2),
        LEVEL_3(3),
        LEVEL_4(4),
        LEVEL_5(5),
        LEVEL_6A(6),
        LEVEL_6B(7),
        LEVEL_7(8),
        LEVEL_8(9),
        LEVEL_9(10),
        LEVEL_10(11),
        LEVEL_11A(12),
        LEVEL_11B(13),
        LEVEL_11C(14),
        LEVEL_12(15),
        LEVEL_13(16),
        LEVEL_14(17),
        LEVEL_15A(18),
        LEVEL_15B(19),
        LEVEL_16(20),
        LEVEL_17(21),
        LEVEL_18(22),
        LEVEL_19(23),
        LEVEL_20(24),
        MULTI_PLAYER_1(25),
        MY_PET_DUNGEON_1(26),
        SECRET_1(27),
        SECRET_2(28),
        SECRET_3(29),
        SECRET_4(30),
        SECRET_5(31),
        DEMO_1(32),
        DEMO_2(33),
        DEMO_3(34),
        MY_PET_DUNGEON_2(35),
        MY_PET_DUNGEON_3(36),
        MY_PET_DUNGEON_4(37),
        MY_PET_DUNGEON_5(38),
        MY_PET_DUNGEON_6(39),
        MULTI_PLAYER_2(41),
        MULTI_PLAYER_3(42),
        MULTI_PLAYER_4(43),
        MULTI_PLAYER_5(44),
        MULTI_PLAYER_6(45),
        MULTI_PLAYER_7(46),
        MULTI_PLAYER_8(47),
        MULTI_PLAYER_9(48),
        MULTI_PLAYER_10(49),
        MULTI_PLAYER_11(50),
        MULTI_PLAYER_12(51),
        MULTI_PLAYER_13(52),
        MULTI_PLAYER_14(53),
        MULTI_PLAYER_15(54),
        MULTI_PLAYER_16(55),
        MULTI_PLAYER_17(56),
        MULTI_PLAYER_18(57),
        MULTI_PLAYER_19(58),
        MULTI_PLAYER_20(59);

        private TextTable(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
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
    protected List<LevelReward> rewardPrev;
    protected List<LevelReward> rewardNext;
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
    protected java.util.Map<Short, Integer> introductionOverrideTextIds; // Creature ID, TextID
    protected String terrainPath;
    protected short oneShotHornyLev;
    protected short playerCount;
    protected short x06405; // rewardPrev[4]??
    protected short x06406; // rewardNext[4]??
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
    
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getEmail() {
        return email;
    }

    public String getInformation() {
        return information;
    }
    
    /**
     * Get level flags
     *
     * @return level flags
     */
    public EnumSet<LevFlag> getLvlFlags() {
        return lvlFlags;
    }

    /**
     * Get number of players supported by the map
     *
     * @return player count
     */
    public short getPlayerCount() {
        return playerCount;
    }
    
    public String getFile(MapDataTypeEnum type) {
        for (FilePath file : paths) {
            if (file.getId() == type) {
                return file.getPath();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
