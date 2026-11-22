/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.utils;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import static org.junit.jupiter.api.Assertions.*;

class AuthoredModelTranslationsTest {
    @Test
    void authoredAndResetLoadsDoNotContaminateEachOthersCachedOrigins() throws Exception {
        String name = "synthetic-origins-" + UUID.randomUUID();
        Path root = Path.of(AssetsConverter.getAssetsFolder());
        Path file = root.resolve("Models/" + name + ".j3o");
        Files.createDirectories(file.getParent());
        Node source = new Node("root");
        Node mesh = new Node("mesh");
        Node part = new Node("part");
        mesh.setLocalTranslation(1, 2, 3);
        part.setLocalTranslation(4, 5, 6);
        mesh.attachChild(part);
        source.attachChild(mesh);
        BinaryExporter.getInstance().save(source, file.toFile());
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(root.toString(), FileLocator.class);
        try {
            Node reset = (Node) AssetUtils.loadModel(assets, name, null);
            assertEquals(Vector3f.ZERO, reset.getChild("mesh").getLocalTranslation());
            Node authored = (Node) AssetUtils.loadModelWithAuthoredTranslations(assets, name, null);
            assertEquals(new Vector3f(1, 2, 3), authored.getChild("mesh").getLocalTranslation());
            assertEquals(new Vector3f(4, 5, 6), authored.getChild("part").getLocalTranslation());
            authored.getChild("mesh").setLocalTranslation(9, 9, 9);
            Node second = (Node) AssetUtils.loadModelWithAuthoredTranslations(assets, name, null);
            assertEquals(new Vector3f(1, 2, 3), second.getChild("mesh").getLocalTranslation());
            Node resetAgain = (Node) AssetUtils.loadModel(assets, name, null);
            assertEquals(Vector3f.ZERO, resetAgain.getChild("mesh").getLocalTranslation());
            assertEquals(Vector3f.ZERO, resetAgain.getChild("part").getLocalTranslation());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
