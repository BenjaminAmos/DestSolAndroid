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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class JSONObject {
    public static Set<String> keySet(org.json.JSONObject jsonObject) {
        Iterator<String> keyIterator = jsonObject.keys();
        Set<String> keys = new HashSet<String>();
        while (keyIterator.hasNext()) {
            keys.add(keyIterator.next());
        }

        return keys;
    }

    public static float getFloat(org.json.JSONObject jsonObject, String name) {
        try {
            return (float) jsonObject.getDouble(name);
        } catch (org.json.JSONException ignore) {
            return Float.NaN;
        }
    }

    public static float optFloat(org.json.JSONObject jsonObject, String name) {
        return (float) jsonObject.optDouble(name);
    }

    public static float optFloat(org.json.JSONObject jsonObject, String name, float fallback) {
        return (float) jsonObject.optDouble(name, fallback);
    }
}
