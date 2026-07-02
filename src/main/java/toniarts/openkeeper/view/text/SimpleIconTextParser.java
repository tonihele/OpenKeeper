/*
 * Copyright (C) 2014-2022 OpenKeeper
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
package toniarts.openkeeper.view.text;

import toniarts.openkeeper.utils.TextUtils;

/**
 * Parses hints and labels present in GUI icons
 *
 * @param <T> The base object type that the icon represents
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class SimpleIconTextParser<T> {

    public SimpleIconTextParser() {
    }

    public String parseText(String text, T dataObject) {
        return TextUtils.parseText(text, (index) -> {
            return getReplacement(index, dataObject);
        });
    }

    protected abstract String getReplacement(int index, T dataObject);

}
