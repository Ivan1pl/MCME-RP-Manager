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

import java.lang.Override;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockModel extends BaseModel implements JsonRoot {
    private static final Set<String> KNOWN_FIELDS =
            Stream.concat(BASE_KNOWN_FIELDS.stream(), Stream.of("ambientocclusion")).collect(Collectors.toSet());
    private Boolean ambientocclusion;

    public Boolean getAmbientocclusion() {
        return ambientocclusion;
    }

    public void setAmbientocclusion(Boolean ambientocclusion) {
        this.ambientocclusion = ambientocclusion;
    }

    @Override
    public Set<String> getKnownFields() {
        return KNOWN_FIELDS;
    }
}
