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

import com.mcmiddleearth.rpmanager.process.Process;

public class ProcessCompletedEvent<Result> implements Event {
    private final Process<Result> source;
    private final Result result;

    public ProcessCompletedEvent(Process<Result> source, Result result) {
        this.source = source;
        this.result = result;
    }

    @Override
    public Object getSource() {
        return source;
    }

    public Result getResult() {
        return result;
    }
}
