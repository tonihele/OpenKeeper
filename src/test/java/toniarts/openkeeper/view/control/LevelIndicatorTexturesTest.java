/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view.control;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import com.jme3.texture.image.ColorSpace;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.tools.convert.KmfModelLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The My Pet Dungeon level indicator shows the level's number when it stops
 */
class LevelIndicatorTexturesTest {

    private static final List<String> NUMBERS = List.of("Textures/MPD1.png", "Textures/MPD10.png",
            "Textures/MPD2.png", "Textures/MPDnumber_blur1.png", "Textures/MPDnumber_blur2.png");

    @TempDir
    Path directory;

    @Test
    void numbersStopOnTheLevel() throws Exception {
        DesktopAssetManager assetManager = assetManager();
        Geometry numbers = geometry(assetManager, NUMBERS);
        Node model = new Node();
        model.attachChild(numbers);

        LevelIndicatorTextures.apply(model, 10, assetManager);

        Material material = numbers.getMaterial();
        assertEquals("Textures/MPD10.png", material.getTextureParam("DiffuseMap").getTextureValue().getKey().getName());
        assertEquals(1, (int) material.getParamValue("NumberOfTiles"));
        assertEquals(0, (int) material.getParamValue("Speed"));
    }

    @Test
    void otherAnimatedTexturesAreLeftAlone() throws Exception {
        DesktopAssetManager assetManager = assetManager();
        Geometry other = geometry(assetManager, List.of("Textures/Water1.png", "Textures/Water2.png"));
        Node model = new Node();
        model.attachChild(other);

        LevelIndicatorTextures.apply(model, 1, assetManager);

        assertNull(other.getMaterial().getTextureParam("DiffuseMap"));
        assertEquals(30, (int) other.getMaterial().getParamValue("NumberOfTiles"));
    }

    private DesktopAssetManager assetManager() throws Exception {
        for (String texture : NUMBERS) {
            Path file = directory.resolve(texture);
            Files.createDirectories(file.getParent());
            ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), "png", file.toFile());
        }
        DesktopAssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator(directory.toString(), FileLocator.class);
        return assetManager;
    }

    /**
     * A geometry with an animated sprite material, as the KMF loader and
     * AssetUtils make them
     */
    private static Geometry geometry(DesktopAssetManager assetManager, List<String> textures) {
        MaterialDef materialDef = new MaterialDef(assetManager, "Sprite");
        materialDef.addMaterialParam(VarType.Int, "NumberOfTiles", 1);
        materialDef.addMaterialParam(VarType.Int, "Speed", null);
        materialDef.addMaterialParamTexture(VarType.Texture2D, "DiffuseMap", ColorSpace.sRGB, null);
        Material material = new Material(materialDef);
        material.setInt("NumberOfTiles", 30);
        material.setInt("Speed", 10);

        Geometry geometry = new Geometry("numbers", new Quad(1, 1));
        geometry.setMaterial(material);
        geometry.setUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES, new ArrayList<>(textures));
        return geometry;
    }
}
