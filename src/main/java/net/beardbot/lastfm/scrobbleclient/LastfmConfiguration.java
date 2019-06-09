/**
 * Copyright (C) 2019 Joscha Düringer
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
package net.beardbot.lastfm.scrobbleclient;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LastfmConfiguration {

    public static final int DEFAULT_API_CALL_LIMIT_PER_SECOND = 5;
    public static final int DEFAULT_MAX_RESULTS_PER_PAGE = 1000;
    public static final int DEFAULT_RESULTS_PER_PAGE = 50;
    public static final long DEFAULT_MILLIS_FOR_CALL_LIMIT = 1039L;
    public static final boolean DEFAULT_INCLUDE_PLAYING_TRACKS = false;

    private int apiCallLimitPerSecond = DEFAULT_API_CALL_LIMIT_PER_SECOND;
    private int maxResultsPerPage = DEFAULT_MAX_RESULTS_PER_PAGE;
    private int resultsPerPage = DEFAULT_RESULTS_PER_PAGE;
    private long millisForCallLimit = DEFAULT_MILLIS_FOR_CALL_LIMIT;
    private boolean includePlayingTracks = DEFAULT_INCLUDE_PLAYING_TRACKS;
}
