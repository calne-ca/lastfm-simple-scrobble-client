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

import de.umass.lastfm.Caller;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import net.beardbot.lastfm.scrobbleclient.exception.LastfmAuthenticationException;
import net.beardbot.lastfm.scrobbleclient.exception.LastfmInsufficientAuthenticationDataException;
import net.beardbot.lastfm.scrobbleclient.exception.UnmanagedScrobbleException;
import net.beardbot.lastfm.unscrobble.Unscrobbler;
import net.beardbot.lastfm.unscrobble.exception.UnscrobblerAuthenticationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScrobbleClientTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private LastfmConfiguration config;
    @Mock
    private LastfmAPI lastfmAPI;
    @Mock
    private Unscrobbler unscrobbler;
    @Mock
    private LastfmApiCallLimiter lastfmApiCallLimiter;
    @Mock
    private Session session;
    @Mock
    private Caller caller;

    private ScrobbleClient scrobbleClient;
    private LastfmAuthenticationDetails sufficientAuthDetails;
    private ScrobbleManager scrobbleManager;

    @Before
    public void setUp() {
        config = new LastfmConfiguration();
        config.setIncludePlayingTracksInScrobbles(true);
        config.setDefaultResultsPerPage(50);
        config.setMaxResultsPerPage(1000);

        scrobbleManager = new ScrobbleManager();
        sufficientAuthDetails = TestUtils.createSufficientAuthDetails();

        when(lastfmAPI.getSession(any(),any(),any(),any())).thenReturn(session);
        when(lastfmAPI.getCaller()).thenReturn(caller);
        when(unscrobbler.unscrobble(anyString(),anyString(),anyInt())).thenReturn(true);

        scrobbleClient = new ScrobbleClient(config,lastfmAPI,unscrobbler,scrobbleManager,lastfmApiCallLimiter);
    }

    @Test
    public void login_doesNotTriggerApiLogin_whenNecessaryAuthenticationDetailsAreMissing() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithUsernameAndPassword();
        scrobbleClient.login(authDetails);

        verify(lastfmAPI,times(0)).getSession(any(),any(),any(),any());
    }
    @Test
    public void login_doesNotTriggerUnscrobblerLogin_whenNecessaryAuthenticationDetailsAreMissing() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        scrobbleClient.login(authDetails);

        verify(unscrobbler,times(0)).login(any(),any());
    }
    @Test
    public void login_triggersUnscrobblerLogin_whenNecessaryAuthenticationDetailsAreSet() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithUsernameAndPassword();
        scrobbleClient.login(authDetails);

        verify(unscrobbler,times(1)).login(authDetails.getUsername(),authDetails.getPassword());
    }
    @Test
    public void login_doesNotThrowException_whenAuthenticationDetailsAreMissing() throws Exception {
        scrobbleClient.login(TestUtils.createEmptyAuthDetails());
    }
    @Test
    public void login_throwLastfmAuthenticationException_whenLastfmApiLoginFails() throws Exception {
        expectedException.expect(LastfmAuthenticationException.class);

        when(lastfmAPI.getSession(any(),any(),any(),any())).thenReturn(null);
        scrobbleClient.login(sufficientAuthDetails);
    }
    @Test
    public void login_throwLastfmAuthenticationException_whenUnscrobblerLoginFails() throws Exception {
        expectedException.expect(LastfmAuthenticationException.class);

        doThrow(UnscrobblerAuthenticationException.class).when(unscrobbler).login(any(),any());
        scrobbleClient.login(sufficientAuthDetails);
    }

    @Test
    public void unscrobble_throwsIllegalArgumentException_whenScrobbleIsMissingTimestamp() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        scrobbleClient.login(TestUtils.createAuthDetailsWithApiKeyAndUsername());
        scrobbleClient.unscrobble(TestUtils.createScrobbleWithoutTimestamp());
    }
    @Test
    public void unscrobble_throwsLastfmInsufficientAuthenticationDataException_whenNecessaryAuthenticationDetailsAreMissing() throws Exception {
        expectedException.expect(LastfmInsufficientAuthenticationDataException.class);

        scrobbleClient.login(TestUtils.createAuthDetailsWithApiKeyAndUsername());
        scrobbleClient.unscrobble(TestUtils.createScrobbleWithTimestamp());
    }
    @Test
    public void unscrobble_removesScrobbleFromScrobbleManager() throws Exception {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        Scrobble persistedScrobble = scrobbleManager.persist(scrobble);

        assertThat(scrobbleManager.getOriginalScrobble(persistedScrobble),not(is(nullValue())));

        scrobbleClient.login(TestUtils.createAuthDetailsWithUsernameAndPassword());
        scrobbleClient.unscrobble(persistedScrobble);

        assertThat(scrobbleManager.getOriginalScrobble(persistedScrobble),is(nullValue()));
    }
    @Test
    public void unscrobble_triggersUnscrobbler() throws Exception {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();

        scrobbleClient.login(TestUtils.createAuthDetailsWithUsernameAndPassword());
        scrobbleClient.unscrobble(scrobble);

        verify(unscrobbler,times(1)).unscrobble(scrobble.getArtist(),scrobble.getTrackName(),scrobble.getTimestampSeconds());
    }
    @Test
    public void unscrobble_returnsFalseWhenUnscrobblingFails() throws Exception {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        when(unscrobbler.unscrobble(scrobble.getArtist(),scrobble.getTrackName(),scrobble.getTimestampSeconds())).thenReturn(false);

        scrobbleClient.login(TestUtils.createAuthDetailsWithUsernameAndPassword());
        boolean success = scrobbleClient.unscrobble(scrobble);

        assertThat(success,is(false));
    }

    @Test
    public void scrobble_throwsIllegalArgumentException_whenArtistIsMissing() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Scrobble scrobble = TestUtils.createScrobbleWithoutTimestamp();
        scrobble.setArtist(null);

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.unscrobble(scrobble);
    }
    @Test
    public void scrobble_throwsIllegalArgumentException_whenTracknameIsMissing() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Scrobble scrobble = TestUtils.createScrobbleWithoutTimestamp();
        scrobble.setTrackName(null);

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.unscrobble(scrobble);
    }
    @Test
    public void scrobble_throwsLastfmInsufficientAuthenticationDataException_whenNecessaryAuthenticationDetailsAreMissing() throws Exception {
        expectedException.expect(LastfmInsufficientAuthenticationDataException.class);

        scrobbleClient.login(TestUtils.createAuthDetailsWithApiKeyAndUsername());
        scrobbleClient.unscrobble(TestUtils.createScrobbleWithTimestamp());
    }
    @Test
    public void scrobble_addsScrobbleToScrobbleManager() throws Exception {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        Scrobble persistedScrobble = scrobbleManager.persist(scrobble);

        assertThat(scrobbleManager.size(),is(1));

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.scrobble(persistedScrobble);

        assertThat(scrobbleManager.size(),is(2));
    }
    @Test
    public void scrobble_scrobbleWithoutTimestamp_generatesTimestamp() throws Exception {
        Scrobble scrobble = TestUtils.createScrobbleWithoutTimestamp();
        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.scrobble(scrobble);
        assertThat(scrobble.getTimestamp().getSecond(), anyOf(
                is(Utils.currentTimestamp().getSecond()),
                is(Utils.currentTimestamp().plusSeconds(1).getSecond())));
    }
    @Test
    public void scrobble_triggersLastfmApi() throws Exception {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        scrobbleClient.login(TestUtils.createSufficientAuthDetails());

        scrobbleClient.scrobble(scrobble);
        verify(lastfmAPI,times(1)).scrobble(scrobble.getArtist(),scrobble.getTrackName(),scrobble.getTimestampSeconds(),session);

        reset(lastfmAPI);
        scrobbleClient.scrobble(scrobble.getArtist(),scrobble.getTrackName());
        verify(lastfmAPI,times(1)).scrobble(eq(scrobble.getArtist()),eq(scrobble.getTrackName()),anyInt(),eq(session));
    }

    @Test
    public void getAllScrobbles_throwsLastfmInsufficientAuthenticationDataException_whenNecessaryAuthenticationDetailsAreMissing() throws Exception {
        expectedException.expect(LastfmInsufficientAuthenticationDataException.class);

        scrobbleClient.login(TestUtils.createAuthDetailsWithUsernameAndPassword());
        scrobbleClient.getAllScrobbles();
    }
    @Test
    public void getAllScrobbles_returnsAllScrobbles() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result1 = TestUtils.createTrackList(1, 2, config.getMaxResultsPerPage(), false);
        PaginatedResult<Track> result2 = TestUtils.createTrackList(2, 2, config.getMaxResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getMaxResultsPerPage(), authDetails.getApiKey())).thenReturn(result1);
        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 2, config.getMaxResultsPerPage(), authDetails.getApiKey())).thenReturn(result2);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getAllScrobbles();

        assertThat(scrobbles.size(),is(result1.getPageResults().size() + result2.getPageResults().size()));
    }
    @Test
    public void getAllScrobbles_emptyResultReturnsEmptyList() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createEmptyTrackList();

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getMaxResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getAllScrobbles();

        assertThat(scrobbles.size(),is(0));
    }
    @Test
    public void getAllScrobbles_returnsAllScrobbles_withNowPlayingTrack() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, config.getMaxResultsPerPage(), true);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getMaxResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getAllScrobbles();

        assertThat(scrobbles.size(),is(result.getPageResults().size()));
    }
    @Test
    public void getAllScrobbles_excludesNowPlayingTrack_whenExcludeConfigured() throws Exception {
        config.setIncludePlayingTracksInScrobbles(false);
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result1 = TestUtils.createTrackList(1, 1, config.getMaxResultsPerPage(), true);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getMaxResultsPerPage(), authDetails.getApiKey())).thenReturn(result1);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getAllScrobbles();

        assertThat(scrobbles.size(),is(result1.getPageResults().size()-1));
    }
    @Test
    public void getLastScrobbles_loadsNextPage_whenAmountBiggerThanDefaultResultsPerPage() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result1 = TestUtils.createTrackList(1, 2, config.getDefaultResultsPerPage(), false);
        PaginatedResult<Track> result2 = TestUtils.createTrackList(2, 2, config.getDefaultResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result1);
        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 2, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result2);

        scrobbleClient.login(authDetails);
        scrobbleClient.getLastScrobbles(config.getDefaultResultsPerPage()+1);

        verify(lastfmAPI,times(1)).getRecentTracks(authDetails.getUsername(),1,config.getDefaultResultsPerPage(),authDetails.getApiKey());
        verify(lastfmAPI,times(1)).getRecentTracks(authDetails.getUsername(),2,config.getDefaultResultsPerPage(),authDetails.getApiKey());
    }
    @Test
    public void getLastScrobbles_onlyLoadsFirstPage_whenAmountNotBiggerThanDefaultResultsPerPage() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result1 = TestUtils.createTrackList(1, 2, config.getDefaultResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result1);

        scrobbleClient.login(authDetails);
        scrobbleClient.getLastScrobbles(config.getDefaultResultsPerPage());

        verify(lastfmAPI,times(1)).getRecentTracks(authDetails.getUsername(),1,config.getDefaultResultsPerPage(),authDetails.getApiKey());
        verify(lastfmAPI,times(0)).getRecentTracks(authDetails.getUsername(),2,config.getDefaultResultsPerPage(),authDetails.getApiKey());
    }
    @Test
    public void getLastScrobbles_returnsEmptyList_whenAmountIsZero() throws Exception {
        scrobbleClient.login(TestUtils.createAuthDetailsWithApiKeyAndUsername());
        List<Scrobble> scrobbles = scrobbleClient.getLastScrobbles(0);

        assertThat(scrobbles.size(),is(0));
    }
    @Test
    public void getLastScrobbles_loadsCorrectAmountOverOnePage() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result1 = TestUtils.createTrackList(1, 2, config.getDefaultResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, 32, authDetails.getApiKey())).thenReturn(result1);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getLastScrobbles(32);

        assertThat(scrobbles.size(),is(32));
    }
    @Test
    public void getLastScrobbles_loadsCorrectAmountOverTwoPages() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result1 = TestUtils.createTrackList(1, 2, config.getDefaultResultsPerPage(), false);
        PaginatedResult<Track> result2 = TestUtils.createTrackList(2, 2, config.getDefaultResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result1);
        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 2, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result2);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getLastScrobbles(64);

        assertThat(scrobbles.size(),is(64));
    }
    @Test
    public void getLastScrobbles_loadsCorrectAmount_withNowPlayingTrack() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, config.getDefaultResultsPerPage(), true);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getLastScrobbles(config.getDefaultResultsPerPage());

        assertThat(scrobbles.size(),is(result.getPageResults().size()));
    }
    @Test
    public void getLastScrobbles_excludesNowPlayingTrack_whenExcludeConfigured() throws Exception {
        config.setIncludePlayingTracksInScrobbles(false);
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, config.getDefaultResultsPerPage(), true);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getLastScrobbles(config.getDefaultResultsPerPage());

        assertThat(scrobbles.size(),is(result.getPageResults().size()-1));
    }
    @Test
    public void getLastScrobbles_loadsCorrectAmount_whenAmountBiggerThanResults() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result1 = TestUtils.createTrackList(1, 2, 50, false);
        PaginatedResult<Track> result2 = TestUtils.createTrackList(2, 2, 13, false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result1);
        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 2, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result2);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getLastScrobbles(5000);

        assertThat(scrobbles.size(),is(result1.getPageResults().size() + result2.getPageResults().size()));
    }
    @Test
    public void getScrobblesSince_throwsLastfmInsufficientAuthenticationDataException_whenNecessaryAuthenticationDetailsAreMissing() throws Exception {
        expectedException.expect(LastfmInsufficientAuthenticationDataException.class);

        scrobbleClient.login(TestUtils.createAuthDetailsWithUsernameAndPassword());
        scrobbleClient.getScrobblesSince(LocalDateTime.now());
    }
    @Test
    public void getScrobblesSince_dateInFuture_returnsEmptyList() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 2, config.getDefaultResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getScrobblesSince(LocalDateTime.now().plusDays(1), config.getDefaultResultsPerPage());

        assertThat(scrobbles.size(),is(0));
    }
    @Test
    public void getScrobblesSince_dateInFuture_doesNotInvokeLastfmApi() throws Exception {
        scrobbleClient.login(TestUtils.createAuthDetailsWithApiKeyAndUsername());
        scrobbleClient.getScrobblesSince(LocalDateTime.now().plusSeconds(15), config.getDefaultResultsPerPage());

        verify(lastfmAPI,times(0)).getRecentTracks(any(),anyInt(),anyInt(),any());
    }
    @Test
    public void getScrobblesSince_dateInPast_returnsCompleteList() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, config.getDefaultResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getScrobblesSince(LocalDateTime.now().minusYears(30), config.getDefaultResultsPerPage());

        assertThat(scrobbles.size(),is(result.getPageResults().size()));
    }
    @Test
    public void getScrobblesSince_fiveScrobblesSinceDate_returnsListWithFiveScrobbles() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, config.getDefaultResultsPerPage(), false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getScrobblesSince(LocalDateTime.now().minusHours(6).minusSeconds(1), config.getDefaultResultsPerPage());

        assertThat(scrobbles.size(),is(5));
    }
    @Test
    public void getScrobblesSince_resultLimit_triggersLastfmApiWithResultLimit() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, 39, false);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, 39, authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getScrobblesSince(LocalDateTime.now().minusYears(30), 39);

        assertThat(scrobbles.size(),is(39));
    }
    @Test
    public void getScrobblesSince_correctAmountOfScrobbles_withNowPlayingTrack() throws Exception {
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, config.getDefaultResultsPerPage(), true);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getScrobblesSince(LocalDateTime.now().minusYears(30), config.getDefaultResultsPerPage());

        assertThat(scrobbles.size(),is(result.getPageResults().size()));
    }
    @Test
    public void getScrobblesSince_excludesNowPlayingTrack_whenExcludeConfigured() throws Exception {
        config.setIncludePlayingTracksInScrobbles(false);
        LastfmAuthenticationDetails authDetails = TestUtils.createAuthDetailsWithApiKeyAndUsername();
        PaginatedResult<Track> result = TestUtils.createTrackList(1, 1, config.getDefaultResultsPerPage(), true);

        when(lastfmAPI.getRecentTracks(authDetails.getUsername(), 1, config.getDefaultResultsPerPage(), authDetails.getApiKey())).thenReturn(result);

        scrobbleClient.login(authDetails);
        List<Scrobble> scrobbles = scrobbleClient.getScrobblesSince(LocalDateTime.now().minusYears(30), config.getDefaultResultsPerPage());

        assertThat(scrobbles.size(),is(result.getPageResults().size()-1));
    }

    @Test
    public void updateScrobble_throwsLastfmInsufficientAuthenticationDataException_whenNecessaryAuthenticationDetailsAreMissing() throws Exception {
        try{
            scrobbleClient.login(TestUtils.createAuthDetailsWithUsernameAndPassword());
            scrobbleClient.updateScrobble(TestUtils.createScrobbleWithTimestamp());
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
        try{
            scrobbleClient.login(TestUtils.createAuthDetailsWithApiKeyAndUsername());
            scrobbleClient.updateScrobble(TestUtils.createScrobbleWithTimestamp());
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
    }
    @Test
    public void updateScrobble_throwsIllegalArgumentException_whenArtistIsMissing() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Scrobble scrobble = TestUtils.createScrobbleWithoutTimestamp();
        scrobble.setArtist(null);

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.updateScrobble(scrobble);
    }
    @Test
    public void updateScrobble_throwsIllegalArgumentException_whenTrackanmeIsMissing() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        Scrobble scrobble = TestUtils.createScrobbleWithoutTimestamp();
        scrobble.setTrackName(null);

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.updateScrobble(scrobble);
    }
    @Test
    public void updateScrobble_throwsUnmanagedScrobbleException_whenScrobbleIsNotPersistedByScrobbleManager() throws Exception {
        expectedException.expect(UnmanagedScrobbleException.class);

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.updateScrobble(TestUtils.createScrobbleWithTimestamp());
    }
    @Test
    public void updateScrobble_unscrobblesOldScrobble() throws Exception {
        Scrobble scrobble = TestUtils.createScrobbleWithTimestamp();
        Scrobble updatedScrobble = scrobbleManager.persist(scrobble);

        Scrobble originalScrobble = scrobble.clone();

        updatedScrobble.setArtist("updated-artist");
        updatedScrobble.setTrackName("updated-trackname");
        updatedScrobble.setTimestamp(Utils.currentTimestamp().minusHours(1));

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.updateScrobble(updatedScrobble);

        verify(unscrobbler,times(1)).unscrobble(originalScrobble.getArtist(),originalScrobble.getTrackName(),originalScrobble.getTimestampSeconds());
    }
    @Test
    public void updateScrobble_scrobblesUpdatedScrobble() throws Exception {
        Scrobble originalScrobble = TestUtils.createScrobbleWithTimestamp();
        Scrobble updatedScrobble = scrobbleManager.persist(originalScrobble);

        updatedScrobble.setArtist(RandomStringUtils.randomAlphabetic(16));
        updatedScrobble.setTrackName(RandomStringUtils.randomAlphabetic(16));
        updatedScrobble.setTimestamp(Utils.currentTimestamp().minusHours(1));

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.updateScrobble(updatedScrobble);

        verify(lastfmAPI,times(1)).scrobble(updatedScrobble.getArtist(),updatedScrobble.getTrackName(),originalScrobble.getTimestampSeconds(),session);
    }
    @Test
    public void updateScrobble_updatesOriginalScrobble() throws Exception {
        Scrobble originalScrobble = TestUtils.createScrobbleWithTimestamp();
        Scrobble updatedScrobble = scrobbleManager.persist(originalScrobble);

        updatedScrobble.setArtist(RandomStringUtils.randomAlphabetic(16));
        updatedScrobble.setTrackName(RandomStringUtils.randomAlphabetic(16));
        updatedScrobble.setTimestamp(Utils.currentTimestamp().minusHours(1));

        scrobbleClient.login(TestUtils.createSufficientAuthDetails());
        scrobbleClient.updateScrobble(updatedScrobble);

        assertThat(originalScrobble.getArtist(),is(updatedScrobble.getArtist()));
        assertThat(originalScrobble.getTrackName(),is(updatedScrobble.getTrackName()));
        assertThat(originalScrobble.getTimestamp(),is(not(updatedScrobble.getTimestamp())));
    }
}