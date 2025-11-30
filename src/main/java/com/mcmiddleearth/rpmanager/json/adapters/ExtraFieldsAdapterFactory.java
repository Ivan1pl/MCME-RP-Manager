/*
 * Copyright (C) 2025 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mcmiddleearth.rpmanager.json.adapters;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mcmiddleearth.rpmanager.model.ExtraFieldsHolder;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ExtraFieldsAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        // Only handle classes implementing ExtraFieldsHolder
        if (!ExtraFieldsHolder.class.isAssignableFrom(type.getRawType())) {
            return null;
        }

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        return new TypeAdapter<T>() {

            @Override
            public void write(JsonWriter out, T value) throws IOException {
                ExtraFieldsHolder holder = (ExtraFieldsHolder) value;

                // Serialize known fields first
                JsonObject obj = delegate.toJsonTree(value).getAsJsonObject();

                // Add extra (unknown) fields
                for (var entry : holder.getExtra().entrySet()) {
                    obj.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
                }

                elementAdapter.write(out, obj);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonObject obj = elementAdapter.read(in).getAsJsonObject();

                // Deserialize known fields
                T instance = delegate.fromJsonTree(obj);

                ExtraFieldsHolder holder = (ExtraFieldsHolder) instance;
                Set<String> known = holder.getKnownFields();
                Map<String, Object> extra = new LinkedHashMap<>();

                // Extract unknown fields (those not in knownFields)
                for (var entry : obj.entrySet()) {
                    String name = entry.getKey();
                    if (!known.contains(name)) {
                        extra.put(name, gson.fromJson(entry.getValue(), Object.class));
                    }
                }

                holder.setExtra(extra);

                return instance;
            }
        };
    }
}
