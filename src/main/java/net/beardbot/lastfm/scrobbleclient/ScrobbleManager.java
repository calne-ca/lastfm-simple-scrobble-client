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

import java.util.HashMap;
import java.util.Map;

class ScrobbleManager {

    private Map<Scrobble,Scrobble> scrobbles = new HashMap<>();

    Scrobble persist(Scrobble originalScrobble){
        Scrobble persistedScrobble = originalScrobble.clone();
        scrobbles.put(persistedScrobble, originalScrobble);
        return persistedScrobble;
    }

    public void remove(Scrobble persistedScrobble){
        scrobbles.remove(persistedScrobble);
    }

    public int size(){
        return scrobbles.size();
    }

    Scrobble getOriginalScrobble(Scrobble persistedScrobble){
        return scrobbles.get(persistedScrobble);
    }

    Scrobble updateOriginalScrobble(Scrobble persistedScrobble){
        Scrobble originalScrobble = scrobbles.get(persistedScrobble);
        originalScrobble.setArtist(persistedScrobble.getArtist());
        originalScrobble.setTrackName(persistedScrobble.getTrackName());
        return persistedScrobble;
    }
}
