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

package com.mcmiddleearth.rpmanager.events;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventDispatcher {
    private final Map<Class<?>, List<EventListener<?>>> listeners = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends Event> void dispatchEvent(T event) {
        for (Map.Entry<Class<?>, List<EventListener<?>>> entry : listeners.entrySet()) {
            if (entry.getKey().isAssignableFrom(event.getClass())) {
                for (EventListener<?> listener : entry.getValue()) {
                    ((EventListener<T>) listener).onEvent(event);
                }
            }
        }
    }

    public <T extends Event> void addEventListener(EventListener<T> listener, Class<? extends T> eventClass) {
        listeners.computeIfAbsent(eventClass, ignored -> new LinkedList<>()).add(listener);
    }
}
