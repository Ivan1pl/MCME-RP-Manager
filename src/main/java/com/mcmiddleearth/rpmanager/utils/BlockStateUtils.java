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

import com.mcmiddleearth.rpmanager.model.Case;
import com.mcmiddleearth.rpmanager.model.When;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockStateUtils {
    private static final Pattern BLOCK_STATE_VARIANT_PATTERN = Pattern.compile("^[^\\[#]+\\[([^]]*)]$");
    private static final Pattern BLOCK_STATE_VARIANT_PATTERN2 = Pattern.compile("^[^#]+#(.*)$");
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

    public static Map<String, String> getVariantValues(String variantString) {
        return variantString == null ?
                new HashMap<>() :
                Stream.of(variantString.split(",")).map(String::trim)
                        .map(s -> s.replaceAll("\\u003d", "=")).filter(s -> s.contains("=")).collect(
                                Collectors.toMap(s -> s.split("=", 2)[0], s -> s.split("=", 2)[1]));
    }

    public static Map<String, String> getVariantValuesBySearchString(String searchString) {
        Matcher matcher = BLOCK_STATE_VARIANT_PATTERN.matcher(searchString);
        Matcher matcher2 = BLOCK_STATE_VARIANT_PATTERN2.matcher(searchString);
        return getVariantValues(matcher.matches() ?
                matcher.group(1) : (matcher2.matches() ? matcher2.group(1) : null));
    }

    public static boolean matches(String variantString, Map<String, String> matchValues) {
        Map<String, String> variantValues = BlockStateUtils.getVariantValues(variantString);
        return allMatch(variantValues, matchValues);
    }

    public static boolean matches(Case theCase, Map<String, String> matchValues) {
        return getCaseValues(theCase.getWhen()).stream()
                .anyMatch(values -> allMatch(values, matchValues));
    }

    private static List<Map<String, String>> getCaseValues(When caseCondition) {
        List<Map<String, Object>> conditions = caseCondition.getValue() == null ?
                caseCondition.getOR() : Collections.singletonList(caseCondition.getValue());
        return conditions == null ?
                new LinkedList<>() :
                conditions.stream().map(m -> m.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, e -> e.getValue() == null ? "" : e.getValue().toString()))).toList();
    }

    private static boolean allMatch(Map<String, String> value, Map<String, String> template) {
        return value.entrySet().stream()
                .allMatch(e -> !template.containsKey(e.getKey()) || e.getValue().equals(template.get(e.getKey())));
    }
}
