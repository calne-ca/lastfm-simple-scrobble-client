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
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class ScrobbleTest {

    @Test
    public void setTimestamp_setsTimestampSecondsAccordingly() {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        scrobble.setTimestamp(Utils.currentTimestamp().minusHours(1));
        Integer seconds = scrobble.getTimestampSeconds();

        assertThat(seconds,is(Utils.dateTimeToEpochSeconds(scrobble.getTimestamp())));

        scrobble = TestUtils.createScrobbleWithoutTimestamp();
        scrobble.setTimestamp(Utils.currentTimestamp().minusHours(1));
        seconds = scrobble.getTimestampSeconds();

        assertThat(seconds,is(Utils.dateTimeToEpochSeconds(scrobble.getTimestamp())));
    }
    @Test
    public void setTimestampSecond_setsTimestampAccordingly() {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        scrobble.setTimestampSeconds(150000);
        ZonedDateTime timestamp = scrobble.getTimestamp();

        assertThat(timestamp,is(Utils.epochSecondsToDateTime(scrobble.getTimestampSeconds())));

        scrobble = TestUtils.createScrobbleWithoutTimestamp();
        scrobble.setTimestampSeconds(150000);
        timestamp = scrobble.getTimestamp();

        assertThat(timestamp,is(Utils.epochSecondsToDateTime(scrobble.getTimestampSeconds())));
    }
    @Test
    public void clone_createsNewObjectWithSameValues() {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        Scrobble clone = scrobble.clone();

        assertThat(scrobble,not(is(clone)));
        assertThat(scrobble.getArtist(),is(clone.getArtist()));
        assertThat(scrobble.getTrackName(),is(clone.getTrackName()));
        assertThat(scrobble.getTimestamp(),is(clone.getTimestamp()));
        assertThat(scrobble.getTimestampSeconds(),is(clone.getTimestampSeconds()));
    }
    @Test
    public void clone_createsNewObjectWithSameValues_withoutTimestamp() {
        Scrobble scrobble = TestUtils.createScrobbleWithoutTimestamp();
        Scrobble clone = scrobble.clone();

        assertThat(scrobble,not(is(clone)));
        assertThat(scrobble.getArtist(),is(clone.getArtist()));
        assertThat(scrobble.getTrackName(),is(clone.getTrackName()));
        assertThat(scrobble.getTimestamp(),is(clone.getTimestamp()));
        assertThat(scrobble.getTimestampSeconds(),is(clone.getTimestampSeconds()));
    }
    @Test
    public void constructor_createsScrobbleCorrectly() {
        Track track = TestUtils.createTrack(new Date(), false);
        Scrobble scrobble = new Scrobble(track);

        assertThat(scrobble.getArtist(),is(track.getArtist()));
        assertThat(scrobble.getTrackName(),is(track.getName()));
        assertThat(scrobble.getTimestampSeconds(),is(Utils.dateToEpochSeconds(track.getPlayedWhen())));
        assertThat(scrobble.getTimestamp(),is(Utils.epochSecondsToDateTime(scrobble.getTimestampSeconds())));
        assertThat(scrobble.isNowPlaying(),is(false));
    }
    @Test
    public void constructor_createsScrobbleCorrectly_withNowPlayingTrack() {
        Track track = TestUtils.createTrack(new Date(), true);
        Scrobble scrobble = new Scrobble(track);

        assertThat(scrobble.getArtist(),is(track.getArtist()));
        assertThat(scrobble.getTrackName(),is(track.getName()));
        assertThat(scrobble.getTimestampSeconds(),is(nullValue()));
        assertThat(scrobble.getTimestamp(),is(nullValue()));
        assertThat(scrobble.isNowPlaying(),is(true));
    }
}