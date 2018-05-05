/**
 * Copyright (C) 2018 Joscha Düringer
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

import de.umass.lastfm.*;
import de.umass.lastfm.scrobble.ScrobbleResult;

class DefaultLastfmAPI implements LastfmAPI {
    @Override
    public PaginatedResult<Track> getRecentTracks(String user, int page, int limit, String apiKey) {
        return User.getRecentTracks(user,page,limit,apiKey);
    }

    @Override
    public ScrobbleResult scrobble(String artist, String trackname, int timestamp, Session session) {
        return Track.scrobble(artist,trackname,timestamp,session);
    }

    @Override
    public Session getSession(String username, String password, String apiKey, String secret) {
        return Authenticator.getMobileSession(username,password,apiKey,secret);
    }

    @Override
    public Caller getCaller() {
        return Caller.getInstance();
    }
}
