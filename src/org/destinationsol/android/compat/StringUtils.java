/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.destinationsol.android.compat;

import java.util.Iterator;

public final class StringUtils {
    private StringUtils() {
    }

    public static String join(CharSequence delimiter, CharSequence... elements) {
        if (elements.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(elements[0]);
        for (int elementNo = 1; elementNo < elements.length; elementNo++) {
            builder.append(delimiter);
            builder.append(elements[elementNo]);
        }

        return builder.toString();
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        StringBuilder builder = new StringBuilder();
        Iterator<? extends CharSequence> elementIterator = elements.iterator();
        while (true) {
            builder.append(elementIterator.next());
            if (elementIterator.hasNext()) {
                builder.append(delimiter);
            } else {
                break;
            }
        }

        return builder.toString();
    }
}
