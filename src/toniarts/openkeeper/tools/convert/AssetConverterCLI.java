/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.tools.convert;

import com.jme3.asset.AssetManager;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import toniarts.openkeeper.tools.convert.conversion.ConversionTaskManager;
import toniarts.openkeeper.tools.convert.conversion.task.*;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.SettingUtils;

/**
 * Standalone CLI tool for converting Dungeon Keeper II assets.
 * This replaces the GUI-based conversion that was integrated into game startup.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class AssetConverterCLI {

    private static final Logger logger = System.getLogger(AssetConverterCLI.class.getName());

    private AssetConverterCLI() {
        // Utility class
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            CLIArgs cliArgs = parseArguments(args);
            
            if (cliArgs.showHelp) {
                showHelp();
                return;
            }

            // Validate DK II folder
            String dkFolder = cliArgs.dungeonKeeperFolder != null ? 
                PathUtils.fixFilePath(cliArgs.dungeonKeeperFolder) : 
                PathUtils.getDKIIFolder();
                
            if (dkFolder == null || !PathUtils.checkDkFolder(dkFolder)) {
                logger.log(Level.ERROR, "Invalid Dungeon Keeper II folder: {0}", dkFolder);
                System.exit(1);
            }

            // Initialize settings
            initSettings();

            // Create asset manager
            AssetManager assetManager = JmeSystem.newAssetManager(
                Thread.currentThread().getContextClassLoader()
                    .getResource("com/jme3/asset/Desktop.cfg"));

            // Perform conversion
            boolean success = convertAssets(dkFolder, assetManager, cliArgs);
            
            if (success) {
                logger.log(Level.INFO, "Asset conversion completed successfully!");
                System.exit(0);
            } else {
                logger.log(Level.ERROR, "Asset conversion failed!");
                System.exit(1);
            }
            
        } catch (Exception e) {
            logger.log(Level.ERROR, "Unexpected error during conversion", e);
            System.exit(1);
        }
    }

    private static CLIArgs parseArguments(String[] args) {
        CLIArgs cliArgs = new CLIArgs();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    cliArgs.showHelp = true;
                    break;
                case "-d":
                case "--dk-folder":
                    if (i + 1 < args.length) {
                        cliArgs.dungeonKeeperFolder = args[++i];
                    } else {
                        throw new IllegalArgumentException("Missing value for " + args[i]);
                    }
                    break;
                case "-f":
                case "--force":
                    cliArgs.overwrite = true;
                    break;
                case "--skip-models":
                    cliArgs.skipModels = true;
                    break;
                default:
                    if (args[i].startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + args[i]);
                    }
                    break;
            }
        }
        
        return cliArgs;
    }

    private static void showHelp() {
        System.out.println("OpenKeeper Asset Converter CLI");
        System.out.println();
        System.out.println("Usage: java -cp openkeeper.jar toniarts.openkeeper.tools.convert.AssetConverterCLI [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help           Show this help message");
        System.out.println("  -d, --dk-folder      Path to Dungeon Keeper II installation folder");
        System.out.println("  -f, --force          Force overwrite existing converted assets");
        System.out.println("  --skip-models        Skip model conversion (models will be loaded directly from .wad)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Convert all assets");
        System.out.println("  java -cp openkeeper.jar toniarts.openkeeper.tools.convert.AssetConverterCLI");
        System.out.println();
        System.out.println("  # Convert with custom DK II folder");
        System.out.println("  java -cp openkeeper.jar toniarts.openkeeper.tools.convert.AssetConverterCLI -d \"/path/to/dk2\"");
        System.out.println();
        System.out.println("  # Force overwrite and skip models");
        System.out.println("  java -cp openkeeper.jar toniarts.openkeeper.tools.convert.AssetConverterCLI -f --skip-models");
    }

    private static void initSettings() {
        // Initialize OpenKeeper settings
        SettingUtils.getInstance();
    }

    private static boolean convertAssets(String dungeonKeeperFolder, AssetManager assetManager, CLIArgs cliArgs) {
        logger.log(Level.INFO, "Starting asset conversion from: {0}", dungeonKeeperFolder);
        logger.log(Level.INFO, "Destination folder: {0}", AssetsConverter.getAssetsFolder());

        // Create destination folder
        try {
            Files.createDirectories(Paths.get(AssetsConverter.getAssetsFolder()));
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed to create destination folder", e);
            return false;
        }

        // Get conversion processes needed
        AppSettings settings = SettingUtils.getInstance().getSettings();
        List<AssetsConverter.ConvertProcess> processes = AssetsConverter.getConversionNeeded(settings);
        
        if (processes.isEmpty()) {
            logger.log(Level.INFO, "No conversion needed, all assets are up to date");
            return true;
        }

        logger.log(Level.INFO, "Need to convert {0} asset types", processes.size());

        // Create conversion task manager
        ConversionTaskManager taskManager = new ConversionTaskManager();

        // Add tasks based on processes needed
        for (AssetsConverter.ConvertProcess process : processes) {
            // Skip models if requested
            if (cliArgs.skipModels && process == AssetsConverter.ConvertProcess.MODELS) {
                logger.log(Level.INFO, "Skipping model conversion as requested");
                continue;
            }

            switch (process) {
                case TEXTURES:
                    taskManager.addTask(process, 
                        new ConvertTextures(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.TEXTURES_FOLDER, cliArgs.overwrite),
                        true);
                    break;
                case MODELS:
                    taskManager.addTask(process,
                        new ConvertModels(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.MODELS_FOLDER, cliArgs.overwrite, assetManager),
                        true);
                    break;
                case MOUSE_CURSORS:
                    taskManager.addTask(process,
                        new ConvertMouseCursors(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.MOUSE_CURSORS_FOLDER, cliArgs.overwrite),
                        true);
                    break;
                case MUSIC_AND_SOUNDS:
                    taskManager.addTask(process,
                        new ConvertSounds(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.SOUNDS_FOLDER, cliArgs.overwrite),
                        true);
                    break;
                case INTERFACE_TEXTS:
                    taskManager.addTask(process,
                        new ConvertTexts(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.INTERFACE_TEXTS_FOLDER, cliArgs.overwrite),
                        true);
                    break;
                case PATHS:
                    taskManager.addTask(process,
                        new ConvertPaths(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.PATHS_FOLDER, cliArgs.overwrite),
                        true);
                    break;
                case HI_SCORES:
                    taskManager.addTask(process,
                        new ConvertHiScores(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.HI_SCORES_FOLDER, cliArgs.overwrite),
                        true);
                    break;
                case FONTS:
                    taskManager.addTask(process,
                        new ConvertFonts(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.FONTS_FOLDER, cliArgs.overwrite),
                        true);
                    break;
                case MAP_THUMBNAILS:
                    taskManager.addTask(process,
                        new ConvertMapThumbnails(dungeonKeeperFolder, AssetsConverter.getAssetsFolder() + AssetsConverter.MAP_THUMBNAILS_FOLDER, cliArgs.overwrite),
                        true);
                    break;
            }
        }

        // Execute conversion
        boolean success = taskManager.executeTasks();
        
        if (success) {
            // Update settings to mark conversion as complete
            for (AssetsConverter.ConvertProcess process : processes) {
                if (cliArgs.skipModels && process == AssetsConverter.ConvertProcess.MODELS) {
                    continue;
                }
                settings.putInteger(process.getSettingName(), process.getVersion());
            }
            SettingUtils.getInstance().saveSettings();
        }

        return success;
    }

    private static class CLIArgs {
        boolean showHelp = false;
        String dungeonKeeperFolder = null;
        boolean overwrite = false;
        boolean skipModels = false;
    }
}