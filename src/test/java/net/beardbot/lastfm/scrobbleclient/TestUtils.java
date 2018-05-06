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

import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class TestUtils {
    static LastfmAuthenticationDetails createSufficientAuthDetails(){
        String apiKey = RandomStringUtils.randomAlphabetic(32);
        String sharedSecret = RandomStringUtils.randomAlphabetic(32);
        String username = RandomStringUtils.randomAlphabetic(32);
        String password = RandomStringUtils.randomAlphabetic(32);
        return new LastfmAuthenticationDetails(apiKey,sharedSecret,username,password);
    }

    static LastfmAuthenticationDetails createAuthDetailsWithUsernameAndPassword(){
        String username = RandomStringUtils.randomAlphabetic(32);
        String password = RandomStringUtils.randomAlphabetic(32);
        return new LastfmAuthenticationDetails(null,null,username,password);
    }

    static LastfmAuthenticationDetails createAuthDetailsWithApiKeyAndUsername(){
        String apiKey = RandomStringUtils.randomAlphabetic(32);
        String username = RandomStringUtils.randomAlphabetic(32);
        return new LastfmAuthenticationDetails(apiKey,null,username,null);
    }

    static LastfmAuthenticationDetails createEmptyAuthDetails(){
        return new LastfmAuthenticationDetails();
    }

    static Scrobble createScrobbleWithoutTimestamp(){
        Scrobble scrobble = new Scrobble();
        scrobble.setArtist(RandomStringUtils.randomAlphabetic(16));
        scrobble.setTrackName(RandomStringUtils.randomAlphabetic(16));
        return scrobble;
    }

    static Scrobble createScrobbleWithTimestamp(){
        Scrobble scrobble = createScrobbleWithoutTimestamp();
        scrobble.setTimestamp(Utils.currentTimestamp());
        return scrobble;
    }

    static PaginatedResult<Track> createTrackList(int pageNumber, int totalPageNumber, int trackAmount, boolean includeOnePlayingTrack){
        PaginatedResult<Track> result = mock(PaginatedResult.class);

        List<Track> tracks = new ArrayList<>();

        for (int i = 0; i < trackAmount; i++) {
            int offset = ( trackAmount * (pageNumber - 1) ) + i;
            Date date = Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusHours(offset).toInstant());
            tracks.add(createTrack(date, (includeOnePlayingTrack && i == 0)));
        }

        when(result.getPageResults()).thenReturn(tracks);
        when(result.getPage()).thenReturn(pageNumber);
        when(result.getTotalPages()).thenReturn(totalPageNumber);
        when(result.isEmpty()).thenReturn(false);

        return result;
    }

    static PaginatedResult<Track> createEmptyTrackList(){
        PaginatedResult<Track> result = mock(PaginatedResult.class);

        when(result.getPageResults()).thenReturn(new ArrayList<>());
        when(result.getPage()).thenReturn(1);
        when(result.getTotalPages()).thenReturn(1);
        when(result.isEmpty()).thenReturn(true);

        return result;
    }

    static Track createTrack(Date date, boolean nowPlaying){
        Track track = mock(Track.class);
        when(track.getArtist()).thenReturn(RandomStringUtils.randomAlphabetic(16));
        when(track.getName()).thenReturn(RandomStringUtils.randomAlphabetic(16));
        when(track.getPlayedWhen()).thenReturn(date);
        when(track.isNowPlaying()).thenReturn(nowPlaying);
        return track;
    }

    static ScrobbleResult createSuccessfulScrobbleResult(){
        return createScrobbleResult(true,false);
    }
    static ScrobbleResult createUnsuccessfulScrobbleResult(){
        return createScrobbleResult(false,false);
    }
    static ScrobbleResult createIgnoredScrobbleResult(){
        return createScrobbleResult(true,true);
    }

    private static ScrobbleResult createScrobbleResult(boolean successful, boolean ignored){
        ScrobbleResult scrobbleResult = mock(ScrobbleResult.class);
        when(scrobbleResult.isSuccessful()).thenReturn(successful);
        when(scrobbleResult.isIgnored()).thenReturn(ignored);
        return scrobbleResult;
    }
}
