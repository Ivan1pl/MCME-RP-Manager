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

package com.mcmiddleearth.rpmanager.events;

import java.util.List;

public class BlockstateCloneNewNamesSelectedEvent implements Event {
    private final Object source;
    private final NewNames newNames;

    public BlockstateCloneNewNamesSelectedEvent(Object source, NewNames newNames) {
        this.source = source;
        this.newNames = newNames;
    }

    @Override
    public Object getSource() {
        return source;
    }

    public NewNames getNewNames() {
        return newNames;
    }

    public static class NewNames {
        private final String newName;
        private final List<String> newTextureNames;

        public NewNames(String newName, List<String> newTextureNames) {
            this.newName = newName;
            this.newTextureNames = newTextureNames;
        }

        public String getNewName() {
            return newName;
        }

        public List<String> getNewTextureNames() {
            return newTextureNames;
        }
    }
}
