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
package toniarts.openkeeper.post;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.Vector4f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.util.ArrayList;

/**
 * Enhanced Subpixel Morphological Anti-Aliasing (SMAA) post filter, configured
 * for the official <b>ULTRA</b> preset (threshold 0.05, 32/16 search steps,
 * corner rounding 25) with colour-edge detection for chroma-aware edges.
 * <p>
 * Two intermediate FBOs (edge detect → blend weights) then neighborhood blend
 * as the filter's final pass. Installed last by {@link PostProcessPipeline}
 * (after {@link TranslucentBucketStage} and optional ambient occlusion) on the
 * LDR colour buffer. OpenKeeper has no HDR tone-map stage; gamma correction
 * remains an AppSettings / framebuffer concern.
 * <p>
 * SMAA library © Jorge Jimenez et al. (see {@code SMAALib.glsllib} header).
 * jME multi-pass wiring adapted from TheToucher's SMAA prototype.
 */
public final class SmaaFilter extends Filter {

    private static final String AREA_TEX = "Textures/SMAA/AreaTexDX10.dds";
    private static final String SEARCH_TEX = "Textures/SMAA/SearchTex.dds";

    private Material edgeMaterial;
    private Material blendMaterial;
    private Pass edgePass;
    private Pass blendPass;
    private final Vector4f rtMetrics = new Vector4f();
    private int screenWidth;
    private int screenHeight;

    public SmaaFilter() {
        super("SMAA Ultra");
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        this.screenWidth = Math.max(1, w);
        this.screenHeight = Math.max(1, h);
        updateRtMetrics();

        postRenderPasses = new ArrayList<>();

        edgeMaterial = new Material(manager, "MatDefs/Post/SMAA/SMAA_EdgeDetection.j3md");
        edgeMaterial.setVector4("RtMetrics", rtMetrics);
        edgePass = new Pass() {
            @Override
            public boolean requiresSceneAsTexture() {
                return true;
            }
        };
        // Edges must clear to black (incl. alpha) every frame — SMAA contract
        // (FilterPostProcessor clears each pass FBO before rendering).
        edgePass.init(renderManager.getRenderer(), screenWidth, screenHeight,
                Image.Format.RGBA8, Image.Format.Depth, 1, edgeMaterial);
        configureColorTarget(edgePass.getRenderedTexture(), Texture.MagFilter.Bilinear, Texture.MinFilter.BilinearNoMipMaps);
        postRenderPasses.add(edgePass);

        blendMaterial = new Material(manager, "MatDefs/Post/SMAA/SMAA_BlendingWeightCalc.j3md");
        blendMaterial.setVector4("RtMetrics", rtMetrics);
        Texture areaTex = loadLookup(manager, AREA_TEX, Texture.MagFilter.Bilinear, Texture.MinFilter.BilinearNoMipMaps);
        Texture searchTex = loadLookup(manager, SEARCH_TEX, Texture.MagFilter.Nearest, Texture.MinFilter.NearestNoMipMaps);
        blendMaterial.setTexture("AreaTexture", areaTex);
        blendMaterial.setTexture("SearchTexture", searchTex);
        blendPass = new Pass() {
            @Override
            public boolean requiresSceneAsTexture() {
                return false;
            }

            @Override
            public void beforeRender() {
                blendMaterial.setTexture("EdgesTexture", edgePass.getRenderedTexture());
            }
        };
        blendPass.init(renderManager.getRenderer(), screenWidth, screenHeight,
                Image.Format.RGBA8, Image.Format.Depth, 1, blendMaterial);
        configureColorTarget(blendPass.getRenderedTexture(), Texture.MagFilter.Bilinear, Texture.MinFilter.BilinearNoMipMaps);
        postRenderPasses.add(blendPass);

        // Neighborhood blend is the filter output — avoids a third intermediate
        // FBO plus a 1:1 Overlay blit of the same result.
        material = new Material(manager, "MatDefs/Post/SMAA/SMAA_NeighborhoodBlending.j3md");
        material.setVector4("RtMetrics", rtMetrics);
    }

    private static Texture loadLookup(AssetManager manager, String path,
            Texture.MagFilter mag, Texture.MinFilter min) {
        Texture texture = manager.loadTexture(new TextureKey(path, false));
        texture.setMagFilter(mag);
        texture.setMinFilter(min);
        texture.setWrap(Texture.WrapMode.EdgeClamp);
        return texture;
    }

    private static void configureColorTarget(Texture texture, Texture.MagFilter mag, Texture.MinFilter min) {
        texture.setMagFilter(mag);
        texture.setMinFilter(min);
        texture.setWrap(Texture.WrapMode.EdgeClamp);
    }

    private void updateRtMetrics() {
        rtMetrics.set(1f / screenWidth, 1f / screenHeight, screenWidth, screenHeight);
        if (edgeMaterial != null) {
            edgeMaterial.setVector4("RtMetrics", rtMetrics);
        }
        if (blendMaterial != null) {
            blendMaterial.setVector4("RtMetrics", rtMetrics);
        }
        if (material != null) {
            material.setVector4("RtMetrics", rtMetrics);
        }
    }

    @Override
    protected void preFrame(float tpf) {
        // Ensure metrics track reshape / resolution changes from AppSettings.
        updateRtMetrics();
        if (material != null && blendPass != null) {
            material.setTexture("WeightsTexture", blendPass.getRenderedTexture());
        }
    }

    @Override
    protected boolean isRequiresSceneTexture() {
        return true;
    }

    @Override
    protected boolean isRequiresBilinear() {
        // Colour edge detection samples neighbours with linear filtering.
        return true;
    }

    @Override
    protected Image.Format getDefaultPassTextureFormat() {
        return Image.Format.RGBA8;
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    /**
     * Called by the filter post processor when the viewport is resized.
     * Materials keep the live {@link #rtMetrics} vector reference, so updating
     * it is enough; passes are recreated by jME on reshape via re-init.
     */
    public void setResolution(int width, int height) {
        this.screenWidth = Math.max(1, width);
        this.screenHeight = Math.max(1, height);
        updateRtMetrics();
    }
}
