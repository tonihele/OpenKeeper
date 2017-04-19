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
package toniarts.openkeeper.game.console;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector2f;
import de.lessvoid.nifty.controls.Console;
import de.lessvoid.nifty.controls.ConsoleCommands;
import de.lessvoid.nifty.controls.ConsoleCommands.ConsoleCommand;
import de.lessvoid.nifty.tools.Color;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.room.ICreatureEntrance;

/**
 *
 * @author ArchDemon
 */
public class GameConsole {
    private final Console console;
    private ConsoleCommands consoleCommands;
    private boolean welcomeMessageShown = false;
    private final Color messageOutputColor = new Color("#bbbbbbff");
    private final AppStateManager stateManager;
    private final Keeper keeper;

    /**
     * These Commands need at least one parameter to work
     */
    private enum ParameterCommands {
        ADD_GOLD,
        ADD_MANA,
        SPAWN_CREATURE,
        SPAWN_IMPS;
    };

    /**
     * These commands don't have parameter
     */
    private enum SimpleCommands {
        CLEAR,
        HELP,
        SPAWN_IMP,
        EXIT;
    };

    GameConsole(AppStateManager stateManager) {
        this.stateManager = stateManager;
        this.console = stateManager.getState(PlayerState.class).getScreen().getConsole();
        this.keeper = stateManager.getState(PlayerState.class).getPlayer();
        initialize();
    }

    private void initialize() {
        consoleCommands = new ConsoleCommands(console.getElement().getNifty(), console);

        ConsoleCommand parameterCommand = new ParameterCommand();

        for (ParameterCommands parameterCmd : ParameterCommands.values()) {
            consoleCommands.registerCommand(parameterCmd.toString().toLowerCase(), parameterCommand);
        }

        ConsoleCommand simpleCommand = new SimpleCommand();
        for (SimpleCommands simpleCmd : SimpleCommands.values()) {
            consoleCommands.registerCommand(simpleCmd.toString().toLowerCase(), simpleCommand);
        }

        consoleCommands.enableCommandCompletion(true);
    }

    public void setVisible(boolean visible) {
        console.getElement().setVisible(visible);
        if (visible) {
            console.getTextField().setFocus();
            //console.setFocus();
            welcomeMessageShown = welcomeMessageShown || showHelpMessage();
        }
    }

    public boolean isVisible() {
        return console.getElement().isVisible();
    }

    public Console getConsole() {
        return console;
    }

    public class SimpleCommand implements ConsoleCommand {
        @Override
        public void execute(final String[] args) {
            String command = args[0].toUpperCase();
            switch (SimpleCommands.valueOf(command)) {
                case CLEAR:
                    console.clear();
                    break;
                case HELP:
                    showHelpMessage();
                    break;
                case SPAWN_IMP:
                    spawnImp();
                    break;
                case EXIT:
                    stateManager.getState(ConsoleState.class).setEnabled(false);
                    break;
                default:
                    console.outputError("Not supported");
            }
        }
    }

    private class ParameterCommand implements ConsoleCommand {

        @Override
        public void execute(final String[] args) {
            String command = args[0].toUpperCase();
            if (args.length == 1) {
                console.outputError("This command needs at least one parameter");
                return;
            }

            switch (ParameterCommands.valueOf(command)) {
                case ADD_GOLD:
                    try {
                        int amount = Integer.parseInt(args[1]);
                        stateManager.getState(WorldState.class).addGold(keeper.getId(), amount);
                    } catch (NumberFormatException e) {
                        console.outputError("First parameter must be a number!");
                    }
                    break;
                case ADD_MANA:
                    try {
                        int amount = Integer.parseInt(args[1]);
                        keeper.getManaControl().addMana(amount);
                    } catch (NumberFormatException e) {
                        console.outputError("First parameter must be a number!");
                    }
                    break;
                case SPAWN_IMPS:
                    try {
                        int amount = Integer.parseInt(args[1]);
                        spawnImps(amount);
                    } catch (NumberFormatException e) {
                        console.outputError("First parameter must be the amount of imps!");
                    }
                    break;
                case SPAWN_CREATURE:
                    try {
                        short amount = Short.parseShort(args[1]);
                        spawnCreature(amount, (short) 1);
                    } catch (NumberFormatException e) {
                        console.outputError("First parameter must be the creature id!");
                    }
                    break;
                default:
                    console.outputError("Not supported");
            }
        }
    }

    private void spawnImps(final int amount) {
        for (int i = 0; i < amount; i++) {
            spawnImp();
        }
    }

    private void spawnImp() {
        spawnCreature(keeper.getCreatureControl().getImp().getCreatureId(), (short) 1);
    }

    private void spawnCreature(short creatureId, short level) {
        Vector2f dhEntrance = WorldUtils.pointToVector2f(((ICreatureEntrance) keeper.getRoomControl().getDungeonHeart()).getEntranceCoordinate());
        stateManager.getState(WorldState.class).getThingLoader().spawnCreature(creatureId, keeper.getId(), level, dhEntrance, false, null);
    }

    private boolean showHelpMessage() {
        StringBuilder outputText = new StringBuilder();
        outputText.append("##########################################\n");
        outputText.append("### You can use the following commands ###\n");
        outputText.append("##########################################\n");
        ConsoleCommand simpleCommand = new SimpleCommand();
        for (SimpleCommands simpleCmd : SimpleCommands.values()) {
            outputText.append("    ");
            outputText.append(simpleCmd.toString().toLowerCase());
            outputText.append("\n");
        }

        for (ParameterCommands parameterCmd : ParameterCommands.values()) {
            outputText.append("    ");
            outputText.append(parameterCmd.toString().toLowerCase().concat(" <PARAMETER>"));
            outputText.append("\n");
        }

        outputText.append("##########################################");
        console.output(outputText.toString(), messageOutputColor);
        return true;
    }
}
