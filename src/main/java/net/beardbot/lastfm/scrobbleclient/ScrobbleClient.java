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

    /**
     * Logs the client in to Last.fm.
     * @param authenticationDetails The authentication details which contain API key, shared secret, username and password.
     *                              None of these are mandatory by default.
     *                              Which authentication details are needed depends on the operations you want to execute.
     * @throws LastfmAuthenticationException If Last.fm authentication fails.
     */
    public void login(final LastfmAuthenticationDetails authenticationDetails) throws LastfmAuthenticationException {
        this.authDetails = authenticationDetails;

        if (authenticationDetails.hasAllData()){
            loginWithLastfmApi(authenticationDetails);
        }
        if (authenticationDetails.hasDataForDirectLogin()){
            loginWithUnscrobbler(authenticationDetails);
        }
    }

    /**
     * Scrobbles a track to Last.fm.
     * @param scrobble A {@link Scrobble} object containing track information.
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @throws ScrobbleException If scrobbling failed.
     * @return A persisted scrobble object that can be used for updating scrobble data.
     */
    public Scrobble scrobble(final Scrobble scrobble){
        validateScrobble(scrobble,false);
        authDetails.assureAllPermissions();
        callLimiter.considerCallLimit();

        if (scrobble.getTimestampSeconds() == null){
            scrobble.setTimestampSeconds(currentSeconds());
        }

        log.info("Scrobbling {}",scrobble);
        ScrobbleResult scrobbleResult = lastfmAPI.scrobble(scrobble.getArtist(), scrobble.getTrackName(), scrobble.getTimestampSeconds(), session);

        if (!scrobbleResult.isSuccessful()){
            throw new ScrobbleException(String.format("Scrobbling of Scrobble %s failed.",scrobble),scrobble);
        }

        return scrobbleManager.persist(scrobble);
    }

    /**
     * Scrobbles a track to Last.fm.
     * @param artist The artist of the track.
     * @param trackName The title of the track.
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @throws ScrobbleException If scrobbling failed.
     * @return A persisted scrobble object that can be used for updating scrobble data.
     */
    public Scrobble scrobble(final String artist, final String trackName){
        return scrobble(Scrobble.of(artist,trackName));
    }

    /**
     * Removes a {@link Scrobble} from Last.fm.
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @throws ScrobbleException If unscrobbling failed.
     * @throws UnmanagedScrobbleException If the passed {@link Scrobble} object is not being managed by the scrobble manager.
     *                                    This applies to every {@link Scrobble} object that has not be obtained by the {@link ScrobbleClient}.
     * @param scrobble The {@link Scrobble} that shall be removed.
     */
    public void unscrobble(final Scrobble scrobble){
        validateScrobble(scrobble,true);
        authDetails.assurePermissionForDirectLogin();
        callLimiter.considerCallLimit();

        log.info("Unscrobbling {}",scrobble);

        boolean success = unscrobbler.unscrobble(scrobble.getArtist(),scrobble.getTrackName(),scrobble.getTimestampSeconds());

        if (success){
            scrobbleManager.remove(scrobble);
        } else {
            throw new ScrobbleException(String.format("Unscrobbling of Scrobble %s failed.",scrobble),scrobble);
        }
    }

    /**
     * Updates track data of an existing {@link Scrobble}.
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @throws ScrobbleException If scrobbling or unscrobbling failed.
     * @throws UnmanagedScrobbleException If the passed {@link Scrobble} object is not being managed by the scrobble manager.
     *                                    This applies to every {@link Scrobble} object that has not be obtained by the {@link ScrobbleClient}.
     * @param scrobble A persisted {@link Scrobble} object that shall be updated.
     */
    public void updateScrobble(final Scrobble scrobble){
        validateScrobble(scrobble,true);
        authDetails.assureAllPermissions();
        callLimiter.considerCallLimit();

        Scrobble originalScrobble = scrobbleManager.getOriginalScrobble(scrobble);

        if (originalScrobble == null){
            throw new UnmanagedScrobbleException(String.format("The given scrobble %s is not managed by a scrobble manager.",scrobble));
        }

        log.info("Scrobbling {}",scrobble);
        ScrobbleResult scrobbleResult = lastfmAPI.scrobble(scrobble.getArtist(), scrobble.getTrackName(), originalScrobble.getTimestampSeconds(), session);

        if (!scrobbleResult.isSuccessful()){
            throw new ScrobbleException(String.format("Scrobbling of Scrobble %s failed.",scrobble),scrobble);
        }

        log.info("Unscrobbling {}",originalScrobble);
        boolean unscrobbleSuccess = unscrobbler.unscrobble(originalScrobble.getArtist(), originalScrobble.getTrackName(), originalScrobble.getTimestampSeconds());

        if (!unscrobbleSuccess){
            throw new ScrobbleException(String.format("Unscrobbling of Scrobble %s failed.",originalScrobble),scrobble,true);
        }

        scrobbleManager.updateOriginalScrobble(scrobble);
    }

    /**
     * Fetches all {@link Scrobble}s of the authenticated user from Last.fm.
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @return A {@link List} containing all {@link Scrobble}s.
     */
    public List<Scrobble> getAllScrobbles(){
        return getScrobbles(null, config.getMaxResultsPerPage(), Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Fetches all {@link Scrobble}s of the authenticated user from a specific time until now.
     * @param since A {@link Temporal} representing the time from when the {@link Scrobble}s should be fecthed.
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @return A {@link List} containing all {@link Scrobble}s since the time defined in <b>since</b>.
     */
    public List<Scrobble> getScrobblesSince(final Temporal since){
        return getScrobblesSince(since, config.getResultsPerPage());
    }

    /**
     * Fetches all {@link Scrobble}s of the authenticated user from a specific time until now.
     * @param since A {@link Temporal} representing the time from when the {@link Scrobble}s should be fecthed.
     * @param resultsPerPage The results per page that shall be fetched from Last.fm.
     *                       This may for example be set to a higher value if the time defined in <b>since</b> is way in the past.
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @return A {@link List} containing all {@link Scrobble}s since the time defined in <b>since</b>.
     */
    public List<Scrobble> getScrobblesSince(final Temporal since, final int resultsPerPage){
        return getScrobbles(since ,resultsPerPage, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Fetches a certain amount of {@link Scrobble}s.
     * @param amount The amount of {@link Scrobble}s that shall be fecthed
     * @throws LastfmInsufficientAuthenticationDataException If the provided authentication details are insufficient for this operation.
     * @return A {@link List} containing the last <b>amount</b> {@link Scrobble}s.
     */
    public List<Scrobble> getLastScrobbles(int amount){
        int defaultResultsPerPage = config.getResultsPerPage();
        int resultsPerPage = amount > defaultResultsPerPage ? defaultResultsPerPage : amount;
        int pageLimit = amount > defaultResultsPerPage ? (amount / resultsPerPage) + (amount % resultsPerPage == 0 ? 0 : 1) : 1;
        return getScrobbles(null,resultsPerPage,pageLimit,amount);
    }

    /**
     * Sets the user agent header that is being used for every Last.fm HTTP invocation.
     * @param userAgent The user agent that shall be used.
     */
    public void setUserAgent(final String userAgent){
        lastfmAPI.getCaller().setUserAgent(userAgent);
        unscrobbler.setUserAgent(userAgent);
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

    private List<Scrobble> getScrobbles(Temporal since, int resultsPerPage, int pageLimit, int totalLimit) {
        authDetails.assurePermissionForPublicUserData();

        ArrayList<Scrobble> scrobbles = new ArrayList<>();

        if (resultsPerPage < 1 || pageLimit < 1 || totalLimit < 1 || Utils.isInFuture(since)){
            return scrobbles;
        }

        boolean finished = false;
        int currentPage = 1;

        while (!finished){
            callLimiter.considerCallLimit();
            PaginatedResult<Track> recentTracks = lastfmAPI.getRecentTracks(authDetails.getUsername(), currentPage, resultsPerPage, authDetails.getApiKey());

            log.debug("Fetched scrobble page {}/{}",currentPage,recentTracks.getTotalPages());

            for (Track track : recentTracks.getPageResults()) {
                if (track.isNowPlaying() && !config.isIncludePlayingTracks()){
                    continue;
                }

                Scrobble scrobble = createScrobble(track);

                log.debug("Fetched scrobble {}", scrobble);

                if (since != null && scrobble.getTimestamp() != null){
                    Duration duration = Duration.between(since, scrobble.getTimestamp());

                    log.debug("Checking scrobble's timestamp. Since: {} Timestamp: {}.",since, scrobble.getTimestamp());

                    if (duration.isNegative()){
                        log.debug("Finished scrobble fetching due to reaching the value defined in 'since' parameter.");
                        finished = true;
                        break;
                    }
                }

                scrobbles.add(scrobble);

                if (scrobbles.size() == totalLimit){
                    log.debug("Finished scrobble fetching due to reaching the value defined in 'totalLimit' parameter.");
                    finished = true;
                    break;
                }
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
