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
package toniarts.openkeeper.view.effect;

import com.jme3.animation.AnimControl;
import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.tools.convert.map.ArtResource.ArtResourceType;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Color;
import toniarts.openkeeper.utils.MapThumbnailGenerator;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.*;
import java.util.Map.Entry;

/**
 * An effect & its elements, or a tree of effects & their elements. The logic is
 * quite here now, how the effects are chained and what they do, but:<br>
 * TODO
 * <ul>
 * <li>We probably need our own particle emitter, the stock wont probably do..
 * Just pass the Effect & EffectElement to our custom one and boom</li>
 * <li>Maybe cache the emitters?</li>
 * </ul>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class VisualEffect {
    
    private static final Logger logger = System.getLogger(VisualEffect.class.getName());

    /**
     * Conversion from the file's mass unit (float32, 4096 = 1.0) to
     * tiles/s^2, derived from the effect clock (20 Hz) and the position vs.
     * velocity fixed-point precision difference (16x)
     */
    private static final float GRAVITY_FACTOR = 25f;

    private final Effect effect;
    // Keyed by the spawned spatial (unique per instance), not the shared
    // EffectElement definition -- a burst spawns elementsPerTurn separate
    // instances of the same element id, which would otherwise collide as
    // map keys and orphan all but the last one (never cleaned up).
    private final Map<Spatial, EffectElement> effectElements;
    private final List<VisualEffect> effects;
    private final Node effectNode;
    private final IKwdFile kwdFile;
    private final AssetManager assetManager;
    private final EffectManagerState effectManagerState;
    private final short ownerId;
    private boolean infinite;
    private PointLight light;
    // Populated only for a MESH_COLLECTION effect:
    // one entry per part in the .kmf group, read once at load() and spawned
    // as debris regardless of the effect's own generation flags.
    private List<MeshCollectionPart> meshCollectionParts;
    private boolean deathEffectSpawned;

    private record MeshCollectionPart(String name, Vector3f offset) {
    }

    public VisualEffect(EffectManagerState effectManagerState, Node node, Effect effect) {
        this(effectManagerState, node, null, effect, false, Player.NEUTRAL_PLAYER_ID);
    }

    public VisualEffect(EffectManagerState effectManagerState, Node node, Vector3f location, Effect effect, boolean infinite) {
        this(effectManagerState, node, location, effect, infinite, Player.NEUTRAL_PLAYER_ID);
    }

    public VisualEffect(EffectManagerState effectManagerState, Node node, Vector3f location, Effect effect, boolean infinite, short ownerId) {
        this.effect = effect;
        this.kwdFile = effectManagerState.getKwdFile();
        this.assetManager = effectManagerState.getAssetManger();
        this.effectManagerState = effectManagerState;
        this.infinite = infinite;
        this.ownerId = ownerId;

        // Create the lists
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) {
            effects = new ArrayList<>();
            effectElements = HashMap.newHashMap(effect.getGenerateIds().size());
        } else {
            effects = new ArrayList<>(effect.getGenerateIds().size());
            effectElements = Collections.emptyMap();
        }

        // Attach to scene graph
        effectNode = new Node(effect.getName());
        if (location != null) {
            effectNode.setLocalTranslation(location);
        }
        node.attachChild(effectNode); // We need to attach before emiting, it doesn't work otherwise

        // Load the effect
        load();
    }

    private void load() {
        ArtResource resource = effect.getArtResource();

        Spatial model = new Node();
        if (resource != null) {
            model = createEffectModel(resource);
            model.addControl(createEffectControl());
            effectNode.attachChild(model);

            if (resource.getType() == ArtResourceType.MESH_COLLECTION) {
                meshCollectionParts = loadMeshCollectionParts(resource);
            }
        }

        // Light
        light = getLight(effect.getLight());
        if (light != null) {
            model.addLight(light);
        }

        // Elements/effects
        generateChildren();

        // A MESH_COLLECTION's parts break off regardless of the effect's own
        // generation flags
        if (meshCollectionParts != null) {
            generateMeshCollectionParts();
        }

        // The next effect is chaining the effects, they'll start immediately
        // TODO: probably start this after the effect is through!
        if (effect.getNextEffectId() != 0) {
            addEffect(effect.getNextEffectId(), null);
        }
    }

    private Spatial createEffectModel(ArtResource resource) {
        Spatial model = new Node();
        switch (resource.getType()) {
            case MESH:
            case ANIMATING_MESH:
                model = AssetUtils.loadModel(assetManager, resource.getName(), resource);
                break;

            case PROCEDURAL_MESH:
                model = AssetUtils.createProceduralMesh(resource);
                break;

            case MESH_COLLECTION:
                // The group's own art is never rendered - only the parts
                // spawned by generateMeshCollectionParts() are
                break;

            case ALPHA:
            case ADDITIVE_ALPHA:
            case SPRITE:
                EffectGeometry g = new EffectGeometry("effect", resource.getFlags().contains(ArtResource.ArtResourceFlag.FLAT));
                g.setFrames(Math.max(1, resource.getData(ArtResource.KEY_FRAMES)));

                Material material = AssetUtils.createParticleMaterial(resource, assetManager);
                g.setMaterial(material);

                ((Node) model).attachChild(g);
                break;

            default:
                logger.log(Level.WARNING, "Not supported effect type {0}", resource.getType());
        }

        applyMeshScaleOrAnimation(model, resource);
        return model;
    }

    /**
     * Applies the fixed-point mesh scale for a static {@code MESH}, or
     * enables the baked animation for an {@code ANIMATING_MESH}. Shared by
     * both the top-level effect model and generated mesh effect elements.
     */
    private void applyMeshScaleOrAnimation(Spatial model, ArtResource resource) {
        if (resource.getType() == ArtResourceType.MESH) {
            model.setLocalScale(resource.getData(ArtResource.KEY_SCALE));

        } else if (resource.getType() == ArtResourceType.ANIMATING_MESH) {

             AnimControl animControl = (AnimControl) model.getControl(AnimControl.class);
            if (animControl != null) {
//                    AnimChannel channel = animControl.getChannel(0);
//                    channel.setAnim(ANIM_NAME);
//                    resource.getData(ArtResource.KEY_FPS);
//                    resource.getData(ArtResource.KEY_FRAMES);
//                    channel.setSpeed(speed);
//                    channel.setTime(time);
                animControl.setEnabled(true);
            }
        }
    }

    private EffectControl createEffectControl() {
        return new EffectControl(effect) {

            @Override
            public void onDie(Vector3f location) {
                if (effect.getDeathEffectId() != 0) {
                    VisualEffect.this.addEffect(effect.getDeathEffectId(), location);
                }
            }

            @Override
            public void onHit(Vector3f location) {
                handleEffectHit(location);
            }
        };
    }

    private void handleEffectHit(Vector3f location) {
        IMapTileInformation tile = effectManagerState.getPlayerMapViewState().getMapInformation().getMapData().getTile(WorldUtils.vectorToPoint(location));
        if (tile == null) {
            logger.log(Level.WARNING, "Effect hit error");
            return;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)
                && effect.getHitLavaEffectId() != 0) {
            addEffect(effect.getHitLavaEffectId(), location);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)
                && effect.getHitWaterEffectId() != 0) {
            addEffect(effect.getHitWaterEffectId(), location);
        } else if (effect.getHitSolidEffectId() != 0) {
            // && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            if (effect.getFlags().contains(Effect.EffectFlag.DIE_WHEN_HIT_SOLID)) {
                if (effect.getDeathEffectId() != 0) {
                    addEffect(effect.getDeathEffectId(), location);
                }
            } else {
                addEffect(effect.getHitSolidEffectId(), location);
            }
        }
    }

    /**
     * One call per id: loadElement() already builds an emitter sized for
     * elementsPerTurn instances (ParticleEmitter's particle pool, or
     * EffectEmitter.emitAllParticles()'s own internal loop) - looping
     * elementsPerTurn times here too would square the count. For CUBE_GEN,
     * each of those instances rolls its own spot in the annulus/height band
     * (EffectEmitter.spawnOne() / EmitterCubeGenShape), so the container
     * itself stays at the effect's own origin instead of every instance in
     * the burst stacking on one shared random point.
     */
    private void generateChildren() {
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) {
            boolean cubeGen = effect.getGenerationType() == Effect.GenerationType.CUBE_GEN;
            for (Integer id : effect.getGenerateIds()) {
                addEffectElement(id, cubeGen ? null : randomGenerationOffset());
            }
        } else if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECTS)) {
            for (Integer id : effect.getGenerateIds()) {
                addEffect(id, randomGenerationOffset());
            }
        }
    }

    /**
     * A random offset within the effect's spawn annulus/height band
     * ({@code innerOriginRange}/{@code outerOriginRange},
     * {@code lowerHeightLimit}/{@code upperHeightLimit}), used to spread
     * generated elements/effects around the emission point instead of
     * stacking them all at the same spot.
     */
    private Vector3f randomGenerationOffset() {
        return EffectControl.randomOriginOffset(effect);
    }

    private void addEffect(Integer id, Vector3f location) {
        if (id.equals(0)) {
            return;
        }

        VisualEffect visualEffect = new VisualEffect(effectManagerState, effectNode, location, kwdFile.getEffect(id), false, ownerId);
        effects.add(visualEffect);
        effectNode.attachChild(visualEffect.effectNode);
    }

    private void addEffectElement(Integer id, Vector3f location) {
        if (id.equals(0)) {
            return;
        }

        EffectElement effectElement = kwdFile.getEffectElement(id);
        Spatial emitter = loadElement(effectElement);
        if (emitter != null) {
            if (location != null) {
                emitter.setLocalTranslation(location);
            }
            effectElements.put(emitter, effectElement);
            effectNode.attachChild(emitter);
            if (emitter instanceof ParticleEmitter particleEmitter) {
                particleEmitter.emitAllParticles();
            } else if (emitter instanceof EffectEmitter effectEmitter) {
                effectEmitter.emitAllParticles();
            }
        }

        // The next effect is chaining the effects, they'll start immediately
        if (effectElement.getNextEffectId() != 0) {
            addEffect(effectElement.getNextEffectId(), null);
        }
    }

    private Spatial loadElement(EffectElement element) {
        ArtResource resource = element.getArtResource();

        if (effect.getGenerationType() == Effect.GenerationType.NONE || resource == null) {
            return null;
        }

        switch (resource.getType()) {
            case ALPHA:
            case ADDITIVE_ALPHA:
            case SPRITE:
                return createParticleElement(element, resource);

            case MESH:
            case ANIMATING_MESH:
            case PROCEDURAL_MESH:
                return createMeshElement(element, resource);

            default:
                logger.log(Level.WARNING, "Not supported effect element type {0}", resource.getType());
        }

        return null;
    }

    private ParticleEmitter createParticleElement(EffectElement element, ArtResource resource) {
        ParticleEmitter emitter = new EffectParticleEmitter(element.getName(),
                ParticleMesh.Type.Triangle,
                effect.getElementsPerTurn(),
                element.getAirFriction(),
                element.getElasticity(),
                element.getFlags().contains(EffectElement.EffectElementFlag.DIRECTIONAL_FRICTION),
                effect.getWhirlpoolRate());
        if (effect.getGenerationType() == Effect.GenerationType.CUBE_GEN) {
            // Scatter each particle's spawn point across the annulus/
            // height band instead of jME3's default emission point
            emitter.setShape(new EmitterCubeGenShape(effect));
        }
        configureParticleEmissionRate(emitter, element);

        Material material = AssetUtils.createParticleMaterial(resource, assetManager);
        emitter.setMaterial(material);
        emitter.setImagesX(Math.max(1, resource.getData(ArtResource.KEY_FRAMES)));
        emitter.setImagesY(1);
        emitter.setSelectRandomImage(resource.getFlags().contains(ArtResource.ArtResourceFlag.RANDOM_START_FRAME));
        emitter.setInWorldSpace(false);

        applyParticleColor(emitter, element);
        //
        // Every particle draws its own independent velocity sample from the
        // full minSpeedXy/maxSpeedXy/minSpeedYz/maxSpeedYz range, instead of
        // jME3's default of sharing one vector across the whole burst and
        // only lightly varying it (see EffectParticleInfluencer).
        emitter.setParticleInfluencer(new EffectParticleInfluencer(element));
        //
        applyParticleScale(emitter, element);
        //
        boolean flat = resource.getFlags().contains(ArtResource.ArtResourceFlag.FLAT);
        if (flat) {
            // Lies flat on the floor (e.g. a decal/splat) instead of the
            // default camera-facing billboard - takes priority over
            // ROTATE_TO_MOVEMENT_DIRECTION, since jME3's ParticleTriMesh
            // checks facingVelocity before faceNormal and would otherwise
            // silently ignore the latter.
            emitter.setFaceNormal(new Vector3f(0, 1, 0));
        }
        emitter.setFacingVelocity(!flat && element.getFlags().contains(EffectElement.EffectElementFlag.ROTATE_TO_MOVEMENT_DIRECTION));
        //
        emitter.setRandomAngle(true);
        emitter.setRotateSpeed(Math.abs(EffectControl.randomSpinRate(effect.getSpriteSpinRateRange())));
        emitter.setGravity(0, element.getMass() * GRAVITY_FACTOR, 0);
        emitter.setLowLife(element.getMinHp() / 20f);
        emitter.setHighLife(element.getMaxHp() / 20f);

        return emitter;
    }

    private void configureParticleEmissionRate(ParticleEmitter emitter, EffectElement element) {
        if (element.getDeathElementId() == element.getEffectElementId() && element.getMaxHp() > 0) {
            // keep the pool topped up forever via
            // jME3's own continuous emission instead of a single burst that
            // fades away for good.
            float avgLifeSeconds = (element.getMinHp() + element.getMaxHp()) / 2f / 20f;
            emitter.setParticlesPerSec(effect.getElementsPerTurn() / avgLifeSeconds);
        } else {
            emitter.setParticlesPerSec(0);
        }
    }

    private void applyParticleColor(ParticleEmitter emitter, EffectElement element) {
        float alpha = 1f;
        if (element.getFlags().contains(EffectElement.EffectElementFlag.FADE)) {
            alpha -= element.getFadePercentage() / 100;
        }

        ColorRGBA rgb = getElementColor(element);
        emitter.setStartColor(new ColorRGBA(rgb.r, rgb.g, rgb.b, 1f));
        emitter.setEndColor(new ColorRGBA(rgb.r, rgb.g, rgb.b, alpha));
    }

    /**
     * An element whose art resource is flagged PLAYER_COLOURED is authored
     * with its own color zeroed out (0,0,0) and takes its actual color from
     * the owning player instead - this is how e.g. the claim/tag burst ends
     * up tinted with the keeper's color
     */
    private ColorRGBA getElementColor(EffectElement element) {
        if (element.getArtResource().getFlags().contains(ArtResource.ArtResourceFlag.PLAYER_COLOURED)) {
            java.awt.Color playerColor = MapThumbnailGenerator.getPlayerColor(ownerId);
            return new ColorRGBA(playerColor.getRed() / 255f, playerColor.getGreen() / 255f,
                    playerColor.getBlue() / 255f, playerColor.getAlpha() / 255f);
        }

        Color color = element.getColor();
        int maxComponent = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        if (maxComponent == 0) {
            return new ColorRGBA(1f, 1f, 1f, 1f);
        }
        return new ColorRGBA(color.getRed() / (float) maxComponent, color.getGreen() / (float) maxComponent,
                color.getBlue() / (float) maxComponent, 1f);
    }

    private void applyParticleScale(ParticleEmitter emitter, EffectElement element) {
        // jME3's particle "size" is a half-extent - ParticleTriMesh builds
        // each quad from position +/- size, i.e. a full width/height of
        // 2*size - so the element's scale (a full-size multiplier, same as
        // the mesh path's Spatial.setLocalScale) has to be halved here or
        // every particle renders twice as big as authored.
        if (element.getFlags().contains(EffectElement.EffectElementFlag.SHRINK)) {
            emitter.setStartSize(element.getMinScale());
            emitter.setEndSize(element.getMaxScale());
        } else {
            emitter.setStartSize(element.getMaxScale() / 1.5f);
            emitter.setEndSize(element.getMinScale() / 1.5f);
        }
    }

    private EffectEmitter createMeshElement(EffectElement element, ArtResource resource) {
        EffectEmitter emitter = newEffectEmitter(element);

        Node model;
        if (resource.getType() == ArtResourceType.PROCEDURAL_MESH) {
            model = (Node) AssetUtils.createProceduralMesh(resource);
        } else {
            model = (Node) AssetUtils.loadModel(assetManager, resource.getName(), resource);
        }

        applyMeshScaleOrAnimation(model, resource);
        emitter.setSpatial(model);
        return emitter;
    }

    private EffectEmitter newEffectEmitter(EffectElement element) {
        return new EffectEmitter(element, effect) {

            @Override
            public void onDeath(Vector3f location) {
                if (element.getDeathElementId() != 0) {
                    VisualEffect.this.addEffectElement(element.getDeathElementId(), location);
                }
            }

            @Override
            public void onHit(Vector3f location) {
                handleElementHit(element, location);
            }
        };
    }

    /**
     * Reads the {@code .kmf} group's part list - name and baked offset per
     * part - from the pre-converted group asset
     * The group's own art is never rendered; only these parts are.
     */
    private List<MeshCollectionPart> loadMeshCollectionParts(ArtResource resource) {
        Spatial group = AssetUtils.loadModel(assetManager, resource.getName(), resource);
        if (!(group instanceof Node groupNode)) {
            return Collections.emptyList();
        }

        List<MeshCollectionPart> parts = new ArrayList<>(groupNode.getQuantity());
        for (Spatial child : groupNode.getChildren()) {
            String partName = child.getUserData(KmfModelLoader.GROUP_PART_NAME);
            if (partName != null) {
                parts.add(new MeshCollectionPart(partName, child.getLocalTranslation()));
            }
        }
        return parts;
    }

    /**
     * Spawns one debris element per part in the group, all sharing the
     * effect's first generation id
     */
    private void generateMeshCollectionParts() {
        List<Integer> generateIds = effect.getGenerateIds();
        if (generateIds.isEmpty()) {
            logger.log(Level.WARNING, "Mesh collection effect {0} has no generation id for its parts", effect.getName());
            return;
        }

        EffectElement effectElement = kwdFile.getEffectElement(generateIds.get(0));
        for (MeshCollectionPart part : meshCollectionParts) {
            EffectEmitter emitter = createMeshCollectionPartElement(effectElement, part);
            emitter.setLocalTranslation(part.offset());
            effectElements.put(emitter, effectElement);
            effectNode.attachChild(emitter);
            emitter.emitOne();
        }
    }

    private EffectEmitter createMeshCollectionPartElement(EffectElement element, MeshCollectionPart part) {
        EffectEmitter emitter = newEffectEmitter(element);

        // The part's mesh replaces whatever art the (typically art-less)
        // debris element would otherwise have, forced to its exported scale
        // rather than the element's own min/max scale roll.
        Spatial model = AssetUtils.loadModel(assetManager, part.name(), null);
        model.setLocalScale(1f);
        emitter.setSpatial(model);
        return emitter;
    }

    private void handleElementHit(EffectElement element, Vector3f location) {
        IMapTileInformation tile = effectManagerState.getPlayerMapViewState().getMapInformation().getMapData().getTile(WorldUtils.vectorToPoint(location));
        if (tile == null) {
            logger.log(Level.WARNING, "Effect hit error");
            return;
        }

        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)
                && element.getHitLavaElementId() != 0) {
            addEffectElement(element.getHitLavaElementId(), location);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)
                && element.getHitWaterElementId() != 0) {
            addEffectElement(element.getHitWaterElementId(), location);
        } else if (element.getHitSolidElementId() != 0) {
            // && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            if (element.getFlags().contains(EffectElement.EffectElementFlag.DIE_WHEN_HIT_SOLID)) {
                if (element.getDeathElementId() != 0) {
                    addEffectElement(element.getDeathElementId(), location);
                }
            } else {
                addEffectElement(element.getHitSolidElementId(), location);
            }
        }
        removeEffect();
    }

    private PointLight getLight(Light effectLight) {
        if (effectLight == null) {
            return null;
        }

        PointLight realLight = new PointLight();
        realLight.setColor(new ColorRGBA(effectLight.getColor().getRed() / 255f,
                effectLight.getColor().getGreen() / 255f,
                effectLight.getColor().getBlue() / 255f,
                effectLight.getColor().getAlpha() / 255f));
        realLight.setRadius(effectLight.getRadius());
        realLight.setPosition(new Vector3f(effectLight.getmKPos().x, effectLight.getmKPos().y, effectLight.getmKPos().z));
        return realLight;
    }

    /**
     * Regular style update, but also signals whether this effect has come to an
     * end
     *
     * @param tpf the update time
     * @return true if the effect is still valid, false if the effect has died
     */
    public boolean update(float tpf) {
        updateCircularPath(tpf);

        // Update the child effects
        effects.removeIf(visualEffect -> !visualEffect.update(tpf));

        // Check the elements
        spawnDeathElements(updateElements());

        // If the whole effect has died, create the death effect
        if (effect.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS) && effectElements.isEmpty()
                && effect.getDeathEffectId() != 0 && !deathEffectSpawned) {
            deathEffectSpawned = true;
            addEffect(effect.getDeathEffectId(), null);
        }

        return handleEffectCompletion();
    }

    private void updateCircularPath(float tpf) {
        if (effect.getCircularPathRate() != 0) {
            // All the generated elements swirl together as one rigid group
            // since none of them carry independent velocity, rotating the
            // whole effect node each tick is equivalent to rotating every
            // element's position vector individually, and far simpler.
            float rate = effect.getCircularPathRate() * FastMath.TWO_PI / 2048f * 20f;
            effectNode.rotate(0, rate * tpf, 0);
        }
    }

    /**
     * Removes depleted effect elements and collects the death elements they
     * should spawn in their place.
     */
    private List<Integer> updateElements() {
        Iterator<Entry<Spatial, EffectElement>> iter = effectElements.entrySet().iterator();
        List<Integer> deathEffectElements = null;
        while (iter.hasNext()) {
            Entry<Spatial, EffectElement> entry = iter.next();
            Spatial value = entry.getKey();
            boolean depleted = (value instanceof ParticleEmitter particleEmitter && particleEmitter.getNumVisibleParticles() == 0)
                    || (value instanceof EffectEmitter effectEmitter && effectEmitter.getQuantity() == 0);
            if (depleted) {

                // Kill
                value.removeFromParent();
                iter.remove();

                // Attach on death element
                if (entry.getValue().getDeathElementId() != 0) {
                    if (deathEffectElements == null) {
                        deathEffectElements = new ArrayList<>();
                    }
                    deathEffectElements.add(entry.getValue().getDeathElementId());
                }
            }
        }
        return deathEffectElements;
    }

    private void spawnDeathElements(List<Integer> deathEffectElements) {
        if (deathEffectElements != null) {
            for (Integer id : deathEffectElements) {
                addEffectElement(id, null);
            }
        }
    }

    /**
     * If no children at all remain, either restarts an infinite effect or
     * detaches it for good.
     *
     * @return true if the effect is still valid, false if it has died
     */
    private boolean handleEffectCompletion() {
        if (effectElements.isEmpty() && effects.isEmpty() && effectNode.getQuantity() == 0) {
            // If infitine, just restart
            if (infinite) {
                if (light != null) {
                    effectNode.removeLight(light);
                }
                load();
            } else {
                effectNode.removeFromParent();
                return false;
            }
        }
        return true;
    }

    public void removeEffect() {
        effects.clear();
        effectElements.clear();
        infinite = false;
    }
}
