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

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Console;
import de.lessvoid.nifty.controls.ConsoleCommands;
import static toniarts.openkeeper.game.state.PlayerState.HUD_SCREEN_ID;

/**
 *
 * @author ArchDemon
 */
public class GameConsole {
    private final Console console;

    public GameConsole(Nifty nifty) {
        this.console = nifty.getScreen(HUD_SCREEN_ID).findNiftyControl("console", Console.class);
        initialize(nifty);
    }

    // TODO add normal commands if needed
    private void initialize(Nifty nifty) {
        ConsoleCommands consoleCommands = new ConsoleCommands(nifty, console);
        /*
        ConsoleCommands.ConsoleCommand simpleCommand = new SimpleCommand();
        consoleCommands.registerCommand("simple", simpleCommand);

        ConsoleCommands.ConsoleCommand showCommand = new ShowCommand();
        consoleCommands.registerCommand("show", showCommand);
        */

        consoleCommands.enableCommandCompletion(true);
    }

    public void setVisible(boolean visible) {
        console.getElement().setVisible(visible);
        if (visible) {
            console.getTextField().setFocus();
            //console.setFocus();
        }
    }

    public boolean isVisible() {
        return console.getElement().isVisible();
    }

    public Console getConsole() {
        return console;
    }

    public class SimpleCommand implements ConsoleCommands.ConsoleCommand {
        @Override
        public void execute(final String[] args) {
            System.out.println(args[0]);
            if (args.length > 1) {
                for (String a : args) {
                    System.out.println(a);
                }
            }
        }
    }

    private class ShowCommand implements ConsoleCommands.ConsoleCommand {
        @Override
        public void execute(final String[] args) {
            if (args.length > 1) {
                System.out.println(args[0] + " " + args[1]);
            }
        }
    }
}
