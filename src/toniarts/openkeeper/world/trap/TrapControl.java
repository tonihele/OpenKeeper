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
package toniarts.openkeeper.world.trap;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.File;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.animation.AnimationControl;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.control.IUnitFlowerControl;
import toniarts.openkeeper.world.control.UnitFlowerControl;
import toniarts.openkeeper.world.object.HighlightControl;

/**
 * The trap control, handles trap related functions...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TrapControl extends HighlightControl implements IInteractiveControl, AnimationControl, IUnitFlowerControl {

    public enum TrapState {

        NORMAL, BLUEPRINT, DESTROYED;
    }

    private final WorldState worldState;
    private final Trap trap;
    private final String name;
    private final AssetManager assetManager;
    private final TileData tile;
    private TrapState state = TrapState.NORMAL;
    private int health;
    private final static ResourceBundle BUNDLE = Main.getResourceBundle("Interface/Texts/Text");

    public TrapControl(TileData tile, Trap trap, WorldState worldState, AssetManager assetManager) {
        this(tile, trap, worldState, assetManager, false);
    }

    public TrapControl(TileData tile, Trap trap, WorldState worldState, AssetManager assetManager, boolean blueprint) {
        super();

        this.worldState = worldState;
        this.trap = trap;
        this.tile = tile;
        this.assetManager = assetManager;
        this.health = trap.getHealth();
        if (blueprint) {
            state = TrapState.BLUEPRINT;
        }
        name = BUNDLE.getString(Integer.toString(trap.getNameStringId()));
    }

    @Override
    protected void controlUpdate(float tpf) {

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public void initState() {

        // TODO: We should do this initially, no point of having to do extra work perhaps
        if (state == TrapState.BLUEPRINT) {
            AssetUtils.setBlueprint(assetManager, spatial);
        }
    }

    @Override
    public String getTooltip(short playerId) {
        if (trap.getFlags().contains(Trap.TrapFlag.GUARD_POST)) {
            if (playerId == getOwnerId()) {
                return BUNDLE.getString("2538");
            } else {
                return name;
            }
        }

        // Regular traps
        String tooltip;
        if (playerId == getOwnerId()) {
            tooltip = BUNDLE.getString("2534").replaceFirst("%68", name)
                    .replaceFirst("%25", Integer.toString(trap.getManaUsage()))
                    .replaceFirst("%26", Integer.toString(trap.getManaCostToFire()))
                    .replaceFirst("%72", trap.getFlags().contains(Trap.TrapFlag.REVEAL_WHEN_FIRED) ? BUNDLE.getString("2514") : BUNDLE.getString("2513")); // This is not entirely true if you compare to original, see Fear trap
        } else {
            tooltip = BUNDLE.getString("2542");
        }

        return tooltip.replaceFirst("%37%", Integer.toString(getHealthPercentage()));
    }

    @Override
    public boolean isPickable(short playerId) {
        return false;
    }

    @Override
    public boolean isInteractable(short playerId) {
        return false;
    }

    @Override
    public IInteractiveControl pickUp(short playerId) {
        throw new UnsupportedOperationException("You can't pick up a trap!");
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return null;
    }

    @Override
    public ArtResource getInHandMesh() {
        return null;
    }

    @Override
    public ArtResource getInHandIcon() {
        return null;
    }

    @Override
    public DroppableStatus getDroppableStatus(TileData tile, short playerId) {
        return null;
    }

    @Override
    public void drop(TileData tile, Vector2f coordinates, IInteractiveControl control) {
        throw new UnsupportedOperationException("You can't drop a trap!");
    }

    @Override
    public boolean interact(short playerId) {
        return false; // Some are interactable but I don't know how to determine it, like boulder & trigger
    }

    @Override
    public void onHover() {
        UnitFlowerControl.showUnitFlower(this, null);
    }

    @Override
    public short getOwnerId() {
        return tile.getPlayerId();
    }

    @Override
    public void onHoverEnd() {
        super.onHoverEnd();

        // Restore blueprint state
        if (state == TrapState.BLUEPRINT) {
            AssetUtils.setBlueprint(assetManager, spatial);
        }
    }

    public TrapState getState() {
        return state;
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public void onAnimationStop() {

    }

    @Override
    public void onAnimationCycleDone() {
        //
    }

    @Override
    public boolean isStopAnimation() {
        return true; // We stop it always
    }

    @Override
    public int getMaxHealth() {
        return trap.getHealth();
    }

    @Override
    public float getHeight() {
        return trap.getHeight();
    }

    @Override
    public String getCenterIcon() {
        return ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                + File.separator + trap.getFlowerIcon().getName() + ".png");
    }

}
