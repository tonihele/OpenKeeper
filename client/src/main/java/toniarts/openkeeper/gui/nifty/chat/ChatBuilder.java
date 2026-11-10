/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.chat;

import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.tools.SizeValue;
import javax.annotation.Nonnull;

/**
 * @author ractoc
 */
public final class ChatBuilder extends ControlBuilder {

    public ChatBuilder(final int lines) {
        super("nifty-chat");
        lines(lines);
    }

    public ChatBuilder(@Nonnull final String id, final int lines) {
        super(id, "nifty-chat");
        lines(lines);
    }

    public void lines(final int lines) {
        set("lines", String.valueOf(lines));
    }

    public void sendLabel(@Nonnull final String sendLabel) {
        set("sendLabel", sendLabel);
    }

    public void chatLineIconWidth(@Nonnull final SizeValue value) {
        set("chatLineIconWidth", value.getValueAsString());
    }

    public void chatLineIconHeight(@Nonnull final SizeValue value) {
        set("chatLineIconHeight", value.getValueAsString());
    }

    public void chatLineHeight(@Nonnull final SizeValue value) {
        set("chatLineHeight", value.getValueAsString());
    }
}
