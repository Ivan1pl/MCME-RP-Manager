/*
 * Copyright (C) 2021 MCME
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

package com.mcmiddleearth.rpmanager.model;

import com.google.gson.annotations.JsonAdapter;
import com.mcmiddleearth.rpmanager.json.adapters.VariantsJsonAdapter;

import java.lang.Override;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockState implements JsonRoot, ExtraFieldsHolder {
    private static final Set<String> KNOWN_FIELDS = Set.of("variants", "multipart");

    @JsonAdapter(VariantsJsonAdapter.Factory.class)
    private Map<String, List<Model>> variants;
    private List<Case> multipart;
    private transient Map<String, Object> extra = new HashMap<>();

    public Map<String, List<Model>> getVariants() {
        return variants;
    }

    public void setVariants(Map<String, List<Model>> variants) {
        this.variants = variants;
    }

    public List<Case> getMultipart() {
        return multipart;
    }

    public void setMultipart(List<Case> multipart) {
        this.multipart = multipart;
    }

    @Override
    public Map<String, Object> getExtra() {
        return extra;
    }

    @Override
    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    @Override
    public Set<String> getKnownFields() {
        return KNOWN_FIELDS;
    }
}
