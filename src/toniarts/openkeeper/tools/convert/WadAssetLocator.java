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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.textures.enginetextures.EngineTextureEntry;
import toniarts.openkeeper.tools.convert.textures.enginetextures.EngineTexturesFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Asset locator that can load assets directly from Dungeon Keeper II WAD files.
 * This allows the game to load original assets without requiring conversion,
 * while still giving priority to extracted/converted assets for modding.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class WadAssetLocator implements AssetLocator {

    private static final Logger logger = System.getLogger(WadAssetLocator.class.getName());

    private String dungeonKeeperFolder;
    private Map<String, WadFile> wadFiles = new HashMap<>();
    private EngineTexturesFile engineTextures;

    @Override
    public void setRootPath(String rootPath) {
        this.dungeonKeeperFolder = PathUtils.fixFilePath(rootPath);
        initializeWadFiles();
    }

    @Override
    public AssetInfo locate(AssetManager manager, AssetKey key) {
        try {
            String name = key.getName();
            
            // Try to locate KMF models from Meshes.WAD
            if (name.startsWith(AssetsConverter.MODELS_FOLDER) && name.endsWith(".kmf")) {
                return locateModelFromWad(key, name);
            }
            
            // Try to locate textures from EngineTextures
            if (name.startsWith(AssetsConverter.TEXTURES_FOLDER) && name.endsWith(".png")) {
                return locateTextureFromEngineTextures(key, name);
            }
            
            // Try to locate sounds from Sounds.WAD
            if (name.startsWith(AssetsConverter.SOUNDS_FOLDER)) {
                return locateSoundFromWad(key, name);
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to locate asset from WAD: " + key.getName(), e);
        }
        
        return null;
    }

    private void initializeWadFiles() {
        if (dungeonKeeperFolder == null) {
            logger.log(Level.WARNING, "Dungeon Keeper folder not set, cannot initialize WAD files");
            return;
        }

        try {
            // Initialize Meshes.WAD for models
            Path meshesWadPath = Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "Meshes.WAD"));
            if (Files.exists(meshesWadPath)) {
                wadFiles.put("Meshes", new WadFile(meshesWadPath));
                logger.log(Level.INFO, "Initialized Meshes.WAD for direct model loading");
            }

            // Initialize Sounds.WAD for audio
            Path soundsWadPath = Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "Sounds.WAD"));
            if (Files.exists(soundsWadPath)) {
                wadFiles.put("Sounds", new WadFile(soundsWadPath));
                logger.log(Level.INFO, "Initialized Sounds.WAD for direct sound loading");
            }

            // Initialize EngineTextures for textures
            Path engineTexturesPath = Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "EngineTextures.dat"));
            if (Files.exists(engineTexturesPath)) {
                engineTextures = new EngineTexturesFile(engineTexturesPath);
                logger.log(Level.INFO, "Initialized EngineTextures.dat for direct texture loading");
            }

        } catch (IOException e) {
            logger.log(Level.ERROR, "Failed to initialize WAD files", e);
        }
    }

    private AssetInfo locateModelFromWad(AssetKey key, String name) {
        WadFile meshesWad = wadFiles.get("Meshes");
        if (meshesWad == null) {
            return null;
        }

        try {
            // Extract model name from path: Models/filename.kmf -> filename.kmf
            String modelName = name.substring(AssetsConverter.MODELS_FOLDER.length() + 1);
            
            // Check if the model exists in the WAD
            if (meshesWad.getWadFileEntries().contains(modelName)) {
                byte[] modelData = meshesWad.getFileData(modelName);
                return new WadAssetInfo(key, new ByteArrayInputStream(modelData));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load model from WAD: " + name, e);
        }

        return null;
    }

    private AssetInfo locateTextureFromEngineTextures(AssetKey key, String name) {
        if (engineTextures == null) {
            return null;
        }

        try {
            // Extract texture name from path: Textures/filename.png -> filename
            String textureName = name.substring(AssetsConverter.TEXTURES_FOLDER.length() + 1);
            textureName = textureName.substring(0, textureName.lastIndexOf('.'));
            
            // Try to get texture from EngineTextures
            EngineTextureEntry entry = engineTextures.getEntry(textureName);
            if (entry != null) {
                // Extract texture data to byte array
                byte[] textureData = extractTextureData(entry);
                if (textureData != null) {
                    return new WadAssetInfo(key, new ByteArrayInputStream(textureData));
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load texture from EngineTextures: " + name, e);
        }

        return null;
    }

    /**
     * Extract texture data from EngineTextures as a PNG byte array
     */
    private byte[] extractTextureData(EngineTextureEntry entry) {
        try {
            // Create a temporary file to extract the texture
            Path tempFile = Files.createTempFile("texture", ".png");
            try {
                Path extractedFile = engineTextures.extractFileData(entry.getName(), tempFile.getParent().toString(), true);
                if (extractedFile != null && Files.exists(extractedFile)) {
                    return Files.readAllBytes(extractedFile);
                }
            } finally {
                // Clean up temp file
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to extract texture data for: " + entry.getName(), e);
        }
        return null;
    }

    private AssetInfo locateSoundFromWad(AssetKey key, String name) {
        WadFile soundsWad = wadFiles.get("Sounds");
        if (soundsWad == null) {
            return null;
        }

        try {
            // Extract sound name from path
            String soundName = name.substring(AssetsConverter.SOUNDS_FOLDER.length() + 1);
            
            // Check if the sound exists in the WAD
            if (soundsWad.getWadFileEntries().contains(soundName)) {
                byte[] soundData = soundsWad.getFileData(soundName);
                return new WadAssetInfo(key, new ByteArrayInputStream(soundData));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load sound from WAD: " + name, e);
        }

        return null;
    }

    /**
     * Simple AssetInfo implementation for WAD-based assets
     */
    private static class WadAssetInfo extends AssetInfo {
        private final InputStream inputStream;

        public WadAssetInfo(AssetKey key, InputStream inputStream) {
            super(null, key);
            this.inputStream = inputStream;
        }

        @Override
        public InputStream openStream() {
            return inputStream;
        }
    }
}