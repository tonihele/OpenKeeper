/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.sound.BankMapFile;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxSoundEntry;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxEEEntry;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxGroupEntry;
import toniarts.openkeeper.utils.PathUtils;

/**
 *
 * @author archdemon
 */
public class SoundGroup {

    public enum SoundType {
        // 1 - 60 level speech
        OBJECT_RESEARCH_1(49),
        OBJECT_RESEARCH_2(50),
        OBJECT_RESEARCH_3(51),
        OBJECT_RESEARCH_4(52),
        PICKED_UP(195), // creature or object
        DROPPED(196), // creature or object
        CREATURE_SLAP(197),
        CREATURE_STUNNED(199),
        CREATURE_TICKLED(200),
        CREATURE_IN_HAND(201),
        CREATURE_HAPPY(202),
        CREATURE_UNHAPPY(203),
        CREATURE_ANGRY(204),
        CREATURE_EAT(205),
        CREATURE_SLEEP(206),
        CREATURE_TORTURE_GENERAL(207), // FIXME maybe not. TORTURE_WHEEL
        CREATURE_IMPRISON(208),
        CREATURE_TRAIN(211),
        CREATURE_PRAY(212),
        CREATURE_GAMBLE(213),
        CREATURE_CHARGE(214),
        CREATURE_ATTKSP(217),
        CREATURE_DIE(218),
        CREATURE_HIT_OR_MELEE(219),
        CREATURE_FLEE(220),
        CREATURE_MELEE_1(221),
        CREATURE_SULK(222),
        CREATURE_MELEE_2(224), // drop
        CREATURE_DRINK(225),
        CREATURE_SACRIFICE(226),
        CREATURE_HELLO(228),
        CREATURE_FALL(230),
        DOOR_OPEN(231),
        DOOR_CLOSE(231),
        DOOR_LOCK(233),
        DOOR_UNLOCK(234),
        TRAP_ATTACK(235),
        CREATE(241), // chicken sound, room create
        OBJECT_RAT_SQUEAK(242), // or bat fly
        DESTROYED(244), // terrain and object
        ROOM_SELL(245),
        CREATURE_FALL_DOOR(246), // sound like door is locked. heavylump, land
        OBJECT_TORTURE_WHEEL(251),
        CREATURE_KEEP_1(265),
        CREATURE_CLAIM_1(266),
        CREATURE_CLAIM_2(267),
        REINFORCE(268), // REPAIR creature or terrain
        CREATURE_DRAG(269),
        MANUFACTURED(271), // door or trap
        OBJECT_CHICKEN(272),
        OBJECT_CHICKEN_FLAP(273),
        OBJECT_FORGE_HIT(274),
        TRAP_SENTURE_EXPLOSE_1(276),
        BOULDER_HIT_WALL(329),
        BOULDER_HIT_WOOD_DOOR(330),
        BOULDER_INTO_WATER(331),
        BOULDER_INTO_LAVA(332),
        BOULDER_HIT_CREATURE(333),
        BOULDER_KILL_CREATURE(334),
        BOULDER_ROLL(335),
        CREATURE_TORTURE_WHEEL(342),
        BOULDER_HIT_DOOR(346),
        TRAP_SENTURE_EXPLOSE_2(443),
        AMBIENCE(746), // terrain or room. E.g. water wave sound
        ROOM_HERO_GATE_BIRDIE(747), // AMBIENCE_2
        ROOM_LAY_TILE(760),
        CREATURE_REPORT(762),
        OBJECT_TRAIN_MECH(785),
        CREATURE_CHAT(786),
        CREATURE_FT_1(788),
        CREATURE_FT_2(789),
        TERRAIN_GENSPELL(793),
        TORTURE_FIRE(795), // creature or object
        CREATURE_TORTURE_ELEC(796), // TORTURE_WATER
        CREATURE_TRAPS_OR_CRATES_1(806),
        CREATURE_DOORS_WOOD(807),
        CREATURE_TRAPS_OR_CRATES_2(808),
        CREATURE_FT_3(818), // drag
        CREATURE_FT_4(819),
        CREATURE_KEEP_2(820),
        CREATURE_IMPENETRABLE(821),
        CREATURE_STINKY(822),
        CREATURE_WATER_SWIM(823),
        CREATURE_REINFORCE(829),
        CREATURE_TORCH_LOOP(832),
        CREATURE_TELEPORT_IN(834),
        CREATURE_TELEPORT_OUT(835),
        CREATURE_LEVEL_UP(841);

        private SoundType(int value) {
            this.value = value;
        }

        private final int value;

        public int getValue() {
            return value;
        }
    }

    private final SoundCategory category;
    private final SfxGroupEntry entry;
    private final List<SoundFile> files = new ArrayList<>();

    public SoundGroup(SoundCategory category, SfxGroupEntry entry) {
        this.category = category;
        this.entry = entry;

        parseFiles();
    }

    public int getId() {
        return entry.getTypeId();
    }

    public SoundCategory getCategory() {
        return category;
    }

    public List<SoundFile> getFiles() {
        return files;
    }

    public void addFile(SoundFile file) {
        files.add(file);
    }

    private void parseFiles() {
        BankMapFile bank = category.getBankMapFile();

        for (SfxEEEntry eEEntry: entry.getEntries()) {
            for (SfxSoundEntry eEEEntry: eEEntry.getSounds()) {
                int archiveId = eEEEntry.getArchiveId() - 1;
                int soundId = eEEEntry.getIndex() - 1;

                String archiveFilename = ConversionUtils.convertFileSeparators(bank.getEntries()[archiveId].getName());
                SdtFile sdt = SoundCategory.getSdtFile(archiveFilename);
                if (sdt == null) {
                    throw new RuntimeException("Sdt file " + archiveFilename + " not exits");
                }

                String relative = new File(PathUtils.getDKIIFolder()
                        + PathUtils.DKII_SFX_FOLDER).toPath().relativize(sdt.getFile().toPath()).toString();
                String soundFilename = relative.substring(0, relative.length() - 4) + File.separator
                        + SdtFile.fixFileExtension(sdt.getEntires()[soundId]);

                SoundFile sf = new SoundFile(this, soundId, soundFilename);
                files.add(sf);
            }
        }
    }

    @Override
    public String toString() {
        return "SoundGroup{" + "typeId=" + entry.getTypeId() + '}';
    }
}
