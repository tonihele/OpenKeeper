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
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.elements.Element;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.console.ConsoleState;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.CheatState;
import static toniarts.openkeeper.game.state.CheatState.CheatType.MONEY;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.PlayerScreenController;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState.Type;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.view.selection.SelectionHandler;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.HighlightControl;
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

    private static final int SPELL_POSSESSION_ID = 2;
    private static final float CURSOR_UPDATE_INTERVAL = 0.25f;

    private Main app;
    private GameState gameState;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private final Player player;
    private SelectionHandler handler;
    private boolean startSet = false;
    public Vector2f mousePosition = Vector2f.ZERO;
    private InteractionState interactionState = new InteractionState();
    private float timeFromLastUpdate = CURSOR_UPDATE_INTERVAL;

    private Element view;
    private boolean isOnGui = false;
    private boolean isTaggable = false;
    private boolean isTagging = false;
    private boolean isOnView = false;
    private boolean isInteractable = false;
    private IInteractiveControl interactiveControl;
    private Label tooltip;
    private KeeperHand keeperHand;
    private static final Logger logger = Logger.getLogger(PlayerInteractionState.class.getName());

    public PlayerInteractionState(Player player) {
        this.player = player;
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        gameState = this.stateManager.getState(GameState.class);

        PlayerScreenController psc = this.stateManager.getState(PlayerState.class).getScreen();
        this.view = psc.getGuiConstraint();
        this.tooltip = psc.getTooltip();
        // Init the keeper hand
        keeperHand = new KeeperHand(assetManager, (int) gameState.getLevelVariable(MiscType.MAX_NUMBER_OF_THINGS_IN_HAND));
        this.app.getGuiNode().attachChild(keeperHand.getNode());

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
                    switch (interactionState.getType()) {
                        case NONE: {
                            return (isTaggable || keeperHand.getItem() != null);
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
                if (interactionState.getType() == Type.NONE && keeperHand.getItem() != null) {
                    TileData tile = getWorldHandler().getMapData().getTile((int) pos.x, (int) pos.y);
                    IInteractiveControl.DroppableStatus status = keeperHand.peek().getDroppableStatus(tile);
                    return (status != IInteractiveControl.DroppableStatus.NOT_DROPPABLE ? SelectionColorIndicator.BLUE : SelectionColorIndicator.RED);
                }
                if (interactionState.getType() == Type.SELL) {
                    return SelectionColorIndicator.RED;
                } else if (interactionState.getType() == Type.ROOM && !isTaggable && !getWorldHandler().isBuildable((int) pos.x, (int) pos.y, player, gameState.getLevelData().getRoomById(interactionState.getItemId()))) {
                    return SelectionColorIndicator.RED;
                }
                return SelectionColorIndicator.BLUE;
            }

            @Override
            public void userSubmit(SelectionArea area) {

                if (interactionState.getType() == Type.NONE
                        || (interactionState.getType() == Type.ROOM
                        && getWorldHandler().isTaggable((int) selectionArea.getActualStartingCoordinates().x, (int) selectionArea.getActualStartingCoordinates().y))) {

                    // Determine if this is a select/deselect by the starting tile's status
                    boolean select = !getWorldHandler().isSelected((int) Math.max(0, selectionArea.getActualStartingCoordinates().x), (int) Math.max(0, selectionArea.getActualStartingCoordinates().y));
                    getWorldHandler().selectTiles(selectionArea, select, player.getPlayerId());
                } else if (interactionState.getType() == Type.ROOM && getWorldHandler().isBuildable((int) selectionArea.getActualStartingCoordinates().x,
                        (int) selectionArea.getActualStartingCoordinates().y, player, gameState.getLevelData().getRoomById(interactionState.getItemId()))) {
                    getWorldHandler().build(selectionArea, player, gameState.getLevelData().getRoomById(interactionState.getItemId()));
                } else if (interactionState.getType() == Type.SELL) {
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
        app.getGuiNode().detachChild(keeperHand.getNode());
        app.getInputManager().removeRawInputListener(this);
        handler.cleanup();
        CheatState cheatState = this.stateManager.getState(CheatState.class);
        if (cheatState != null) {
            this.stateManager.detach(cheatState);
        }

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        timeFromLastUpdate += tpf;

        // Update the cursor, the camera might have moved, a creature might have slipped by us... etc.
        if (timeFromLastUpdate > CURSOR_UPDATE_INTERVAL) {
            handleMouseMoved();
        }
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
        handleMouseMoved();
    }

    private void handleMouseMoved() {
        timeFromLastUpdate = 0;
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
        keeperHand.setPosition(mousePosition.x, mousePosition.y);
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        timeFromLastUpdate = 0;
        if (isOnGui) {
            if (setStateFlags()) {
                setCursor();
            }
            return;
        }

        if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {

            if (interactionState.getType() == Type.SPELL && interactionState.getItemId() == 2) { // possession
                if (evt.isReleased()) {
                    // TODO make normal selection, not first keeper creature
                    if (interactiveControl != null && interactionState.getItemId() == SPELL_POSSESSION_ID
                            && interactiveControl.isPickable(player.getPlayerId())) {
                        CreatureControl cc = interactiveControl.getSpatial().getControl(CreatureControl.class);
                        if (cc != null) {
                            onPossession(cc);
                            // Reset the state
                            // TODO disable selection box
                            setInteractionState(Type.NONE, 0);
                        }
                    }
                }
            } else if (evt.isPressed()) {

                // Creature/object pickup
                interactiveControl = getInteractiveObjectOnCursor();
                if (interactiveControl != null && !keeperHand.isFull() && interactiveControl.isPickable(player.getPlayerId())) {
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
        } else if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT && evt.isReleased()) {

            Vector2f pos = handler.getRoundedMousePos();
            if (interactionState.getType() == Type.NONE) {

                // Drop
                if (keeperHand.getItem() != null) {
                    TileData tile = getWorldHandler().getMapData().getTile((int) pos.x, (int) pos.y);
                    IInteractiveControl.DroppableStatus status = keeperHand.peek().getDroppableStatus(tile);
                    if (status != IInteractiveControl.DroppableStatus.NOT_DROPPABLE) {

                        // Drop & update cursor
                        keeperHand.pop().drop(tile);
                        setCursor();
                    }
                } else if (Main.isDebug()) {
                    // taggable -> "dig"
                    if (getWorldHandler().isTaggable((int) pos.x, (int) pos.y)) {
                        getWorldHandler().digTile((int) pos.x, (int) pos.y);
                    } // ownable -> "claim"
                    else if (getWorldHandler().isClaimable((int) pos.x, (int) pos.y, player.getPlayerId())) {
                        getWorldHandler().claimTile((int) pos.x, (int) pos.y, player.getPlayerId());
                    }
                } else {
                    interactiveControl = getInteractiveObjectOnCursor();
                    if (interactiveControl != null && interactiveControl.isInteractable(player.getPlayerId())) {
                        getWorldHandler().playSoundAtTile((int) pos.x, (int) pos.y, KeeperHand.getSlapSound());
                        interactiveControl.interact(player.getPlayerId());
                    }
                }
            }

            // Reset the state
            setInteractionState(Type.NONE, 0);
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
        if (evt.isPressed()) {
            if (evt.getKeyCode() == KeyInput.KEY_F12) {
                // FIXME use CTRL + ALT + C to activate cheats!
                // TODO Disable in multi player!
                CheatState cheat = stateManager.getState(CheatState.class);
                if (!cheat.isEnabled()) {
                    cheat.setEnabled(true);
                }
            } else if (evt.getKeyCode() == ConsoleState.KEY && Main.isDebug()) {
                stateManager.getState(ConsoleState.class).setEnabled(true);
            } else if (evt.getKeyCode() == (Integer) Settings.Setting.TOGGLE_PLAYER_INFORMATION.getDefaultValue()) {
                Element stats = view.findElementById("statistics");
                if (stats != null) {
                    if (stats.isVisible()) {
                        stats.hide();
                    } else {
                        stats.show();
                    }
                }
            }
        }

    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
    }

    private WorldState getWorldHandler() {
        return stateManager.getState(WorldState.class);
    }

    /**
     * Set the interaction state for the keeper
     *
     * @param type interaction type
     * @param id object id, i.e. build state requires the room id
     */
    public void setInteractionState(Type type, int id) {
        interactionState.setState(type, id);

        // Call the update
        onInteractionStateChange(interactionState);
    }

    /**
     * Get the current interaction state
     *
     * @return current interaction
     */
    public InteractionState getInteractionState() {
        return interactionState;
    }

    private boolean isCursorOnGUI() {
        int height = app.getContext().getSettings().getHeight();

        if (view.isVisible() && view.isMouseInsideElement((int) mousePosition.x, height - (int) mousePosition.y)) {
            for (Element e : view.getChildren()) {
                if (e.isVisible() && e.isMouseInsideElement((int) mousePosition.x, height - (int) mousePosition.y)) {
                    return true;
                }
            }
            return false;
        }

        return true;
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
        if (isOnGui) {
            return false;
        }

        interactiveControl = getInteractiveObjectOnCursor();
        Vector2f v = null;
        if (interactiveControl != null) {

            // Maybe a kinda hack, but set the tooltip here
            tooltip.setText(interactiveControl.getTooltip(player.getPlayerId()));
            interactiveControl.onHover();
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
            if (interactiveControl != null) {
                p = getWorldHandler().getTileCoordinates(((AbstractControl) interactiveControl).getSpatial().getWorldTranslation());
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

        return (interactiveControl != null);
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
        Node object;
        for (int i = 0; i < results.size(); i++) {

            // TODO: Now just creature control, but all interaction objects
            object = results.getCollision(i).getGeometry().getParent().getParent();
            interactiveControl = object.getControl(IInteractiveControl.class);
            if (interactiveControl != null) {
                HighlightControl hc = object.getControl(HighlightControl.class);
                if (hc != null) {
                    hc.activate();
                } else {
                    object.addControl(new HighlightControl());
                }
                return interactiveControl;
            }
        }
        return null;
    }

    private boolean isTaggable() {
        Vector2f pos = handler.getRoundedMousePos();
        return (interactionState.getType() == Type.ROOM
                || interactionState.getType() == Type.NONE)
                && isOnView && getWorldHandler().isTaggable((int) pos.x, (int) pos.y);
    }

    private boolean isOnView() {
        if (isOnGui) {
            return false;
        }
        Vector2f pos = handler.getRoundedMousePos();
        return (pos.x >= 0 && pos.x < gameState.getLevelData().getMap().getWidth() && pos.y >= 0 && pos.y < gameState.getLevelData().getMap().getHeight());
    }

    protected void setCursor() {
        keeperHand.setVisible(false);
        if (Main.getUserSettings().getSettingBoolean(Settings.Setting.USE_CURSORS)) {
            if (isOnGui || isInteractable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));
            } else if (isTagging) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE_TAGGING, assetManager));
            } else if (isTaggable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE, assetManager));
            } else if (keeperHand.getItem() != null) {

                // Keeper hand item
                inputManager.setMouseCursor(CursorFactory.getCursor(keeperHand.getItem().getInHandCursor(), assetManager));
                keeperHand.setVisible(true);
            } else {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.IDLE, assetManager));
            }
        } else if (keeperHand.getItem() != null) {
            keeperHand.setVisible(true);
        }
    }

    /**
     * A callback for changing the interaction state
     *
     * @param interactionState new state
     */
    protected abstract void onInteractionStateChange(InteractionState interactionState);

    protected abstract void onPossession(CreatureControl creature);

    public static class InteractionState {

        public enum Type {

            NONE, ROOM, SELL, SPELL, TRAP, DOOR, STUFF_IN_HAND
        }

        private int itemId = 0;
        private Type type = Type.NONE;

        public InteractionState() {
            itemId = 0;
            type = Type.NONE;
        }

        public InteractionState(Type type, int itemId) {
            this.type = type;
            this.itemId = itemId;
        }

        public InteractionState(Type type) {
            this(type, 0);
        }

        protected void setState(Type type, int itemId) {
            this.type = type;
            this.itemId = itemId;

            //PlayerInteractionState.this.onInteractionStateChange(this);
        }

        protected void setState(Type type) {
            setState(type, 0);
        }

        public Type getType() {
            return type;
        }

        public int getItemId() {
            return itemId;
        }
    }
}
