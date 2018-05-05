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

import de.umass.lastfm.Track;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Scrobble {

    private String artist;
    private String trackName;
    private Integer timestampSeconds;
    private ZonedDateTime timestamp;

    public Scrobble(Track track){
        this.artist = track.getArtist();
        this.trackName = track.getName();
        this.timestampSeconds = Utils.dateToEpochSeconds(track.getPlayedWhen());
        this.timestamp = Utils.epochSecondsToDateTime(this.timestampSeconds);
    }

    public static Scrobble of(final String artist, final String trackName){
        Scrobble scrobble = new Scrobble();
        scrobble.setArtist(artist);
        scrobble.setTrackName(trackName);
        return scrobble;
    }

    public Scrobble clone(){
        Scrobble clonedScrobble = new Scrobble();
        clonedScrobble.setArtist(this.artist);
        clonedScrobble.setTrackName(this.trackName);
        clonedScrobble.setTimestampSeconds(this.timestampSeconds);
        clonedScrobble.setTimestamp(this.timestamp);
        return clonedScrobble;
    }

    public void setTimestampSeconds(Integer timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
        this.timestamp = Utils.epochSecondsToDateTime(timestampSeconds);
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
        this.timestampSeconds = Utils.dateTimeToEpochSeconds(timestamp);
    }

    @Override
    public String toString() {
        return String.format("%s - %s",artist,trackName);
    }
}
