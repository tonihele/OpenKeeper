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
package toniarts.openkeeper.view.loader;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.effect.EffectGeometry;
import toniarts.openkeeper.view.effect.procedural.ProceduralMeshFactory;

/**
 * Loads the model of a shot, the projectile or the bolt itself. The effects
 * that go with the shot are not part of the model.
 */
public final class ShotLoader implements ILoader<ShotViewState> {

    private static final Logger logger = System.getLogger(ShotLoader.class.getName());

    private final IKwdFile kwdFile;

    public ShotLoader(IKwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    @Override
    public Spatial load(AssetManager assetManager, ShotViewState shotViewState) {
        Shot shot = kwdFile.getShotById(shotViewState.shotId);
        ArtResource meshResource = shot.getMeshResource();
        if (meshResource == null) {

            // Some shots are nothing but their effects
            return new Node(shot.getName());
        }

        try {
            return switch (meshResource.getType()) {
                case PROCEDURAL_MESH ->
                    ProceduralMeshFactory.create(meshResource, assetManager);
                case MESH, ANIMATING_MESH ->
                    AssetUtils.loadModel(assetManager, meshResource.getName(), meshResource);
                case ALPHA, ADDITIVE_ALPHA, SPRITE ->
                    createSprite(assetManager, meshResource);
                default -> {
                    logger.log(Level.WARNING, "Shot {0} has a mesh of type {1}, which is not supported",
                            shot.getName(), meshResource.getType());
                    yield null;
                }
            };
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed to load shot " + shot.getName() + "!", e);
        }

        return null;
    }

    /**
     * A sprite, like the fireball, is a picture facing the camera. Its width
     * and height are in tiles, it is centered on the shot.
     */
    private static Spatial createSprite(AssetManager assetManager, ArtResource meshResource) {
        float width = meshResource.getData(ArtResource.KEY_WIDTH);
        float height = meshResource.getData(ArtResource.KEY_HEIGHT);
        int frames = meshResource.getData(ArtResource.KEY_FRAMES);

        EffectGeometry geometry = new EffectGeometry(meshResource.getName());
        geometry.setFrames(Math.max(1, frames));
        geometry.setMaterial(AssetUtils.createParticleMaterial(meshResource, assetManager));
        geometry.setLocalScale(width, height, 1);
        geometry.setLocalTranslation(0, -height / 2, 0);

        Node result = new Node(meshResource.getName());
        result.attachChild(geometry);
        result.addControl(new BillboardControl());

        return result;
    }

}
