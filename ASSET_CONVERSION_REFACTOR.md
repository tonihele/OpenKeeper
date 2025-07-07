# Asset Conversion Refactoring

This document describes the major refactoring of OpenKeeper's asset conversion and management system.

## Overview

The asset conversion system has been refactored to:

1. **Move conversion out of game startup** - Asset conversion is now a separate CLI tool
2. **Enable direct WAD file loading** - Assets can be loaded directly from original DK II files
3. **Stop generating .j3o models** - Raw KMF files are extracted instead
4. **Maintain modding support** - Extracted files still take priority over WAD files

## New Asset Loading Priority

1. **Extracted/converted assets** (highest priority) - Located in the assets folder
2. **Original WAD files** (fallback) - Loaded directly from DK II installation
3. **Built-in defaults** (lowest priority) - Embedded in the JAR

## Asset Converter CLI

### Usage

The standalone asset converter can be run with:

```bash
# Using Gradle
./gradlew assetConverterCLI

# With arguments
./gradlew assetConverterCLI -PcliArgs="-d /path/to/dk2 -f"

# Direct Java execution
java -cp build/libs/openkeeper.jar toniarts.openkeeper.tools.convert.AssetConverterCLI [options]
```

### Command Line Options

- `-h, --help` - Show help message
- `-d, --dk-folder` - Path to Dungeon Keeper II installation folder
- `-f, --force` - Force overwrite existing converted assets  
- `--skip-models` - Skip model conversion (models will be loaded directly from WAD)

### Examples

```bash
# Convert all assets
./gradlew assetConverterCLI

# Convert with custom DK II folder
./gradlew assetConverterCLI -PcliArgs="-d /opt/games/dk2"

# Force overwrite and skip models
./gradlew assetConverterCLI -PcliArgs="-f --skip-models"
```

## Benefits

### For Users
- **Faster game startup** - No conversion delay when starting the game
- **Smaller disk usage** - Can skip model conversion and load directly from WAD
- **Better cross-platform support** - Especially important for Android builds

### For Developers  
- **Cleaner separation** - Asset conversion is independent of game logic
- **Easier testing** - Can test game without full asset conversion
- **Modding flexibility** - Easy to replace individual assets while keeping others original

## Technical Changes

### Files Modified

1. **Main.java** 
   - Removed asset conversion dependency from startup
   - Added WadAssetLocator registration
   - Added KmfModelLoader registration

2. **ConvertModels.java**
   - Removed .j3o model generation  
   - Extract raw KMF files instead
   - Maintains backward compatibility

3. **build.gradle**
   - Added assetConverterCLI Gradle task

### Files Added

1. **AssetConverterCLI.java** - Standalone command-line asset converter
2. **WadAssetLocator.java** - Asset locator for direct WAD file access

## Migration Guide

### Existing Users

No action required. The game will automatically load assets using the new priority system:
- If you have extracted assets, they'll be used
- If not, assets will load directly from your DK II installation

### For Fresh Installs

1. Install OpenKeeper
2. Set your DK II folder in the game settings  
3. Optionally run the asset converter CLI for faster loading:
   ```bash
   ./gradlew assetConverterCLI
   ```

### For Android/Mobile Builds

The asset converter CLI enables building for Android without requiring asset conversion on the target device:

1. Run asset conversion on development machine
2. Include converted assets in the Android build
3. No DK II installation required on mobile device

## Asset Types Supported

| Asset Type | Direct WAD Loading | Conversion Available |
|------------|-------------------|---------------------|
| Models (KMF) | ✅ Meshes.WAD | ✅ Raw KMF files |
| Textures | ✅ EngineTextures.dat | ✅ PNG files |
| Sounds | ✅ Sounds.WAD | ✅ Converted audio |
| Interface | ❌ | ✅ Required |
| Maps | ❌ | ✅ Required |

## Troubleshooting

### Models not loading
- Ensure DK II folder is correctly set
- Check that Meshes.WAD exists in the Data folder
- Try running the asset converter CLI

### Textures missing
- Verify EngineTextures.dat exists in the Data folder
- Some textures may require conversion - run the CLI

### Performance issues
- Consider running the asset converter CLI for better performance
- Converted assets load faster than direct WAD access