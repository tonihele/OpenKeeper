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
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.WatchedEntity;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.MapThumbnailGenerator;

/**
 * A base class for showing unit (creature, object...) flower. TODO: Maybe
 * listen for changes in health to automatically show the icon
 *
 * @param <T> The type of the entity, the data record
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class UnitFlowerControl<T> extends BillboardControl implements IUnitFlowerControl<T> {
    
    private static final Logger LOGGER = Logger.getLogger(UnitFlowerControl.class.getName());

    private static final float DISPLAY_SECONDS = 2.5f;
    private static final Collection<Class<? extends EntityComponent>> WATCHED_COMPONENTS = Arrays.asList(Health.class, Owner.class);

    private float targetTimeVisible = DISPLAY_SECONDS;
    private float timeVisible = 0;
    private int currentHealthIndex = 0;
    private Node unitSpatial;
    private boolean updateRequired = false;
    private Material material;
    private final EntityId entityId;
    private final WatchedEntity entity;
    private final AssetManager assetManager;
    private final T data;

    protected UnitFlowerControl(EntityId entityId, EntityData entityData, T data, AssetManager assetManager) {
        super();
        this.entityId = entityId;
        this.data = data;
        this.assetManager = assetManager;

        // Subscribe to the entity changes
        entity = entityData.watchEntity(entityId, compileWatchedComponents());

        enabled = false;
        setAlignment(Alignment.Screen);
    }

    /**
     * Get the entity for this control. It has all the up to date components
     * attached. To control what components are included, add the wanted
     * components with the {@link #getWatchedComponents() } method.
     *
     * @return
     */
    protected final Entity getEntity() {
        return entity;
    }

    private Class<EntityComponent>[] compileWatchedComponents() {
        Set<Class<? extends EntityComponent>> components = new HashSet<>();
        components.addAll(WATCHED_COMPONENTS);
        components.addAll(getWatchedComponents());

        return components.toArray(new Class[0]);
    }

    /**
     * Override this to give out the components needed to watch for. By default
     * a mutable list is returned so you can just ourright add your stuff here.
     * No need to worry about duplicates.
     *
     * @return list of components needed from the entity
     */
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        return new ArrayList<>();
    }

    /**
     * Get the unit owner id, for the color
     *
     * @return the unit player id
     */
    protected short getOwnerId() {
        Owner owner = entity.get(Owner.class);
        if (owner == null) {
            return 0;
        }
        return owner.ownerId;
    }

    /**
     * Get unit max health
     *
     * @return max health
     */
    protected int getHealthMax() {
        Health health = entity.get(Health.class);
        if (health == null) {
            return 100;
        }
        return health.maxHealth;
    }

    /**
     * Get current unit health
     *
     * @return unit current health
     */
    protected int getHealthCurrent() {
        Health health = entity.get(Health.class);
        if (health == null) {
            return 0;
        }
        return health.health;
    }

    /**
     * Get the center icon resource as a string resource path
     *
     * @return the center icon
     */
    abstract protected String getCenterIcon();

    /**
     * Get unit height, to correctly position the flower
     *
     * @return the unit height
     */
    abstract protected float getHeight();

    /**
     * Get the objective icon as a string resource path, the flower around the
     * icon
     *
     * @return objective icon
     */
    protected String getObjectiveIcon() {
        return null;
    }

    /**
     * Prior to show the flower (note that the flower might already be showing)
     */
    protected void onShow() {

    }

    /**
     * The flower is hidden
     */
    protected void onHide() {

    }

    /**
     * When the material is created for the first time
     *
     * @param material the material
     */
    protected void onMaterialCreated(Material material) {

    }

    /**
     * The flower texture is being refreshed
     *
     * @param material the material for modifying
     */
    protected void onMaterialGenerated(Material material) {

    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public final T getDataObject() {
        return data;
    }

    /**
     * On update loop, only when we are showing
     *
     * @param tpf the time since last update
     * @return true if a redraw of the icon is needed
     */
    protected boolean onUpdate(float tpf) {
        return false;
    }

    /**
     * Show the flower for a brief time
     */
    @Override
    public final void show() {
        show(DISPLAY_SECONDS);
    }

    /**
     * Show the flower for a period of time
     *
     * @param period the time period the flower should be visible
     */
    @Override
    public final void show(float period) {

        if (isEnabled() && targetTimeVisible - timeVisible < period) {

            // If already showing, just extend the time
            targetTimeVisible = timeVisible + period;
        } else if (!isEnabled()) {

            // If no health, assume that we should not be visible, ever
            Health health = entity.get(Health.class);
            if (health == null) {
                return;
            }

            // Reset counter and create the graphics
            timeVisible = 0;
            targetTimeVisible = period;
            updateRequired = true;
            unitSpatial.attachChild(getFlower());

            // Enable
            setEnabled(true);

            onShow();
        }
    }

    /**
     * Hide the flower
     */
    @Override
    public final void hide() {
        setEnabled(false);
        unitSpatial.detachChild(getFlower());

        onHide();
    }

    @Override
    public void update(float tpf) {
        entity.applyChanges();

        super.update(tpf);
    }

    @Override
    protected final void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

        timeVisible += tpf;
        if (timeVisible >= targetTimeVisible) {

            // Remove us
            hide();

            return;
        }

        // Update health ring
        updateHealth();

        // See if we need to update
        if (onUpdate(tpf) || updateRequired) {
            generateTexture();
        }
    }

    protected Spatial getFlower() {
        if (spatial == null) {
            updateRequired = false;

            createFlower();

            generateTexture();
        }
        return spatial;
    }

    /**
     * Called once, when the flower is created
     */
    private void createFlower() {
        Mesh mesh = createMesh(0.5f, 0.5f);
        spatial = new Geometry("Health indicator", mesh);
        material = new Material(assetManager, "MatDefs/UnitFlower.j3md");
        setFlowerColor(getPlayerColor());
        spatial.setMaterial(material);
        material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        material.getAdditionalRenderState().setDepthTest(false);
        spatial.setQueueBucket(Bucket.Translucent);
        spatial.setUserData(AssetUtils.USER_DATA_KEY_REMOVABLE, false);
        spatial.setShadowMode(RenderQueue.ShadowMode.Off);

        onMaterialCreated(material);
    }

    protected final ColorRGBA getPlayerColor() {
        return getPlayerColor(getOwnerId());
    }

    protected static ColorRGBA getPlayerColor(short ownerId) {
        return getColor(MapThumbnailGenerator.getPlayerColor(ownerId));
    }

    protected final void setFlowerColor(ColorRGBA c) {
        if (material != null) {
            material.setColor("Color", c);
        }
    }

    private static ColorRGBA getColor(Color c) {
        return new ColorRGBA(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
    }

    @Override
    public void setSpatial(Spatial spatial) {

        // Create the spatial
        unitSpatial = (Node) spatial;
        if (unitSpatial != null) {
            show();
        }
    }

    private void updateHealth() {
        int healthIndex = 5 - (int) Math.ceil((float) Math.max(getHealthCurrent(), 0) / getHealthMax() / (1.0 / 5));
        if (healthIndex != currentHealthIndex) {

            // Reload health ring
            currentHealthIndex = healthIndex;
            updateRequired = true;
        }
    }

    private void generateTexture() {
        if (material != null) {

            // Health ring
            material.setTexture("HealthTexture", assetManager.loadTexture("Textures/GUI/moods/H-0" + currentHealthIndex + ".png"));

            // The rest
            String centerIcon = getCenterIcon();
            if (centerIcon != null) {
                material.setTexture("CenterTexture", assetManager.loadTexture(getCenterIcon()));
            } else {
                material.setTexture("CenterTexture", null);
            }
            String objectiveIcon = getObjectiveIcon();
            if (objectiveIcon != null) {
                material.setTexture("ObjectiveTexture", assetManager.loadTexture(objectiveIcon));
            } else {
                material.setTexture("ObjectiveTexture", null);
            }

            onMaterialGenerated(material);
        }
    }

    /**
     * Creates a quad, just that this one is centered on x-axis and on y-axis
     * lifted up by the unit height
     *
     * @param width width
     * @param height height
     * @return the mesh
     */
    private Mesh createMesh(float width, float height) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{-width / 2f, -height / 2f + getHeight(), 0,
            width / 2f, -height / 2f + getHeight(), 0,
            width / 2f, height / 2f + getHeight(), 0,
            -width / 2f, height / 2f + getHeight(), 0
        });

        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 1,
            1, 1,
            1, 0,
            0, 0});
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, new float[]{0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1});
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2,
            0, 2, 3});

        mesh.updateBound();
        mesh.setStatic();
        return mesh;
    }

    /**
     * Show the unit flower
     *
     * @param control the control to own the flower control
     * @param seconds how many seconds to show, can be {@code null}
     */
    public static void showUnitFlower(AbstractControl control, Integer seconds) {
        IUnitFlowerControl aufc = control.getSpatial().getControl(IUnitFlowerControl.class);
        if (aufc != null) {
            if (seconds != null) {
                aufc.show(seconds);
            } else {
                aufc.show();
            }
        }
    }

    /**
     * Hide the unit flower
     *
     * @param control the control to own the flower control
     */
    public static void hideUnitFlower(AbstractControl control) {
        IUnitFlowerControl aufc = control.getSpatial().getControl(IUnitFlowerControl.class);
        if (aufc != null) {
            aufc.hide();
        }
    }

    @Override
    public void cleanup() {
        entity.release();
    }

}
