/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.world.room.control;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.sound.BankMapFile;
import toniarts.openkeeper.tools.convert.sound.BankMapFileEntry;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.tools.convert.sound.SdtFileEntry;
import toniarts.openkeeper.tools.convert.sound.SfxMapFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Keeps the tunes playing in a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomAudioControl extends AbstractControl {

    private final Room room;
    private final BankMapFile bankMapFile;
    private final SfxMapFile sfxMapFile;
    private final List<SdtFile> sdtFiles;
    private AudioNode audioNode;
    private final AssetManager assetManager;
    private int index = 0;

    public RoomAudioControl(Room room, AssetManager assetManager) {
        this.room = room;
        this.assetManager = assetManager;

        if (room.getSoundCategory() != null && !room.getSoundCategory().isEmpty()) {
            String soundFolder = PathUtils.getDKIIFolder().concat(PathUtils.DKII_DATA_FOLDER).concat(File.separator).concat(PathUtils.DKII_SOUND_FOLDER).concat(File.separator).concat(PathUtils.DKII_SFX_FOLDER).concat(File.separator);
            String mapFolder = soundFolder.concat("Global").concat(File.separator).concat(room.getSoundCategory().toLowerCase());
            bankMapFile = new BankMapFile(new File(mapFolder.concat("BANK.map")));
            try {
                sfxMapFile = new SfxMapFile(new File(mapFolder.concat("SFX.map")));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(RoomAudioControl.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }

            // Open the SDTs ready too
            sdtFiles = new ArrayList<>(bankMapFile.getEntries().size());
            for (BankMapFileEntry entry : bankMapFile.getEntries()) {
                sdtFiles.add(new SdtFile(new File(soundFolder.concat(entry.getArchive()).concat("HD.sdt"))));
            }
        } else {
            bankMapFile = null;
            sfxMapFile = null;
            sdtFiles = null;
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (audioNode == null || audioNode.getStatus() == AudioSource.Status.Stopped) {

            // Play next
            if (sfxMapFile.getEntries()[0].entries[0].entries.length > index) {
                SfxMapFile.SfxMapEEEEntry entry = sfxMapFile.getEntries()[0].entries[0].entries[index].entries[0];
                SdtFileEntry fileEntry = sdtFiles.get(entry.archiveId - 1).getEntry(entry.index - 1);
                audioNode = new AudioNode(assetManager, "Sounds/Global/".concat(SdtFile.fixFileExtension(fileEntry, fileEntry.getName())), AudioData.DataType.Buffer);
                audioNode.setPositional(true);
                audioNode.setDirectional(true);
                audioNode.setRefDistance(sfxMapFile.getEntries()[0].minDistance);
                audioNode.setMaxDistance(sfxMapFile.getEntries()[0].maxDistance);
                audioNode.play();
                index++;
            } else {
                index = 0; // Rewind
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}
