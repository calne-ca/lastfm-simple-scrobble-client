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
    private boolean nowPlaying;
    private Integer timestampSeconds;
    private ZonedDateTime timestamp;

    public Scrobble(Track track){
        this.artist = track.getArtist();
        this.trackName = track.getName();
        this.nowPlaying = track.isNowPlaying();

        if (!track.isNowPlaying()){
            this.timestampSeconds = Utils.dateToEpochSeconds(track.getPlayedWhen());
            this.timestamp = Utils.epochSecondsToDateTime(this.timestampSeconds);
        }
    }

    /**
     * Creates a {@link Scrobble} object from the passed data.
     * @param artist The artist of the track.
     * @param trackName The title of the track.
     * @return A {@link Scrobble} object containing the given data.
     */
    public static Scrobble of(final String artist, final String trackName){
        Scrobble scrobble = new Scrobble();
        scrobble.setArtist(artist);
        scrobble.setTrackName(trackName);
        return scrobble;
    }

    /**
     * Creates a copy of the {@link Scrobble} object.
     * @return A {@link Scrobble} object containing the same data.
     */
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

        if (timestampSeconds == null){
            this.timestamp = null;
        } else {
            this.timestamp = Utils.epochSecondsToDateTime(timestampSeconds);
        }
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;

        if (timestamp == null){
            this.timestampSeconds = null;
        } else {
            this.timestampSeconds = Utils.dateTimeToEpochSeconds(timestamp);
        }
    }

    @Override
    public String toString() {
        return String.format("%s - %s",artist,trackName);
    }
}
