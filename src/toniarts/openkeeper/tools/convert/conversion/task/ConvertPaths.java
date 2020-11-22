/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.tools.convert.conversion.task;

import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataEntry;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.kcs.KcsEntry;
import toniarts.openkeeper.tools.convert.kcs.KcsFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II camera path conversion. Converts all camera path files to
 * our own format.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertPaths extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertPaths.class.getName());

    public ConvertPaths(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);
    }

    @Override
    public void internalExecuteTask() {
        convertPaths(dungeonKeeperFolder, destination);
    }

    /**
     * Extract and copy DK II camera sweep files (paths)
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertPaths(String dungeonKeeperFolder, String destination) {
        LOGGER.log(Level.INFO, "Extracting paths to: {0}", destination);
        updateStatus(null, null);
        AssetUtils.deleteFolder(Paths.get(destination));

        // Paths are in the data folder, access the packed file
        WadFile wad;
        try {
            wad = new WadFile(Paths.get(ConversionUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "Paths.WAD")));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to open the Paths.wad archive!", ex);
        }
        int i = 0;
        int total = wad.getWadFileEntryCount();
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        BinaryExporter exporter = BinaryExporter.getInstance();
        for (final String entry : wad.getWadFileEntries()) {
            try {
                updateStatus(i, total);
                i++;

                // Convert all the KCS entries
                if (entry.toLowerCase().endsWith(".kcs")) {

                    // Extract each file to temp
                    Path f = wad.extractFileData(entry, tmpdir.toString());
                    f.toFile().deleteOnExit();

                    // Open the entry
                    KcsFile kcsFile = new KcsFile(f);

                    // Convert
                    List<CameraSweepDataEntry> entries = new ArrayList<>(kcsFile.getKcsEntries().size());
                    for (KcsEntry kcsEntry : kcsFile.getKcsEntries()) {

                        // Convert the rotation matrix to quatenion
                        Matrix3f mat = new Matrix3f();
                        Vector3f direction = ConversionUtils.convertVector(kcsEntry.getDirection());
                        Vector3f left = ConversionUtils.convertVector(kcsEntry.getLeft());
                        Vector3f up = ConversionUtils.convertVector(kcsEntry.getUp());
                        mat.setColumn(0, new Vector3f(-direction.x, direction.y, direction.z));
                        mat.setColumn(1, new Vector3f(left.x, -left.y, -left.z));
                        mat.setColumn(2, new Vector3f(-up.x, up.y, up.z));

                        entries.add(new CameraSweepDataEntry(ConversionUtils.convertVector(kcsEntry.getPosition()),
                                new Quaternion().fromRotationMatrix(mat), FastMath.RAD_TO_DEG * kcsEntry.getLens(),
                                kcsEntry.getNear()));
                    }
                    CameraSweepData cameraSweepData = new CameraSweepData(entries);

                    // Save it
                    exporter.save(cameraSweepData, new File(destination.concat(entry.substring(0, entry.length() - 3)).concat(CameraSweepDataLoader.FILE_EXTENSION)));
                } else if (entry.toLowerCase().endsWith(".txt")) {

                    // The text file is nice to have, it is an info text
                    wad.extractFileData(entry, destination);
                }

            } catch (Exception ex) {
                String msg = "Failed to save the path file to " + destination + "!";
                LOGGER.log(Level.SEVERE, msg, ex);
                throw new RuntimeException(msg, ex);
            }
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.PATHS;
    }

}
