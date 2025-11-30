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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseModel implements ExtraFieldsHolder {
    protected static Set<String> BASE_KNOWN_FIELDS = Set.of("parent", "display", "textures", "elements");
    private String parent;
    private Map<Position, Display> display;
    private Map<String, String> textures;
    private List<Element> elements;
    private transient Map<String, Object> extra = new HashMap<>();

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Map<Position, Display> getDisplay() {
        return display;
    }

    public void setDisplay(Map<Position, Display> display) {
        this.display = display;
    }

    public Map<String, String> getTextures() {
        return textures;
    }

    public void setTextures(Map<String, String> textures) {
        this.textures = textures;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    @Override
    public Map<String, Object> getExtra() {
        return extra;
    }

    @Override
    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
