/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Dungeon Keeper strings have some parameters which are shared between all
 * the strings. They all start with '%'. If '%' is wanted to be displayed, it is
 * entered twice: '%%'. The parameters are simple integer indexes. For example
 * '%37' -> index 37 (37 always means health).
 * <p>
 * Maybe this is a sufficient parser implementation. Aho-Corasick algorithm
 * could be one. But lets face it, our strings are very short.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TextUtils {

    private final static Pattern PATTERN = Pattern.compile("%(\\d+|%)");

    private TextUtils() {
    }

    public static String parseText(String text, TextReplacer replacer) {
        Matcher matcher = PATTERN.matcher(text);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String param = matcher.group(1);
            if ("%".equals(param)) {
                matcher.appendReplacement(sb, "%");
            } else {
                matcher.appendReplacement(sb, replacer.getReplacement(Integer.parseInt(param)));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
