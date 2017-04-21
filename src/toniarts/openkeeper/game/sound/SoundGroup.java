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
        PICKED_UP(195),
        DROPPED(196),
        SLAP(197),
        STUNNED(199),
        TICKLED(200),
        IN_HAND(201),
        HAPPY(202),
        UNHAPPY(203),
        ANGRY(204),
        EAT(205),
        SLEEP(206),
        TORTURE_GENERAL(207), // FIXME maybe not. TORTURE_WHEEL
        IMPRISON(208),
        TRAIN(211),
        PRAY(212),
        GAMBLE(213),
        CHARGE(214),
        ATTKSP(217),
        DIE(218),
        HIT_OR_MELEE(219),
        FLEE(220),
        MELEE_1(221),
        SULK(222),
        MELEE_2(224), // drop
        DRINK(225),
        SACRIFICE(226),
        HELLO(228),
        FALL(230),
        FALL_DOOR(246), // sound like door is locked. heavylump, land
        KEEP_1(265),
        CLAIM_1(266),
        CLAIM_2(267),
        REPAIR(268),
        DRAG(269),
        TORTURE_WHEEL(342),
        REPORT(762),
        CHAT(786),
        FT_1(788),
        FT_2(789),
        TORTURE_FIRE(795),
        TORTURE_ELEC(796), // TORTURE_WATER
        TRAPS_OR_CRATES_1(806),
        DOORS_WOOD(807),
        TRAPS_OR_CRATES_2(808),
        FT_3(818), // drag
        FT_4(819),
        KEEP_2(820),
        IMPENETRABLE(821),
        STINKY(822),
        WATER_SWIM(823),
        REINFORCE(829),
        TORCH_LOOP(832),
        TELEPORT_IN(834),
        TELEPORT_OUT(835),
        LEVEL_UP(841);

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
}
