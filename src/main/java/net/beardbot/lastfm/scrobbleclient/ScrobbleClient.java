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

import lombok.extern.slf4j.Slf4j;
import net.beardbot.lastfm.scrobbleclient.exception.*;
import net.beardbot.lastfm.unscrobble.Unscrobbler;
import net.beardbot.lastfm.unscrobble.exception.UnscrobblerAuthenticationException;
import org.apache.commons.lang3.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ScrobbleClient {

    private LastfmAuthenticationDetails authDetails;

    private LastfmAPI lastfmAPI;

    private Session session;
    private Unscrobbler unscrobbler;
    private ScrobbleManager scrobbleManager;
    private LastfmApiCallLimiter callLimiter;

    private LastfmConfiguration config;

    public ScrobbleClient(){
        this(new LastfmConfiguration());
    }

    public ScrobbleClient(final LastfmConfiguration config){
        this.config = config;
        lastfmAPI = new DefaultLastfmAPI();
        scrobbleManager = new ScrobbleManager();
        callLimiter = new LastfmApiCallLimiter(config);
        this.unscrobbler = new Unscrobbler();
    }

    ScrobbleClient(final LastfmConfiguration config, final LastfmAPI lastfmAPI, final Unscrobbler unscrobbler,
                   final ScrobbleManager scrobbleManager, final LastfmApiCallLimiter callLimiter){
        this.config = config;
        this.lastfmAPI = lastfmAPI;
        this.unscrobbler = unscrobbler;
        this.scrobbleManager = scrobbleManager;
        this.callLimiter = callLimiter;
    }

    public void setUserAgent(String userAgent){
        lastfmAPI.getCaller().setUserAgent(userAgent);
    }

    public void login(final LastfmAuthenticationDetails authenticationDetails) throws LastfmAuthenticationException {
        this.authDetails = authenticationDetails;

        if (authenticationDetails.hasAllData()){
            loginWithLastfmApi(authenticationDetails);
        }
        if (authenticationDetails.hasDataForDirectLogin()){
            loginWithUnscrobbler(authenticationDetails);
        }
    }

    private void loginWithLastfmApi(final LastfmAuthenticationDetails authenticationDetails) throws LastfmAuthenticationException {
        this.session = createSession(authenticationDetails);
        if (this.session == null){
            throw new LastfmAuthenticationException(String.format("Failed to login to Last.FM API account: username=%s, password=******, apiKey=%s, sharedSecret=******",
                    authenticationDetails.getUsername(),
                    authenticationDetails.getApiKey()));
        }
    }

    private void loginWithUnscrobbler(final LastfmAuthenticationDetails authenticationDetails) throws LastfmAuthenticationException {
        try {
            this.unscrobbler.login(authenticationDetails.getUsername(),authenticationDetails.getPassword());
        } catch (UnscrobblerAuthenticationException e) {
            throw new LastfmAuthenticationException(String.format("Failed to login to Last.FM account: username=%s, password=******",
                    authenticationDetails.getUsername()),e);
        }
    }

    private Session createSession(final LastfmAuthenticationDetails authenticationDetails) {
        return lastfmAPI.getSession(
                authenticationDetails.getUsername(),
                authenticationDetails.getPassword(),
                authenticationDetails.getApiKey(),
                authenticationDetails.getSharedSecret());
    }

    public void updateScrobble(Scrobble scrobble){
        validateScrobble(scrobble,true);
        authDetails.assureAllPermissions();
        callLimiter.considerCallLimit();

        Scrobble originalScrobble = scrobbleManager.getOriginalScrobble(scrobble);

        if (originalScrobble == null){
            throw new UnmanagedScrobbleException(String.format("The given scrobble %s is not managed by a scrobble manager.",scrobble));
        }

        log.info("Scrobbling {}",scrobble);
        lastfmAPI.scrobble(scrobble.getArtist(),scrobble.getTrackName(),originalScrobble.getTimestampSeconds(),session);

        log.info("Unscrobbling {}",originalScrobble);
        unscrobbler.unscrobble(originalScrobble.getArtist(),originalScrobble.getTrackName(),originalScrobble.getTimestampSeconds());

        scrobbleManager.updateOriginalScrobble(scrobble);
    }

    public List<Scrobble> getAllScrobbles(){
        return getScrobbles(null, config.getMaxResultsPerPage(), Integer.MAX_VALUE);
    }

    public List<Scrobble> getScrobblesSince(Temporal since){
        return getScrobblesSince(since, config.getDefaultResultsPerPage());
    }

    public List<Scrobble> getScrobblesSince(Temporal since, int resultsPerPage){
        return getScrobbles(since ,resultsPerPage, Integer.MAX_VALUE);
    }

    public List<Scrobble> getScrobblesSince(Temporal since, int resultsPerPage, int pageLimit){
        return getScrobbles(since ,resultsPerPage, pageLimit);
    }

    private List<Scrobble> getScrobbles(Temporal since, int resultsPerPage, int pageLimit) {
        authDetails.assurePermissionForPublicUserData();

        ArrayList<Scrobble> scrobbles = new ArrayList<>();

        boolean finished = false;
        int currentPage = 1;

        while (!finished){
            callLimiter.considerCallLimit();
            PaginatedResult<Track> recentTracks = lastfmAPI.getRecentTracks(authDetails.getUsername(), currentPage, resultsPerPage, authDetails.getApiKey());

            log.debug("Fetched scrobble page {}/{}",currentPage,recentTracks.getTotalPages());

            for (Track track : recentTracks.getPageResults()) {
                Scrobble scrobble = createScrobble(track);

                log.debug("Fetched scrobble {}", scrobble);

                if (since != null){
                    Duration duration = Duration.between(since, scrobble.getTimestamp());

                    log.debug("Checking scrobble's timestamp. Since: {} Timestamp: {}.",since, scrobble.getTimestamp());

                    if (duration.isNegative()){
                        log.debug("Finished scrobble fetching due to reaching the value defined in 'since' parameter.");
                        finished = true;
                        break;
                    }
                }

                scrobbles.add(scrobble);
            }

            if (currentPage >= pageLimit){
                log.debug("Finished scrobble fetching due to reaching the value defined in 'pageLimit'.");
                finished = true;
            }
            if (currentPage >= recentTracks.getTotalPages()){
                log.debug("Finished scrobble fetching due to end of results.");
                finished = true;
            }

            currentPage++;
        }

        return scrobbles;
    }

    public Scrobble scrobble(Scrobble scrobble){
        validateScrobble(scrobble,false);
        authDetails.assureAllPermissions();
        callLimiter.considerCallLimit();

        if (scrobble.getTimestampSeconds() == null){
            scrobble.setTimestampSeconds(currentSeconds());
        }

        log.info("Scrobbling {}",scrobble);
        lastfmAPI.scrobble(scrobble.getArtist(),scrobble.getTrackName(),scrobble.getTimestampSeconds(),session);

        return scrobbleManager.persist(scrobble);
    }

    public Scrobble scrobble(String artist, String trackName){
        return scrobble(Scrobble.of(artist,trackName));
    }

    public boolean unscrobble(Scrobble scrobble){
        validateScrobble(scrobble,true);
        authDetails.assurePermissionForDirectLogin();
        callLimiter.considerCallLimit();

        log.info("Unscrobbling {}",scrobble);

        boolean success = unscrobbler.unscrobble(scrobble.getArtist(),scrobble.getTrackName(),scrobble.getTimestampSeconds());

        if (success){
            scrobbleManager.remove(scrobble);
        }

        return success;
    }

    private void validateScrobble(Scrobble scrobble, boolean expectTimestamp){
        if (StringUtils.isBlank(scrobble.getArtist()) ||
                StringUtils.isBlank(scrobble.getTrackName()) ||
                (expectTimestamp && scrobble.getTimestampSeconds() == null)){
            throw new IllegalArgumentException(String.format("Invalid scrobble %s",scrobble.toString()));
        }
    }

    private int currentSeconds(){
        return (int) (Clock.systemUTC().millis() / 1000);
    }

    private Scrobble createScrobble(Track track){
        Scrobble scrobble = new Scrobble(track);
        return scrobbleManager.persist(scrobble);
    }
}
