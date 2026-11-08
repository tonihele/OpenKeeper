/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

/**
 * Builds {@link TriggerAction} fixtures for the tests of the actions that
 * show something to the player. Lives in the trigger's own package because
 * its setters are {@code protected}.
 */
public final class PresentationActionFixtures {

    private PresentationActionFixtures() {
    }

    /**
     * A "play speech" action
     *
     * @param id the trigger id
     * @param speechId the speech
     * @return the action
     */
    public static TriggerAction playSpeech(int id, int speechId) {
        TriggerAction action = action(id, TriggerAction.ActionType.PLAY_SPEECH);
        action.setUserData("speechId", speechId);
        action.setUserData("pathId", 0);
        action.setUserData("introduction", (short) 0);
        action.setUserData("text", (short) 0);
        return action;
    }

    /**
     * A "set widescreen mode" action
     *
     * @param id the trigger id
     * @param enabled widescreen on or off
     * @return the action
     */
    public static TriggerAction setWidescreen(int id, boolean enabled) {
        TriggerAction action = action(id, TriggerAction.ActionType.SET_WIDESCREEN_MODE);
        action.setUserData("available", (short) (enabled ? 1 : 0));
        return action;
    }

    /**
     * A "display text string" action
     *
     * @param id the trigger id
     * @param textId the text
     * @return the action
     */
    public static TriggerAction displayText(int id, int textId) {
        TriggerAction action = action(id, TriggerAction.ActionType.DISPLAY_TEXT_STRING);
        action.setUserData("textId", textId);
        return action;
    }

    private static TriggerAction action(int id, TriggerAction.ActionType type) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setRepeatTimes((short) 1);
        action.setType(type);
        return action;
    }
}
