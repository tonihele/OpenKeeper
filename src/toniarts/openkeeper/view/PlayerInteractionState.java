/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.font.Rectangle;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import de.lessvoid.nifty.controls.Label;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.CheatState;
import static toniarts.openkeeper.game.state.CheatState.CheatType.MONEY;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.view.selection.SelectionHandler;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.CreatureLoader;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomInstance;

/**
 * State for managing player interactions in the world. Heavily drawn from
 * Philip Willuweit's AgentKeeper code <p.willuweit@gmx.de>.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author ArchDemon
 */
// TODO: States, now only selection
public abstract class PlayerInteractionState extends AbstractPauseAwareState implements RawInputListener {

    public enum InteractionState {

        NONE, ROOM, SELL, SPELL, TRAP, DOOR, STUFF_IN_HAND
    }
    private Main app;
    private GameState gameState;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private final Player player;
    private SelectionHandler handler;
    private boolean startSet = false;
    public Vector2f mousePosition = Vector2f.ZERO;
    private InteractionState interactionState = InteractionState.NONE;
    private int itemId;
    private final Rectangle guiConstraint;
    private boolean isOnGui = false;
    private boolean isTaggable = false;
    private boolean isTagging = false;
    private boolean isOnView = false;
    private boolean isInteractable = false;
    private final Label tooltip;
    private final KeeperHandQueue keeperHand;
    private IInteractiveControl itemInHand;
    private final Node keeperHandNode = new Node("Keeper hand");
    private static final List<String> SLAP_SOUNDS = Arrays.asList(new String[]{"/Global/Slap_1.mp2", "/Global/slap_2.mp2", "/Global/Slap_3.mp2", "/Global/Slap_4.mp2"});
    private static final Logger logger = Logger.getLogger(PlayerInteractionState.class.getName());

    public PlayerInteractionState(Player player, GameState gameState, Rectangle guiConstraint, Label tooltip) {
        this.player = player;
        this.guiConstraint = guiConstraint;
        this.tooltip = tooltip;

        // Init the keeper hand
        keeperHandNode.setLocalScale(500);
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(-FastMath.PI / 4, new Vector3f(-1, 1, 0));
        keeperHandNode.setLocalRotation(rotation);
        keeperHandNode.addLight(new AmbientLight(ColorRGBA.White));
        keeperHand = new KeeperHandQueue((int) gameState.getLevelVariable(Variable.MiscVariable.MiscType.MAX_NUMBER_OF_THINGS_IN_HAND));
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        gameState = this.stateManager.getState(GameState.class);

        this.app.getGuiNode().attachChild(keeperHandNode);

        // Init handler
        handler = new SelectionHandler(this.app, this) {
            @Override
            protected boolean isOnView() {
                return isOnView;
            }

            @Override
            protected boolean isVisible() {

                if (!startSet && !Main.isDebug()) {

                    // Selling is always visible, and the only thing you can do
                    // When building, you can even tag taggables
                    switch (interactionState) {
                        case NONE: {
                            return isTaggable;
                        }
                        case ROOM: {
                            return isOnView;
                        }
                        case SELL: {
                            return isOnView;
                        }
                        case SPELL: {
                            return false;
                        }
                        default:
                            return false;
                    }
                }
                return true;
            }

            @Override
            protected SelectionHandler.SelectionColorIndicator getSelectionColorIndicator() {
                Vector2f pos;
                if (startSet) {
                    pos = handler.getSelectionArea().getActualStartingCoordinates();
                } else {
                    pos = handler.getRoundedMousePos();
                }
                if (interactionState == InteractionState.SELL) {
                    return SelectionColorIndicator.RED;
                } else if (interactionState == InteractionState.ROOM && !isTaggable && !getWorldHandler().isBuildable((int) pos.x, (int) pos.y, player, gameState.getLevelData().getRoomById(itemId))) {
                    return SelectionColorIndicator.RED;
                }
                return SelectionColorIndicator.BLUE;
            }

            @Override
            public void userSubmit(SelectionArea area) {

                if (PlayerInteractionState.this.interactionState == InteractionState.NONE || (PlayerInteractionState.this.interactionState == InteractionState.ROOM && getWorldHandler().isTaggable((int) selectionArea.getActualStartingCoordinates().x, (int) selectionArea.getActualStartingCoordinates().y))) {

                    // Determine if this is a select/deselect by the starting tile's status
                    boolean select = !getWorldHandler().isSelected((int) Math.max(0, selectionArea.getActualStartingCoordinates().x), (int) Math.max(0, selectionArea.getActualStartingCoordinates().y));
                    getWorldHandler().selectTiles(selectionArea, select, player.getPlayerId());
                } else if (PlayerInteractionState.this.interactionState == InteractionState.ROOM && getWorldHandler().isBuildable((int) selectionArea.getActualStartingCoordinates().x, (int) selectionArea.getActualStartingCoordinates().y, player, gameState.getLevelData().getRoomById(itemId))) {
                    getWorldHandler().build(selectionArea, player, gameState.getLevelData().getRoomById(itemId));
                } else if (PlayerInteractionState.this.interactionState == InteractionState.SELL) {
                    getWorldHandler().sell(selectionArea, player);
                }
            }
        };

        CheatState cheatState = new CheatState(app) {
            @Override
            public void onSuccess(CheatState.CheatType cheat) {

                switch (cheat) {
                    case MONEY:
                        getWorldHandler().addGold(player.getPlayerId(), 100000);
                        break;
                    case MANA:
                        gameState.getPlayer(player.getPlayerId()).getManaControl().addMana(100000);
                        break;
                    default:
                        logger.log(Level.WARNING, "Cheat {0} not implemented yet!", cheat.toString());
                }
            }
        };
        this.stateManager.attach(cheatState);

        // Add listener
        if (isEnabled()) {
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            app.getInputManager().addRawInputListener(this);
        } else {
            app.getInputManager().removeRawInputListener(this);
        }
    }

