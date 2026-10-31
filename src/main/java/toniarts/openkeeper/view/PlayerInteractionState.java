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
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.elements.Element;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.console.ConsoleState;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.controller.KeeperSpellCastValidator;
import toniarts.openkeeper.game.controller.RoomPlacementValidator;
import toniarts.openkeeper.game.controller.TrapPlacementValidator;
import toniarts.openkeeper.game.controller.TrapSellRules;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.PlayerScreenController;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState.Type;
import toniarts.openkeeper.view.control.IEntityViewControl;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.lighting.LightConstants;
import toniarts.openkeeper.view.lighting.LightRules;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.view.selection.SelectionHandler;
import toniarts.openkeeper.view.text.TextParser;

/**
 * State for managing player interactions in the world. Heavily drawn from
 * Philip Willuweit's AgentKeeper code <p.willuweit@gmx.de>.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author ArchDemon
 */
// TODO: States, now only selection
public abstract class PlayerInteractionState extends AbstractPauseAwareState {

    private static final float CURSOR_UPDATE_INTERVAL = 0.25f;

    private Main app;
    private GameClientState gameClientState;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;

    private final Player player;
    private final IKwdFile kwdFile;
    private final EntityData entityData;
    private final EntitySet constructionBlockingObjects;
    private final EntitySet constructionBlockingDoors;
    private final EntitySet constructionBlockingTraps;
    private final IMapInformation mapInformation;
    private final TextParser textParser;
    private SelectionHandler selectionHandler;
    private CursorIcon cursorIcon;
    private CursorIcon.Price cursorPrice;
    private final Vector2f mousePosition = new Vector2f(Vector2f.ZERO);
    private final InteractionState interactionState = new InteractionState();
    private float timeFromLastUpdate = CURSOR_UPDATE_INTERVAL;
    private Element view;

    private boolean isOnGui = false;
    private boolean isTaggable = false;
    private boolean isOnMap = false;
    private boolean isInteractable = false;

    /**
     * A cursor animation the hand plays once as a reaction, a slap or a drop,
     * before the normal cursor comes back
     */
    private CursorFactory.CursorType reactionCursor;
    private float reactionCursorTimeRemaining = 0f;

    private RawInputListener inputListener;
    private boolean inputListenerAdded = false;
    private final Set<Integer> keys = new HashSet<>();
    private IEntityViewControl interactiveControl;
    private Label tooltip;
    private KeeperHandState keeperHandState;
    private PlayerEntityViewState playerEntityViewState;

    public PlayerInteractionState(Player player, IKwdFile kwdFile, EntityData entityData,
            IMapInformation mapInformation, TextParser textParser) {
        this.player = player;
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        constructionBlockingObjects = entityData.getEntities(ObjectComponent.class, Position.class);
        constructionBlockingDoors = entityData.getEntities(DoorComponent.class, Position.class);
        constructionBlockingTraps = entityData.getEntities(TrapComponent.class, Position.class, Owner.class);
        this.mapInformation = mapInformation;
        this.textParser = textParser;

        // The input
        initializeInput();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        gameClientState = this.stateManager.getState(GameClientState.class);

        PlayerScreenController psc = this.stateManager.getState(PlayerState.class).getScreen();
        this.view = psc.getGuiConstraint();
        this.tooltip = psc.getTooltip();

        // Init the keeper hand
        keeperHandState = new InteractionKeeperHandState();
        this.stateManager.attach(keeperHandState);

        // Init handler
        selectionHandler = new InteractionSelectionHandler();
        cursorIcon = new CursorIcon(assetManager, this.app.getGuiNode(), this.app.getCamera());

        if (!gameClientState.isMultiplayer()) {
            this.stateManager.attach(new InteractionCheatState());
        }

        // Add listener
        if (isEnabled()) {
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled && !inputListenerAdded) {
            app.getInputManager().addRawInputListener(inputListener);
            inputListenerAdded = true;
        } else if (!enabled && inputListenerAdded) {
            app.getInputManager().removeRawInputListener(inputListener);
            inputListenerAdded = false;
            keys.clear();

            // We are no longer around to keep it updated (e.g. entering possession
            // mode), so drop any leftover held-item/spell icon and cursor image
            // rather than leaving them frozen on screen
            keeperHandState.setSpellIcon(null, true);
            keeperHandState.setVisible(false);
            inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.IDLE, assetManager));
        }
    }

    @Override
    public void cleanup() {
        playerEntityViewState = null;
        this.stateManager.detach(keeperHandState);
        keeperHandState = null;
        app.getInputManager().removeRawInputListener(inputListener);
        keys.clear();
        selectionHandler.cleanup();
        cursorIcon.cleanup();
        constructionBlockingObjects.release();
        constructionBlockingDoors.release();
        constructionBlockingTraps.release();
        CheatState cheatState = this.stateManager.getState(CheatState.class);
        if (cheatState != null) {
            this.stateManager.detach(cheatState);
        }

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!isInitialized() || !isEnabled()) {
            return;
        }

        constructionBlockingObjects.applyChanges();
        constructionBlockingDoors.applyChanges();
        constructionBlockingTraps.applyChanges();

        selectionHandler.update(mousePosition);
        cursorIcon.update(mousePosition, interactionState.getType(), isOnMap && !isOnGui,
                selectionHandler.getSelectionColor() != SelectionHandler.ColorIndicator.RED, cursorPrice);
        if (isOnMap && !isOnGui && !isTaggable) {
            updateInteractiveObjectOnCursor();
        }
        updateCursorLight();

        if (reactionCursorTimeRemaining > 0) {
            reactionCursorTimeRemaining -= tpf;
            if (reactionCursorTimeRemaining <= 0) {
                reactionCursorTimeRemaining = 0;
                reactionCursor = null;
                updateCursor();
            }
        }

