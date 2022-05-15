/*
 * Copyright (C) 2022 MCME
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

package com.mcmiddleearth.rpmanager.utils.loaders;

import com.mcmiddleearth.rpmanager.model.ItemModel;
import com.mcmiddleearth.rpmanager.model.project.Layer;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ItemModelFileLoader extends AbstractFileLoader {
    @Override
    public Object loadFile(Layer layer, Object[] path) throws IOException {
        return loadFile(layer, path, ItemModel.class);
    }

    @Override
    public boolean canLoad(Layer layer, Object[] path) {
        return path != null && path.length > 0 && path[path.length - 1].toString().endsWith(".json") &&
                isItemModelPath(Arrays.stream(path).map(Object::toString)
                        .skip(layer.getFile().getName().endsWith(".jar") ? 0L : 1L)
                        .limit(4L)
                        .collect(Collectors.joining("/")));
    }

    private static boolean isItemModelPath(String path) {
        return path.startsWith("assets/minecraft/models/") && !path.equals("assets/minecraft/models/block");
    }
}