    @Override
    public void cleanup() {
        app.getGuiNode().detachChild(keeperHandNode);
        app.getInputManager().removeRawInputListener(this);
        handler.cleanup();
        CheatState cheatState = this.stateManager.getState(CheatState.class);
        if (cheatState != null) {
            this.stateManager.detach(cheatState);
        }

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        mousePosition.set(evt.getX(), evt.getY());
        Vector2f pos = handler.getRoundedMousePos();
        if (setStateFlags()) {
            setCursor();
        }
        if (startSet) {
            handler.getSelectionArea().setEnd(pos);
            handler.updateSelectionBox();
        } else {
            handler.getSelectionArea().setStart(pos);
            handler.getSelectionArea().setEnd(pos);
            handler.updateSelectionBox();
        }

        // Move the picked up item
        keeperHandNode.setLocalTranslation(evt.getX(), evt.getY(), 0);
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {

            if (interactionState == InteractionState.SPELL && itemId == 2) { // possession
                if (evt.isReleased()) {
                    // TODO make normal selection, not first keeper creature
                    for (Thing t : gameState.getLevelData().getThings()) {
                        if (t instanceof Thing.KeeperCreature) {
                            onPossession((Thing.KeeperCreature) t);
                            break;
                        }
                    }
                    // Reset the state
                    // TODO disable selection box
                    setInteractionState(InteractionState.NONE, 0);
                }
            } else {
                if (evt.isPressed()) {

                    // Creature/object pickup
                    IInteractiveControl interactiveControl = getInteractiveObjectOnCursor();
                    if (interactiveControl != null && interactiveControl.isPickable(player.getPlayerId())) {
                        keeperHand.push(interactiveControl.pickUp(player.getPlayerId()));
                        setCursor();
                    } else {

                        // Selection stuff
                        if (!startSet) {
                            handler.getSelectionArea().setStart(handler.getRoundedMousePos());
                        }
                        startSet = true;

                        // I suppose we are tagging
                        if (isTaggable) {
                            isTagging = true;
                            setCursor();

                            // The tagging sound is positional and played against the cursor change, not the action itself
                            Vector2f pos = handler.getRoundedMousePos();
                            getWorldHandler().playSoundAtTile((int) pos.x, (int) pos.y, "/Global/dk1tag.mp2");
                        }
                    }

                } else if (evt.isReleased()) {
                    startSet = false;
                    handler.userSubmit(null);
                    handler.getSelectionArea().setStart(handler.getRoundedMousePos());
                    handler.updateSelectionBox();
                    handler.setNoSelectedArea();
                    if (isTagging) {
                        isTagging = false;
                        setCursor();
                    }
                }
            }
        } else if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT && evt.isReleased()) {

            Vector2f pos = handler.getRoundedMousePos();
            if (interactionState == InteractionState.NONE) {
                if (Main.isDebug()) {
                    // taggable -> "dig"
                    if (getWorldHandler().isTaggable((int) pos.x, (int) pos.y)) {
                        getWorldHandler().digTile((int) pos.x, (int) pos.y);
                    } // ownable -> "claim"
                    else if (getWorldHandler().isClaimable((int) pos.x, (int) pos.y, player.getPlayerId())) {
                        getWorldHandler().claimTile((int) pos.x, (int) pos.y, player.getPlayerId());
                    }
                } else {
                    IInteractiveControl interactiveControl = getInteractiveObjectOnCursor();
                    if (interactiveControl != null && interactiveControl.isInteractable(player.getPlayerId())) {
                        getWorldHandler().playSoundAtTile((int) pos.x, (int) pos.y, Utils.getRandomItem(SLAP_SOUNDS));
                        interactiveControl.interact(player.getPlayerId());
                    }
                }
            }

            // Reset the state
            setInteractionState(InteractionState.NONE, 0);
            if (setStateFlags()) {
                setCursor();
            }

            startSet = false;
            handler.getSelectionArea().setStart(pos);
            handler.updateSelectionBox();

        } else if (evt.getButtonIndex() == MouseInput.BUTTON_MIDDLE && evt.isReleased()) {
            Vector2f pos = handler.getRoundedMousePos();
            if (Main.isDebug()) {
                getWorldHandler().claimTile((int) pos.x, (int) pos.y, player.getPlayerId());
            }
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        // FIXME use CTRL + ALT + C to activate cheats!
        // TODO Disable in multi player!
        if (evt.isPressed() && evt.getKeyCode() == KeyInput.KEY_F12) {
            CheatState cheat = stateManager.getState(CheatState.class);
            if (!cheat.isEnabled()) {
                cheat.setEnabled(true);
            }
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
    }

    private WorldState getWorldHandler() {
        return stateManager.getState(WorldState.class);
    }

    private PlayerState getPlayerState() {
        return stateManager.getState(PlayerState.class);
    }

    /**
     * Set the interaction state for the keeper
     *
     * @param interactionState state
     * @param id object id, i.e. build state requires the room id
     */
    public void setInteractionState(InteractionState interactionState, int id) {
        this.interactionState = interactionState;
        this.itemId = id;

        // Call the update
        onInteractionStateChange(interactionState, id);
    }

    /**
     * Get the current interaction state
     *
     * @return current interaction
     */
    public InteractionState getInteractionState() {
        return interactionState;
    }

    /**
     * Get the current interaction state item id
     *
     * @return current interaction state item id
     */
    public int getInteractionStateItemId() {
        return itemId;
    }

    private boolean isCursorOnGUI() {
        // if mouse not in rectangle guiConstraint => true
        return mousePosition.x <= guiConstraint.x
                || mousePosition.x >= guiConstraint.x + guiConstraint.width
                || app.getContext().getSettings().getHeight() - mousePosition.y <= guiConstraint.y
                || app.getContext().getSettings().getHeight() - mousePosition.y >= guiConstraint.y + guiConstraint.height;
    }

    /**
     * Set the state flags according to what user can do. Rather clumsy. Fix if
     * you can.
     *
     * @return has the state being changed
     *
     */
    private boolean setStateFlags() {
        boolean changed = false;
        boolean value = isCursorOnGUI();
        if (!changed && isOnGui != value) {
            changed = true;
        }
        isOnGui = value;

        value = isOnView();
        if (!changed && isOnView != value) {
            changed = true;
        }
        isOnView = value;

        value = isTaggable();
        if (!changed && isTaggable != value) {
            changed = true;
        }
        isTaggable = value;

        value = isInteractable();
        if (!changed && isInteractable != value) {
            changed = true;
        }
        isInteractable = value;

        return changed;
    }

    private boolean isInteractable() {

        // TODO: Now just creature control, but all interaction objects
        IInteractiveControl controller = getInteractiveObjectOnCursor();
        Vector2f v = null;
        if (controller != null) {

            // Maybe a kinda hack, but set the tooltip here
            tooltip.setText(controller.getTooltip(player.getPlayerId()));
            controller.onHover();
        } else {

            // Tile tooltip then
            v = handler.getRoundedMousePos();
            TileData tile = getWorldHandler().getMapData().getTile((int) v.x, (int) v.y);
            if (tile != null) {
                if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    RoomInstance roomInstance = getWorldHandler().getMapLoader().getRoomCoordinates().get(new Point((int) v.x, (int) v.y));
                    GenericRoom room = getWorldHandler().getMapLoader().getRoomActuals().get(roomInstance);
                    tooltip.setText(room.getTooltip(player.getPlayerId()));
                } else {
                    tooltip.setText(tile.getTooltip());
                }
            } else {
                tooltip.setText("");
            }
        }

        // If debug, show tile coordinate
        if (Main.isDebug()) {
            StringBuilder sb = new StringBuilder();
            Point p;
            if (controller != null) {
                p = getWorldHandler().getTileCoordinates(((AbstractControl) controller).getSpatial().getWorldTranslation());
            } else {
                p = new Point((int) v.x, (int) v.y);
            }
            sb.append("(");
            sb.append(p.x);
            sb.append(", ");
            sb.append(p.y);
            sb.append("): ");
            sb.append(tooltip.getText());
            tooltip.setText(sb.toString());
        }

        return (controller != null);
    }

