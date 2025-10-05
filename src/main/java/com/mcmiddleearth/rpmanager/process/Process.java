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

package com.mcmiddleearth.rpmanager.process;

import com.mcmiddleearth.rpmanager.events.EventDispatcher;
import com.mcmiddleearth.rpmanager.events.ProcessCompletedEvent;

import java.util.function.Consumer;

public abstract class Process<Result> {
    private final EventDispatcher eventDispatcher = new EventDispatcher();

    @SuppressWarnings("unchecked")
    protected Process(Consumer<Result> onProcessCompleted) {
        eventDispatcher.addEventListener(
                event -> onProcessCompleted.accept((Result) event.getResult()), ProcessCompletedEvent.class);
    }

    protected final void complete(Result result) {
        eventDispatcher.dispatchEvent(new ProcessCompletedEvent<>(this, result));
    }

    public abstract void start() throws Exception;
}
