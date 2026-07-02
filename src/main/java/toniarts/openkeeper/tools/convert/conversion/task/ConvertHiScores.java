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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import toniarts.openkeeper.game.data.HiScores;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.hiscores.HiScoresEntry;
import toniarts.openkeeper.tools.convert.hiscores.HiScoresFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II hiscores conversion. Converts all hiscore files to our own
 * format.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ConvertHiScores extends ConversionTask {

    private static final Logger logger = System.getLogger(ConvertHiScores.class.getName());

    public ConvertHiScores(String dungeonKeeperFolder, boolean overwriteData) {
        super(dungeonKeeperFolder, null, overwriteData);
    }

    @Override
    public void internalExecuteTask() {
        convertHiScores(dungeonKeeperFolder);
    }

    /**
     * Extract and copy DK II HiScores
     *
     * @param dungeonKeeperFolder DK II main folder
     */
    private void convertHiScores(String dungeonKeeperFolder) {
        logger.log(Level.INFO, "Converting hiscores");
        updateStatus(0, 1);
        try {

            // Load the original
            Path file = Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder, "Data/Settings/HiScores.dat"));
            HiScoresFile originalHiScores = new HiScoresFile(file);

            // Convert it!
            HiScores hiScores = new HiScores();
            for (HiScoresEntry entry : originalHiScores.getHiScoresEntries()) {
                hiScores.addWithoutSaving(entry.getScore(), entry.getName(), entry.getLevel());
            }
            hiScores.save();
            updateStatus(1, 1);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Can not convert HiScores!", ex);

            // By no means fatal :D
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.HI_SCORES;
    }

}