    private IInteractiveControl getInteractiveObjectOnCursor() {

        // See if we hit a creature/object
        CollisionResults results = new CollisionResults();

        // Convert screen click to 3D position
        Vector3f click3d = app.getCamera().getWorldCoordinates(
                new Vector2f(mousePosition.x, mousePosition.y), 0f);
        Vector3f dir = app.getCamera().getWorldCoordinates(
                new Vector2f(mousePosition.x, mousePosition.y), 1f).subtractLocal(click3d);

        // Aim the ray from the mouse spot forwards
        Ray ray = new Ray(click3d, dir);

        // Collect intersections between ray and all nodes in results list
        getWorldHandler().getThingsNode().collideWith(ray, results);

        // See the results so we see what is going on
        for (int i = 0; i < results.size(); i++) {

            // TODO: Now just creature control, but all interaction objects
            IInteractiveControl controller = results.getCollision(i).getGeometry().getParent().getParent().getControl(IInteractiveControl.class);
            if (controller != null) {
                return controller;
            }
        }
        return null;
    }

    private boolean isTaggable() {
        Vector2f pos = handler.getRoundedMousePos();
        return (interactionState == InteractionState.ROOM || interactionState == InteractionState.NONE) && isOnView && getWorldHandler().isTaggable((int) pos.x, (int) pos.y);
    }

