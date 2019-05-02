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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * State for handling cheats
 *
 * @author ufdada
 */
public abstract class CheatState extends AbstractPauseAwareState implements RawInputListener {

    private static final Logger LOGGER = Logger.getLogger(CheatState.class.getName());
    private final Application app;
    private String cheat = "";
    private boolean success = false;

    public enum CheatType {

        MONEY("show me the money"),
        MANA("ha ha thisaway ha ha thataway"),
        LEVEL_MAX("feel the power"),
        UNLOCK_ROOMS("this is my church"),
        UNLOCK_SPELLS("i believe its magic"),
        UNLOCK_ROOMS_TRAPS("fit the best"),
        REMOVE_FOW("now the rain has gone"),
        WIN_LEVEL("do not fear the reaper");
        private final String cheatMessage;

        private CheatType(final String cheatMessage) {
            this.cheatMessage = cheatMessage;
        }

        public String getMessage() {
            return this.cheatMessage;
        }

        public static CheatType getCheatType(final String cheatMessage) {
            for (CheatType type : CheatType.values()) {
                if (type.getMessage().equals(cheatMessage)) {
                    return type;
                }
            }
            return null;
        }

        public static boolean hasCheat(final String cheatMessage) {
            for (CheatType type : CheatType.values()) {
                if (type.getMessage().startsWith(cheatMessage)) {
                    return true;
                }
            }
            return false;
        }
    }

    public CheatState(final Application app) {
        this.app = app;
        super.setEnabled(false);
    }

    private void playSound(boolean enabled) {
        String file = String.format("Sounds/Global/MistressHW/ms_1ft0%s.mp2", enabled ? "1" : "2");
        AudioNode sound = new AudioNode(app.getAssetManager(), file, DataType.Buffer);
        sound.setLooping(false);
        sound.setPositional(false);
        sound.play();
    }

    @Override
    public boolean isPauseable() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.playSound(enabled);
        if (enabled) {
            LOGGER.log(Level.INFO, "Cheat mode activated. Please enter your cheat code");
            this.app.getInputManager().addRawInputListener(this);
        } else {
            this.app.getInputManager().removeRawInputListener(this);
            if (!this.success) {
                LOGGER.log(Level.INFO, "Wrong cheat code. You entered {0}", this.cheat);
            }
            this.cheat = "";
        }
        this.success = false;
    }

    @Override
    public void cleanup() {
        this.app.getInputManager().removeRawInputListener(this);
        this.cheat = "";
        this.success = false;
        super.cleanup();
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (evt.isPressed() && evt.getKeyChar() != 0) {
            this.cheat += evt.getKeyChar();

            // Leave cheat mode if message was incorrect
            if (!CheatType.hasCheat(this.cheat)) {
                this.setEnabled(false);
            }

            // Check if cheat code is completely entered
            CheatType cheatCode = CheatType.getCheatType(this.cheat);
            if (cheatCode != null) {
                executeCheat(cheatCode);
                this.success = true;
                this.setEnabled(false);
            }
        }
    }

    public void executeCheat(@Nonnull CheatType cheatCode) {
        LOGGER.log(Level.INFO, "Executing cheat {0}", cheatCode.toString());
        this.onSuccess(cheatCode);
    }

    public abstract void onSuccess(CheatType cheat);

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
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
    }
}
