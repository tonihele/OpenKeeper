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
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector2f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.view.selection.SelectionHandler;
import toniarts.openkeeper.world.WorldHandler;

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

        NONE, BUILD, SELL, CAST
    }
    private Main app;
    private final GameState gameState;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private final Player player;
    private SelectionHandler handler;
    private boolean actionIsPressed = false;
    private boolean cancelIsPressed = false;
    private boolean startSet = false;
    public Vector2f mousePosition = Vector2f.ZERO;
    private InteractionState interactionState = InteractionState.NONE;
    private int itemId;
    private final int guiConstraintTop;
    private final int guiConstraintBottom;
    private boolean isOnGui = false;
    private boolean isTaggable = false;
    private boolean isTagging = false;
    private boolean isOnView = false;
    private static final Logger logger = Logger.getLogger(PlayerInteractionState.class.getName());

    public PlayerInteractionState(Player player, GameState gameState, final int guiConstraintTop, final int guiConstraintBottom) {
        this.player = player;
        this.gameState = gameState;
        this.guiConstraintTop = guiConstraintTop;
        this.guiConstraintBottom = guiConstraintBottom;
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();

        // Init handler
        handler = new SelectionHandler(this.app, this) {
            @Override
            protected boolean isOnView() {
                return isOnView;
            }

            @Override
            protected boolean isVisible() {

                if (!startSet && !PlayerInteractionState.this.app.isDebug()) {

                    // Selling is always visible, and the only thing you can do
                    // When building, you can even tag taggables
                    Vector2f pos = handler.getRoundedMousePos();
                    switch (interactionState) {
                        case NONE: {
                            return isTaggable;
                        }
                        case BUILD: {
                            return isOnView;
                        }
                        case SELL: {
                            return isOnView;
                        }
                        case CAST: {
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
                Vector2f pos = handler.getRoundedMousePos();
                if (interactionState == InteractionState.SELL) {
                    return SelectionColorIndicator.RED;
                } else if (interactionState == InteractionState.BUILD && !isTaggable && !getWorldHandler().isBuildable((int) pos.x, (int) pos.y, player, gameState.getLevelData().getRoomById(itemId))) {
                    return SelectionColorIndicator.RED;
                }
                return SelectionColorIndicator.BLUE;
            }

            @Override
            public void userSubmit(SelectionArea area) {

                if (PlayerInteractionState.this.interactionState == InteractionState.NONE || (PlayerInteractionState.this.interactionState == InteractionState.BUILD && getWorldHandler().isTaggable((int) selectionArea.getActualStartingCoordinates().x, (int) selectionArea.getActualStartingCoordinates().y))) {

                    // Determine if this is a select/deselect by the starting tile's status
                    boolean select = !getWorldHandler().isSelected((int) Math.max(0, selectionArea.getActualStartingCoordinates().x), (int) Math.max(0, selectionArea.getActualStartingCoordinates().y));
                    getWorldHandler().selectTiles(selectionArea, select);
                } else if (PlayerInteractionState.this.interactionState == InteractionState.BUILD && getWorldHandler().isBuildable((int) selectionArea.getActualStartingCoordinates().x, (int) selectionArea.getActualStartingCoordinates().y, player, gameState.getLevelData().getRoomById(itemId))) {
                    getWorldHandler().build(selectionArea, player, gameState.getLevelData().getRoomById(itemId));

                    // Reset state after successful build
                    setInteractionState(InteractionState.NONE, 0);
                } else if (PlayerInteractionState.this.interactionState == InteractionState.SELL && getWorldHandler().isSellable((int) selectionArea.getActualStartingCoordinates().x, (int) selectionArea.getActualStartingCoordinates().y, player)) {
                    getWorldHandler().sell(selectionArea, player);
                }
            }
        };

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
        app.getInputManager().removeRawInputListener(this);

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
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {

            if (evt.isPressed()) {
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
            if (interactionState == InteractionState.NONE && app.isDebug()) {

                // Debug
                // taggable -> "dig"
                if (getWorldHandler().isTaggable((int) pos.x, (int) pos.y)) {
                    getWorldHandler().digTile((int) pos.x, (int) pos.y);
                } // ownable -> "claim"
                else if (getWorldHandler().isClaimable((int) pos.x, (int) pos.y, player)) {
                    getWorldHandler().claimTile((int) pos.x, (int) pos.y, player);
                }
                //
            }

            // Reset the state
            setInteractionState(InteractionState.NONE, 0);
            if (setStateFlags()) {
                setCursor();
            }

            startSet = false;
            handler.getSelectionArea().setStart(pos);
            handler.updateSelectionBox();
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
    }

    public WorldHandler getWorldHandler() {
        return gameState.getWorldHandler();
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

        return changed;
    }

    private boolean isCursorOnGUI() {
        return app.getContext().getSettings().getHeight() - mousePosition.y <= guiConstraintTop || app.getContext().getSettings().getHeight() - mousePosition.y >= guiConstraintBottom;
    }

    private boolean isTaggable() {
        Vector2f pos = handler.getRoundedMousePos();
        return (interactionState == InteractionState.BUILD || interactionState == InteractionState.NONE) && isOnView && getWorldHandler().isTaggable((int) pos.x, (int) pos.y);
    }

    private boolean isOnView() {
        if (isOnGui) {
            return false;
        }
        Vector2f pos = handler.getRoundedMousePos();
        return (pos.x >= 0 && pos.x < gameState.getLevelData().getWidth() && pos.y >= 0 && pos.y < gameState.getLevelData().getHeight());
    }

    protected void setCursor() {
        if (app.getUserSettings().getSettingBoolean(Settings.Setting.USE_CURSORS)) {
            if (isOnGui) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));
            } else if (isTagging) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE_TAGGING, assetManager));
            } else if (isTaggable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE, assetManager));
            } else {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.IDLE, assetManager));
            }
        }
    }

    /**
     * A callback for changing the interaction state
     *
     * @param interactionState new state
     * @param id new id
     */
    protected abstract void onInteractionStateChange(InteractionState interactionState, int id);
}