    private boolean isOnView() {
        if (isOnGui) {
            return false;
        }
        Vector2f pos = handler.getRoundedMousePos();
        return (pos.x >= 0 && pos.x < gameState.getLevelData().getMap().getWidth() && pos.y >= 0 && pos.y < gameState.getLevelData().getMap().getHeight());
    }

    protected void setCursor() {
        removeItemInHand();
        if (Main.getUserSettings().getSettingBoolean(Settings.Setting.USE_CURSORS)) {
            if (isOnGui || isInteractable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));
            } else if (isTagging) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE_TAGGING, assetManager));
            } else if (isTaggable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE, assetManager));
            } else if (!keeperHand.isEmpty()) {

                // Keeper hand item
                itemInHand = keeperHand.peek();
                inputManager.setMouseCursor(CursorFactory.getCursor(itemInHand.getInHandCursor(), assetManager));
                setupItemInHand();
            } else {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.IDLE, assetManager));
            }
        } else if (!keeperHand.isEmpty()) {
            itemInHand = keeperHand.peek();
            setupItemInHand();
        }
    }

    private void setupItemInHand() {
        if (itemInHand != null && itemInHand.getInHandMesh() != null) {

            // Attach to GUI node and play the animation
            itemInHand.getSpatial().setLocalTranslation(0, 0, 0);
            itemInHand.getSpatial().setLocalRotation(Matrix3f.ZERO);
            keeperHandNode.attachChild(itemInHand.getSpatial());
            CreatureLoader.playAnimation(itemInHand.getSpatial(), itemInHand.getInHandMesh(), assetManager);
        }
    }

    private void removeItemInHand() {
        if (itemInHand != null && itemInHand.getInHandMesh() != null) {

            // Remove from GUI node
            itemInHand.getSpatial().removeFromParent();
            itemInHand = null;
        }
    }

    /**
     * A callback for changing the interaction state
     *
     * @param interactionState new state
     * @param id new id
     */
    protected abstract void onInteractionStateChange(InteractionState interactionState, int id);

    protected abstract void onPossession(Thing.KeeperCreature creature);
}
