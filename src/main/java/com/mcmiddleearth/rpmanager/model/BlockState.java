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

import java.util.List;
import java.util.Map;

public class BlockState implements JsonRoot {
    @JsonAdapter(VariantsJsonAdapter.Factory.class)
    private Map<String, List<Model>> variants;
    private List<Case> multipart;

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
}
