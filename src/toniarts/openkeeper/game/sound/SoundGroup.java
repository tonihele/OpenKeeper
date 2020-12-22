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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.sound.BankMapFile;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxEEEntry;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxGroupEntry;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxSoundEntry;
import toniarts.openkeeper.utils.PathUtils;

/**
 *
 * @author archdemon
 */
public class SoundGroup {

    private final SoundCategory category;
    private final SfxGroupEntry entry;
    private final List<SoundFile> files = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(SoundGroup.class.getName());

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

        for (SfxEEEntry eEEntry : entry.getEntries()) {
            for (SfxSoundEntry eEEEntry : eEEntry.getSounds()) {
                int archiveId = eEEEntry.getArchiveId() - 1;
                int soundId = eEEEntry.getIndex() - 1;

                String archiveFilename = ConversionUtils.convertFileSeparators(bank.getEntries()[archiveId].getName());
                SdtFile sdt = SoundCategory.getSdtFile(archiveFilename);
                if (sdt == null) {
                    throw new RuntimeException("Sdt file " + archiveFilename + " does not exist");
                }

                String relative = Paths.get(PathUtils.getDKIIFolder(), PathUtils.DKII_SFX_FOLDER)
                        .relativize(sdt.getFile()).toString();

                try {
                    String soundFilename = relative.substring(0, relative.length() - 4) + File.separator
                            + SdtFile.fixFileExtension(sdt.getEntries()[soundId]);

                    SoundFile sf = new SoundFile(this, soundId, soundFilename);
                    files.add(sf);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex, () -> {
                        return "Error in file " + sdt.getFile().toString() + " with id " + soundId;
                    });
                }
            }
        }
    }

    @Override
    public String toString() {
        return "SoundGroup{" + "typeId=" + entry.getTypeId() + '}';
    }
}
