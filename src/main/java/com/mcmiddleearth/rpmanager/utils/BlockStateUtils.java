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

package com.mcmiddleearth.rpmanager.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlockStateUtils {
    private static Map<String, Object> BLOCKSTATES;

    private BlockStateUtils() {}

    public static void init() throws IOException {
        BLOCKSTATES = YamlUtils.loadResource("/blockstates.yml");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, List<String>> getPossibleStates(String block) {
        if (!block.startsWith(ResourcePackUtils.DEFAULT_NAMESPACE + ":")) {
            block = ResourcePackUtils.DEFAULT_NAMESPACE + ":" + block;
        }
        return ((Map<String, List<Object>>) BLOCKSTATES.get(block)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream().map(Object::toString).collect(Collectors.toList())));
    }
}
