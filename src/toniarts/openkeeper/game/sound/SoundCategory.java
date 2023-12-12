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
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.tools.convert.sound.BankMapFile;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxGroupEntry;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxMapFile;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxMapFileEntry;
import toniarts.openkeeper.utils.PathUtils;

/**
 *
 * @author archdemon
 */
public class SoundCategory {
    
    private static final Logger LOGGER = System.getLogger(SoundCategory.class.getName());

    public static final String SPEECH_MENTOR = "SPEECH_MENTOR";
    public static final String SPEECH_MULTIPLAYER = "SPEECH_MULTIPLAYER";
    public static final String SPEECH_HORNY = "SPEECH_HORNY";
    public static final String SPEECH_MYPETDUNGEON = "SPEECH_MYPETDUNGEON";

    private final String folder;
    private final String name;
    private final Map<Integer, SoundGroup> groups = new HashMap<>();

    public SoundCategory(String name, boolean useGlobal) {
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Sound category is empty");
        }

        if (useGlobal) {
            this.folder = PathUtils.DKII_SFX_GLOBAL_FOLDER;
        } else {
            this.folder = PathUtils.DKII_SFX_FOLDER + name.toLowerCase() + File.separator;
        }

        this.name = name;
        parseGroups();
    }

    public String getName() {
        return name;
    }

    public Map<Integer, SoundGroup> getGroups() {
        return groups;
    }

    @Nullable
    public SoundGroup getGroup(int id) {
        return groups.get(id);
    }

    @Nullable
    public SoundGroup getGroup(IIndexable type) {
        return groups.get((int) type.getId());
    }

    public boolean hasGroup(int id) {
        return groups.containsKey(id);
    }

    @Nullable
    public SfxMapFile getSfxMapFile() {
        try {
            Path f = Paths.get(PathUtils.getRealFileName(PathUtils.getDKIIFolder(), folder + name.toLowerCase() + "SFX.map"));

            return new SfxMapFile(f);
        } catch (IOException ex) {
            LOGGER.log(Level.ERROR, () -> {
                return String.format("Sfx file of category {0} does not exist", name);
            }, ex);
        }

        return null;
    }

    public BankMapFile getBankMapFile() {
        try {
            Path f = Paths.get(PathUtils.getRealFileName(PathUtils.getDKIIFolder(), folder + name.toLowerCase() + "BANK.map"));

            return new BankMapFile(f);
        } catch (IOException ex) {
            throw new RuntimeException("Bank file of category " + name + " does not exist", ex);
        }
    }

    /**
     * Get SdtFile
     *
     * @param archiveFilename name in BankMapFile
     * @return SdtFile
     */
    @Nullable
    public static SdtFile getSdtFile(String archiveFilename) {
        // FIXME I don`t know what better HD or HW, but quantity HW less than HD, but size HW more than HD
        for (String part : new String[]{"HD.sdt", "HW.sdt"}) {
            try {
                Path f = Paths.get(PathUtils.getRealFileName(PathUtils.getDKIIFolder(),
                        PathUtils.DKII_SFX_FOLDER + archiveFilename + part));
                if (Files.exists(f)) {
                    return new SdtFile(f);
                }
            } catch (IOException ex) {
                // nop
            }
        }

        LOGGER.log(Level.WARNING, "SDT File archive {0} not found", archiveFilename);
        return null;
    }

    private void parseGroups() {
        SfxMapFile sfx = getSfxMapFile();
        if (sfx == null) {
            return;
        }

        for (SfxMapFileEntry entry : sfx.getEntries()) {
            for (SfxGroupEntry eEntry : entry.getGroups()) {
                // take action Index
                int actionId = eEntry.getTypeId();
                if (!groups.containsKey(actionId)) {
                    SoundGroup action = new SoundGroup(this, eEntry);
                    groups.put(actionId, action);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "SoundCategory{" + "folder=" + folder + ", name=" + name + '}';
    }
}