//        updateStateFlags();
        timeFromLastUpdate += tpf;

        // Update the cursor, the camera might have moved, a creature might have slipped by us... etc.
        if (timeFromLastUpdate > CURSOR_UPDATE_INTERVAL) {
            //updateCursor();
            updateStateFlags();
            timeFromLastUpdate = 0;
        }
    }

    @Override
    public boolean isPauseable() {
        return true;
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

    /**
     * The tile-drag selection box currently being dragged (left mouse
     * button held, covering however many tiles the drag has crossed so
     * far)
     */
    public SelectionArea getActiveSelectionArea() {
        return selectionHandler.isActive() ? selectionHandler.getSelectionArea() : null;
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
    private void updateStateFlags() {
        boolean changed = false;

        boolean value = isCursorOnGUI();
        if (isOnGui != value) {
            isOnGui = value;
            changed = true;
        }

        value = isOnMap();
        if (isOnMap != value) {
            isOnMap = value;
            changed = true;
        }

        value = isTaggable();
        if (isTaggable != value) {
            isTaggable = value;
            changed = true;
        }

        value = isInteractable();
        if (isInteractable != value) {
            isInteractable = value;
            changed = true;
        }

        if (changed) {
            updateCursor();
        }
    }

    private boolean isInteractable() {
        if (isOnGui || !isOnMap || isTaggable) {
            setInteractiveControl(null);
        }

        Point p = null;
        if (interactiveControl != null) {

            // Maybe a kinda hack, but set the tooltip here
            tooltip.setText(interactiveControl.getTooltip(player.getPlayerId()));
            interactiveControl.onHover(player.getPlayerId());
        } else if (isOnMap) {
            p = selectionHandler.getPointedTileIndex();
            tooltip.setText(getTileTooltip(p));
        }

        // If debug, show tile coordinate
        if (Main.isDebug() && (interactiveControl != null || isOnMap)) {
            if (interactiveControl != null) {
                p = WorldUtils.vectorToPoint(((AbstractControl) interactiveControl).getSpatial().getWorldTranslation());
            }
            tooltip.setText(getDebugTooltip(p, tooltip.getText()));
        }

        return interactiveControl != null
                && (interactiveControl.isPickable(player.getPlayerId())
                || interactiveControl.isInteractable(player.getPlayerId()));
    }

    private String getTileTooltip(Point p) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(p);
        if (tile == null) {
            return "";
        }

        IFogOfWarInformation fogOfWarInformation = gameClientState.getFogOfWarInformation();
        if (fogOfWarInformation != null && !fogOfWarInformation.isExplored(p)) {
            return Utils.getMainTextResourceBundle().getString("348");
        }

        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            return getRoomTooltip(tile, terrain);
        }

        return textParser.getMapTileTextParser().parseText(Utils.getMainTextResourceBundle().getString(Integer.toString(terrain.getTooltipStringId())), tile);
    }

    private String getRoomTooltip(IMapTileInformation tile, Terrain terrain) {
        if (tile.getOwnerId() != player.getPlayerId()) {
            return Utils.getMainTextResourceBundle().getString("2471");
        }

        // Override dungeon heart JELLY text
        Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
        String bundleKey = kwdFile.getDungeonHeart() == room ? "2579" : Integer.toString(room.getTooltipStringId());

        return textParser.getRoomTextParser().parseText(Utils.getMainTextResourceBundle().getString(bundleKey),
                tile.getRoomId(), gameClientState.getPlayer(tile.getOwnerId()));
    }

    private static String getDebugTooltip(Point p, String tooltipText) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(p.x + 1);  // 1-based coordinates
        sb.append(", ");
        sb.append(p.y + 1);  // 1-based coordinates
        sb.append("): ");
        sb.append(tooltipText);
        return sb.toString();
    }

    private void updateInteractiveObjectOnCursor() {

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
        if (playerEntityViewState == null) {
            playerEntityViewState = stateManager.getState(Short.toString(player.getPlayerId()), PlayerEntityViewState.class);
        }
        playerEntityViewState.getRoot().collideWith(ray, results);

        // See the results so we see what is going on
        Node object;
        for (int i = 0; i < results.size(); i++) {

            // TODO: Now just creature control, but all interaction objects
            object = results.getCollision(i).getGeometry().getParent().getParent();
            IEntityViewControl control = object.getControl(IEntityViewControl.class);
            if (control != null) {
                setInteractiveControl(control);
                return;
            }
        }
        setInteractiveControl(null);
    }

    private void setInteractiveControl(IEntityViewControl interactiveControl) {

        // If it is the same, don't do anything
        if (interactiveControl != null && interactiveControl.equals(this.interactiveControl)) {
            return;
        }

        // Changed
        if (this.interactiveControl != null) {
            this.interactiveControl.onHoverEnd(player.getPlayerId());
        }
        this.interactiveControl = interactiveControl;
        if (this.interactiveControl != null) {
            this.interactiveControl.onHoverStart(player.getPlayerId());
        }
    }

    private boolean isTaggable() {
        if (isOnGui || !isOnMap) {
            return false;
        }
        Point p = selectionHandler.getPointedTileIndex();
        return (interactionState.getType() == Type.ROOM
                || interactionState.getType() == Type.NONE)
                  && isOnMap && gameClientState.getFogOfWarInformation().isHighlightable(p);
    }

    /**
     * All tile coordinates covered by a selection area's rectangle
     * (inclusive of both corners).
     */
    private static List<Point> getRectPoints(SelectionArea selectionArea) {
        Point start = WorldUtils.vectorToPoint(selectionArea.getStart());
        Point end = WorldUtils.vectorToPoint(selectionArea.getEnd());
        List<Point> points = new ArrayList<>();
        for (int x = start.x; x <= end.x; x++) {
            for (int y = start.y; y <= end.y; y++) {
                points.add(new Point(x, y));
            }
        }
        return points;
    }

    /**
     * The cursor light: follows
     * the point on the world directly under the mouse (continuous, not
     * snapped to tile centres -- see LightRules.updateCursorLight for the
     * deviation), hidden in build mode / off the world / off the map,
     * black (not hidden) over the map border or unexplored ground.
     */
    private void updateCursorLight() {
        PlayerMapViewState mapViewState = stateManager.getState(PlayerMapViewState.class);
        // The map loader thread mutates the light list until the map is loaded
        if (mapViewState == null || !mapViewState.isMapLoaded()) {
            return;
        }
        LightRules lightRules = mapViewState.getMapLoader().getLightRules();

        boolean visible = isOnMap && !isOnGui && interactionState.getType() != Type.ROOM;
        Point p = selectionHandler.getPointedTileIndex();
        boolean onBorder = false;
        if (visible) {
            int width = mapInformation.getMapData().getWidth();
            int height = mapInformation.getMapData().getHeight();
            onBorder = p.x < 1 || p.y < 1 || p.x >= width - 1 || p.y >= height - 1;
        }

        // Over open ground the light is lowered to reach the things standing
        // there. The mouse ray is cast to the light's own height, else the
        // light sits off towards the camera, under the pointer
        // Unexplored ground (incl. the perceived claim scan) is drawn as rock,
        // so it gets the rock height even where the real tile is open
        IFogOfWarInformation fogOfWar = gameClientState.getFogOfWarInformation();
        boolean overSolid = !visible || mapInformation.isSolid(p)
                || (fogOfWar != null && !fogOfWar.isExplored(p));
        float height = overSolid ? LightConstants.CURSOR_LIGHT_HEIGHT : LightConstants.CURSOR_LIGHT_HEIGHT_OPEN;
        Vector2f pointed = selectionHandler.getPointedPositionAtHeight(height);
        Vector3f position = new Vector3f(pointed.x, height, pointed.y);

        // Border tiles: light stays on but black. Over unexplored
        // ground it stays white: the hand lights the fog it points at, which
        // is otherwise completely black (a deviation from the original).
        boolean explored = visible && !onBorder;
         lightRules.updateCursorLight(visible, position, explored);
    }

    private boolean isOnMap() {
        if (isOnGui) {
            return false;
        }
        Point p = selectionHandler.getPointedTileIndex();
        return gameClientState.getMapClientService().getMapData().getTile(p) != null;
    }

    protected void updateCursor() {
        keeperHandState.setVisible(false);

        // Show the spell's icon next to the cursor while it's selected for casting,
        // the same way a grabbed creature or object is shown
        boolean showSpellIcon = !isOnGui && interactionState.getType() == Type.SPELL;
        if (showSpellIcon) {
            KeeperSpell keeperSpell = kwdFile.getKeeperSpellById(interactionState.getItemId());
            boolean castable = isOnMap() && canCastSpell(keeperSpell, interactiveControl, selectionHandler.getPointedTileIndex());
            keeperHandState.setSpellIcon(keeperSpell.getGuiIcon(), castable);
        } else {
            keeperHandState.setSpellIcon(null, true);
        }

        if (Main.getUserSettings().getBoolean(Settings.Setting.USE_CURSORS)) {
            if (reactionCursor != null) {

                // Play the reaction once, then fall through to normal cursor logic
                inputManager.setMouseCursor(CursorFactory.getCursor(reactionCursor, assetManager));
            } else if (isOnGui || isInteractable || interactionState.getType() == Type.SPELL) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));
                if (showSpellIcon) {
                    keeperHandState.setVisible(true);
                }
            } else if (selectionHandler.isActive() && isTaggable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE_TAGGING, assetManager));
            } else if (isTaggable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE, assetManager));
            } else if (keeperHandState.getItem() != null) {

                // Keeper hand item
                inputManager.setMouseCursor(CursorFactory.getCursor(keeperHandState.getItem().getInHandCursor(), assetManager));
                keeperHandState.setVisible(true);
            } else {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.IDLE, assetManager));
            }
        } else if (keeperHandState.getItem() != null || showSpellIcon) {
            keeperHandState.setVisible(true);
        }
    }

    /**
     * Plays a cursor animation once, the normal cursor comes back after it.
     * Takes effect with the next cursor update
     *
     * @param cursor the animation to play
     */
    private void playReactionCursor(CursorFactory.CursorType cursor) {
        reactionCursor = cursor;
        reactionCursorTimeRemaining = CursorFactory.getAnimationDuration(cursor, assetManager);
    }

    private void initializeInput() {
          inputListener = new MapInteractionInputListener();
    }

    /**
     * Handles raw input for the map interaction state. Split into per-button
     * and per-key helper methods (rather than one large listener method) to
     * keep each individual handler's branching manageable.
     */
    private final class MapInteractionInputListener implements RawInputListener {

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
            keeperHandState.setPosition(evt.getX(), evt.getY());

            timeFromLastUpdate = 0;
            updateStateFlags();
            //updateCursor();
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
//            timeFromLastUpdate = 0;
            if (isOnGui || !isOnMap) {
                return;
            }

            if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                onLeftMouseButtonEvent(evt);
            } else if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT && evt.isReleased()) {
                onRightMouseButtonReleased();
            } else if (evt.getButtonIndex() == MouseInput.BUTTON_MIDDLE && evt.isReleased()) {
//                if (Main.isDebug()) {
//                    Point p = selectionHandler.getPointedTileIndex();
//                    getWorldHandler().claimTile(p.x, p.y, player.getPlayerId());
//                }
            }
        }

        private void onLeftMouseButtonEvent(MouseButtonEvent evt) {
            if (evt.isPressed()) {
                onLeftMouseButtonPressed();
            } else if (evt.isReleased() && selectionHandler.isActive()) {
                onLeftMouseButtonReleased();
            }
        }

        private void onLeftMouseButtonPressed() {
            if (interactionState.getType() == Type.SPELL) {
                castSpell(kwdFile.getKeeperSpellById(interactionState.getItemId()), interactiveControl, selectionHandler.getPointedTileIndex(), selectionHandler.getActualPointedPosition());
            } else if (interactionState.getType() == Type.TRAP) {
                Point p = selectionHandler.getPointedTileIndex();
                if (isTrapPlacementValid(p)) {
                    gameClientState.getGameClientService().placeTrap((short) interactionState.getItemId(), p);
                }
            } else if (interactionState.getType() == Type.DOOR) {
                //TODO put door
            } else if (interactionState.getType() == Type.NONE
                    && interactiveControl != null && !keeperHandState.isFull()
                    && interactiveControl.isPickable(player.getPlayerId())) {
                pickupObject(interactiveControl);
            } else if (interactionState.getType() == Type.NONE
                    && interactiveControl != null
                    && interactiveControl.isInteractable(player.getPlayerId())) {
                interactiveControl.interact(player.getPlayerId());
            } else {

                // Selection stuff
                if (selectionHandler.isVisible()) {
                    selectionHandler.setActive(true);
                }

                // I suppose we are tagging
                if (isTaggable) {
                    updateCursor();
                    // The tagging sound is positional and played against the cursor change, not the action itself
                    Point pos = selectionHandler.getPointedTileIndex();
//                    getWorldHandler().playSoundAtTile(pos, GlobalCategory.HAND, GlobalType.HAND_TAG);
                }
            }
        }

        private void onLeftMouseButtonReleased() {
            SelectionArea selectionArea = selectionHandler.getSelectionArea();
            if (interactionState.getType() == Type.NONE
                    || (interactionState.getType() == Type.ROOM
                    && gameClientState.getFogOfWarInformation().isHighlightable(WorldUtils.vectorToPoint(selectionArea.getRealStart())))) {

                // Determine if this is a select/deselect by the starting tile's status.
                // An unexplored start tile has no real server-side selected state to
                // speak of yet, so also fold in whether it currently *looks* tagged.
                Point realStart = WorldUtils.vectorToPoint(selectionArea.getRealStart());
                boolean currentlyTagged = gameClientState.getMapClientService().isSelected(realStart, player.getPlayerId())
                        || gameClientState.getFogOfWarInformation().isPendingTagged(realStart);
                boolean select = !currentlyTagged;
                gameClientState.getGameClientService().selectTiles(selectionArea.getStart(), selectionArea.getEnd(), select);

                // The server only marks a tile selected if its real, hidden terrain is
                // actually taggable - fog can't reach it to know any better. Locally,
                // still-unexplored tiles in this drag should look tagged regardless,
                // until exploration reveals whether that's really true.
                gameClientState.markPendingTaggedTiles(getRectPoints(selectionArea), select);
            } else if (interactionState.getType() == Type.ROOM
                    && RoomPlacementValidator.validate(kwdFile, mapInformation, getConstructionBlockingTiles(),
                            selectionArea.getRealStart(), selectionArea.getRealEnd(), player.getPlayerId(),
                            (short) interactionState.getItemId(),
                            gameClientState.getPlayer(player.getPlayerId()).getGold()).isValid()) {
                gameClientState.getGameClientService().build(selectionArea.getRealStart(), selectionArea.getRealEnd(), (short) interactionState.getItemId());
            } else if (interactionState.getType() == Type.SELL) {

                // Started on a trap: sell just that one, a trap is never sold together with rooms
                Point realStart = WorldUtils.vectorToPoint(selectionArea.getRealStart());
                if (hasSellableTrap(realStart)) {
                    Vector2f tile = WorldUtils.pointToVector2f(realStart);
                    gameClientState.getGameClientService().sell(tile, tile);
                } else {
                    gameClientState.getGameClientService().sell(selectionArea.getStart(), selectionArea.getEnd());
                }
            }

            selectionHandler.setActive(false);
            updateCursor();
        }

        private void onRightMouseButtonReleased() {
            Point p = selectionHandler.getPointedTileIndex();
            if (interactionState.getType() == Type.NONE) {

                // Drop
                IEntityViewControl entityViewControl = keeperHandState.getItem();
                if (entityViewControl != null) {
                    IMapTileInformation mapTile = gameClientState.getMapClientService().getMapData().getTile(p);
                    if (entityViewControl.getDroppableStatus(mapTile, gameClientState.getMapClientService().getTerrain(mapTile), player.getPlayerId()) != IEntityViewControl.DroppableStatus.NOT_DROPPABLE) {
                        gameClientState.getGameClientService().drop(entityViewControl.getEntityId(), p, selectionHandler.getActualPointedPosition(), interactiveControl != null ? interactiveControl.getEntityId() : null);

                        // Show the hand letting go of it, the updateCursor() call below picks it up
                        CursorFactory.CursorType dropCursor = entityViewControl.getDropCursor();
                        if (dropCursor != null) {
                            playReactionCursor(dropCursor);
                        }
                    }
                    //MapTile tile = gameClientState.getMapClientService().getMapData().getTile(p);
//                    IEntityControl.DroppableStatus status = keeperHand.peek().getDroppableStatus(tile, player.getPlayerId());
//                    if (status != IEntityControl.DroppableStatus.NOT_DROPPABLE) {
//
//                        // Drop & update cursor
//                        keeperHand.pop().drop(tile, selectionHandler.getActualPointedPosition(), interactiveControl);
//                        updateCursor();
//                    }
                } else if (interactiveControl != null && interactiveControl.isSlappable(player.getPlayerId())) {
//                    getWorldHandler().playSoundAtTile(p, GlobalCategory.HAND, GlobalType.HAND_SLAP);
                    gameClientState.getGameClientService().interact(interactiveControl.getEntityId());
                    interactiveControl.slap(player.getPlayerId());
                    stateManager.getState(EffectManagerState.class).load(
                            (Node) interactiveControl.getSpatial().getParent(),
                            interactiveControl.getSpatial().getWorldTranslation(),
                            interactiveControl.getSlapEffectId(player.getPlayerId()), false, interactiveControl.getOwnerId());

                    // Flash the slap cursor once; the updateCursor() call below
                    // picks it up, and update(tpf) reverts it once it elapses
                    playReactionCursor(CursorFactory.CursorType.SLAP);
                } else if (interactiveControl != null && interactiveControl.isInteractable(player.getPlayerId())) {
                    gameClientState.getGameClientService().interact(interactiveControl.getEntityId());
                    interactiveControl.interact(player.getPlayerId());
                } else if (Main.isDebug()) {
                    // taggable -> "dig"
//                    if (getWorldHandler().isTaggable(p.x, p.y)) {
//                        getWorldHandler().digTile(p.x, p.y);
//                    } // ownable -> "claim"
//                    else if (getWorldHandler().isClaimable(p.x, p.y, player.getPlayerId())) {
//                        getWorldHandler().claimTile(p.x, p.y, player.getPlayerId());
//                    }
                }
            }

            // Reset the state
            setInteractionState(Type.NONE, 0);
            updateCursor();

            selectionHandler.setActive(false);
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            updateModifierKeys(evt);

            if (evt.isPressed()) {
                handleKeyPress(evt);
            }

            if (evt.isPressed() && evt.getKeyCode() == (int) Settings.Setting.TOGGLE_GUI.getDefaultValue()) {
                stateManager.getState(PlayerState.class).getScreen().toggleGui();
            }
        }

        private void updateModifierKeys(KeyInputEvent evt) {

            // See the CTRL + ALT
            switch (evt.getKeyCode()) {
                case KeyInput.KEY_LCONTROL:
                case KeyInput.KEY_RCONTROL:
                    if (evt.isPressed()) {
                        keys.add(KeyInput.KEY_LCONTROL);
                        keys.add(KeyInput.KEY_RCONTROL);
                    } else {
                        keys.remove(Integer.valueOf(KeyInput.KEY_LCONTROL));
                        keys.remove(Integer.valueOf(KeyInput.KEY_RCONTROL));
                    }
                    break;

                case KeyInput.KEY_LMENU:
                case KeyInput.KEY_RMENU:
                    if (evt.isPressed()) {
                        keys.add(KeyInput.KEY_LMENU);
                        keys.add(KeyInput.KEY_RMENU);
                    } else {
                        keys.remove(Integer.valueOf(KeyInput.KEY_LMENU));
                        keys.remove(Integer.valueOf(KeyInput.KEY_RMENU));
                    }
                    break;
            }
        }

        private void handleKeyPress(KeyInputEvent evt) {
            if (evt.getKeyCode() == KeyInput.KEY_C && keys.contains(KeyInput.KEY_LCONTROL) && keys.contains(KeyInput.KEY_LMENU)) {
                CheatState cheat = stateManager.getState(CheatState.class);
                if (cheat != null && !cheat.isEnabled()) {
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

        @Override
        public void onTouchEvent(TouchEvent evt) {
        }

    }

    /**
     * Cast a keeper spell
     *
     * @param keeperSpell the spell
     * @param object pointed object, can be null
     * @param tile the tile index
     * @param position the actual cursor position
     * @return true if spell can be cast
     */
    private boolean castSpell(KeeperSpell keeperSpell, IEntityViewControl object, Point tile, Vector2f position) {
        if (!canCastSpell(keeperSpell, object, tile)) {
            return false;
        }
        gameClientState.getGameClientService().castKeeperSpell(keeperSpell.getId(), object != null ? object.getEntityId() : null, tile, position);

        return true;
    }

    private boolean canCastSpell(KeeperSpell keeperSpell, IEntityViewControl object, Point tile) {
        Creature creature = object != null && object.getDataObject() instanceof Creature targetCreature ? targetCreature : null;
        Short owner = object != null ? object.getControlId() : null;
        return KeeperSpellCastValidator.isValidCast(keeperSpell, mapInformation, mapInformation.getMapData().getTile(tile), gameClientState.getPlayer(), creature, owner);
    }

    private Set<Point> getConstructionBlockingTiles() {
        Set<Point> blockedTiles = new HashSet<>();
        addConstructionBlockingTiles(blockedTiles, constructionBlockingObjects, this::isConstructionBlockingObject);
        addConstructionBlockingTiles(blockedTiles, constructionBlockingDoors, entity -> true);
        addConstructionBlockingTiles(blockedTiles, constructionBlockingTraps, entity -> true);
        return blockedTiles;
    }

    private boolean isTrapPlacementValid(Point tile) {
        Set<Point> occupiedTiles = new HashSet<>();
        addConstructionBlockingTiles(occupiedTiles, constructionBlockingDoors, entity -> true);
        addConstructionBlockingTiles(occupiedTiles, constructionBlockingTraps, entity -> true);
        return TrapPlacementValidator.validate(mapInformation, occupiedTiles, tile, player.getPlayerId(),
                kwdFile.getTrapById(interactionState.getItemId()),
                gameClientState.getPlayer(player.getPlayerId()).getGold()) == TrapPlacementValidator.Failure.NONE;
    }

    private boolean hasSellableTrap(Point tile) {
        for (Entity trap : constructionBlockingTraps) {
            if (TrapSellRules.isSellable(trap.get(Owner.class).ownerId, player.getPlayerId())
                    && WorldUtils.vectorToPoint(trap.get(Position.class).position).equals(tile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConstructionBlockingObject(Entity entity) {
        ObjectComponent objectComponent = entity.get(ObjectComponent.class);
        return RoomPlacementValidator.isConstructionBlockingObject(kwdFile.getObject(objectComponent.objectId));
    }

    private static void addConstructionBlockingTiles(Set<Point> blockedTiles, EntitySet entities,
            Predicate<Entity> isBlocking) {
        for (Entity entity : entities) {
            if (!isBlocking.test(entity)) {
                continue;
            }
            Position position = entity.get(Position.class);
            if (position != null) {
                blockedTiles.add(WorldUtils.vectorToPoint(position.position));
            }
        }
    }

    /**
     * Picks up an object, places it in Keeper's hand
     *
     * @param object the object to pickup
     * @return picked or not
     */
    public boolean pickupObject(IEntityViewControl object) {
        if (object == null || keeperHandState.isFull() || !object.isPickable(player.getPlayerId())) {
            return false;
        }

        //keeperHand.push(object.pickUp(player.getPlayerId()));
        gameClientState.getGameClientService().pickUp(object.getEntityId());
        updateCursor();
        return true;
    }

    /**
     * Checks if the Keeper hand is full
     *
     * @return is keeper hand full
     */
    public boolean isKeeperHandFull() {
        return keeperHandState.isFull();
    }

    /**
     * A callback for changing the interaction state
     *
     * @param interactionState new state
     */
    protected abstract void onInteractionStateChange(InteractionState interactionState);

    private final class InteractionKeeperHandState extends KeeperHandState {

        private InteractionKeeperHandState() {
            super((int) gameClientState.getLevelVariable(MiscType.MAX_NUMBER_OF_THINGS_IN_HAND),
                    kwdFile, entityData, player.getPlayerId());
        }

        @Override
        protected void updateCursor() {
            PlayerInteractionState.this.updateCursor();
        }
    }

    private final class InteractionSelectionHandler extends SelectionHandler {

        private long roomCost;

        private InteractionSelectionHandler() {
            super(app);
        }

        @Override
        public boolean isVisible() {
            if (isTaggable || isActive()) {
                return true;
            }
            if (!isOnMap) {
                return false;
            }

            return switch (interactionState.getType()) {
                case NONE -> keeperHandState.getItem() != null;
                case SELL, ROOM, DOOR, TRAP -> true;
                default -> false;
            };
        }

        @Override
        protected boolean isBlock(Point tile) {

            // Beyond the edge there is nothing to see through to
            IMapTileInformation mapTile = mapInformation.getMapData().getTile(tile);
            if (mapTile == null) {
                return true;
            }

            // Whatever the fog hides is drawn as plain rock, see MapViewController
            IFogOfWarInformation fogOfWarInformation = gameClientState.getFogOfWarInformation();
            if (fogOfWarInformation != null && !fogOfWarInformation.isVisible(tile)) {
                return true;
            }

            return mapInformation.getTerrain(mapTile).getFlags().contains(Terrain.TerrainFlag.SOLID);
        }

        @Override
        protected ColorIndicator getColorIndicator() {
            roomCost = interactionState.getType() == Type.ROOM ? getRoomCost() : 0;
            cursorPrice = getPrice();
            Point point = isActive()
                    ? WorldUtils.vectorToPoint(getSelectionArea().getRealStart())
                    : WorldUtils.vectorToPoint(getPointedTilePosition());
            if (interactionState.getType() == Type.NONE && keeperHandState.getItem() != null) {
                return getDropColor(point);
            }
            if (interactionState.getType() == Type.SELL) {
                return gameClientState.getMapClientService().isSellable(point, player.getPlayerId()) || hasSellableTrap(point)
                        ? ColorIndicator.BLUE : ColorIndicator.RED;
            }
            if (interactionState.getType() == Type.TRAP) {
                return isTrapPlacementValid(point) ? ColorIndicator.BLUE : ColorIndicator.RED;
            }
            if (isInvalidRoomSelection(point)) {
                return ColorIndicator.RED;
            }
            return ColorIndicator.BLUE;
        }

        private ColorIndicator getDropColor(Point point) {
            IMapTileInformation tile = gameClientState.getMapClientService().getMapData().getTile(point);
            if (tile == null) {
                return ColorIndicator.RED;
            }
            IEntityViewControl.DroppableStatus status = keeperHandState.getItem().getDroppableStatus(
                    tile, gameClientState.getMapClientService().getTerrain(tile), player.getPlayerId());
            return status == IEntityViewControl.DroppableStatus.NOT_DROPPABLE
                    ? ColorIndicator.RED : ColorIndicator.BLUE;
        }

        private boolean isInvalidRoomSelection(Point point) {
            return interactionState.getType() == Type.ROOM
                    && !gameClientState.getFogOfWarInformation().isHighlightable(point)
                    && !isRoomSelectionValid();
        }

        /**
         * The room can be built if it has tiles to build on and the keeper can pay for them
         */
        private boolean isRoomSelectionValid() {
            return roomCost > 0 && roomCost <= gameClientState.getPlayer(player.getPlayerId()).getGold();
        }

        /**
         * What the room selection costs, validated with any amount of gold so that the price shows even when the
         * keeper can't afford it
         *
         * @return the cost, 0 if nothing can be built there
         */
        private long getRoomCost() {
            SelectionArea area = getSelectionArea();
            RoomPlacementValidator.Result result = RoomPlacementValidator.validate(kwdFile, mapInformation,
                    getConstructionBlockingTiles(), area.getRealStart(), area.getRealEnd(), player.getPlayerId(),
                    (short) interactionState.getItemId(), Integer.MAX_VALUE);
            if (!result.isValid()) {
                return 0;
            }
            return (long) result.plots().size() * kwdFile.getRoomById(interactionState.getItemId()).getCost();
        }

        /**
         * The price the original shows next to the hand for the selection
         *
         * @return the price, {@code null} for none
         */
        private CursorIcon.Price getPrice() {
            int gold = gameClientState.getPlayer(player.getPlayerId()).getGold();
            return switch (interactionState.getType()) {
                case ROOM ->
                    CursorIcon.Price.cost(roomCost, gold);
                case DOOR ->
                    CursorIcon.Price.cost(kwdFile.getDoorById(interactionState.getItemId()).getGoldCost(), gold);
                case TRAP ->
                    CursorIcon.Price.cost(kwdFile.getTrapById(interactionState.getItemId()).getGoldCost(), gold);
                case SELL ->
                    CursorIcon.Price.sale(getSellValue());
                default ->
                    null;
            };
        }

        /**
         * What selling the own rooms in the selection gives back
         *
         * @return the gold
         */
        private int getSellValue() {
            SelectionArea area = getSelectionArea();
            float percentage = gameClientState.getLevelVariable(MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST);
            int value = 0;
            for (int x = (int) Math.min(area.getRealStart().x, area.getRealEnd().x); x <= (int) Math.max(area.getRealStart().x, area.getRealEnd().x); x++) {
                for (int y = (int) Math.min(area.getRealStart().y, area.getRealEnd().y); y <= (int) Math.max(area.getRealStart().y, area.getRealEnd().y); y++) {
                    Point point = new Point(x, y);
                    if (gameClientState.getMapClientService().isSellable(point, player.getPlayerId())) {
                        IMapTileInformation tile = gameClientState.getMapClientService().getMapData().getTile(point);
                        value += (int) (kwdFile.getRoomByTerrain(tile.getTerrainId()).getCost() * (percentage / 100));
                    }
                }
            }
            return value;
        }
    }

    private final class InteractionCheatState extends CheatState {

        private InteractionCheatState() {
            super(app);
        }

        @Override
        public void onSuccess(CheatType cheat) {
            gameClientState.getGameClientService().triggerCheat(cheat);
        }
    }

    public static final class InteractionState {

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
