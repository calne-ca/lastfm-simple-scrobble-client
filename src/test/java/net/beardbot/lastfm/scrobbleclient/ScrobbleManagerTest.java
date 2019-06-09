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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScrobbleManagerTest {

    private ScrobbleManager scrobbleManager;
    private Scrobble originalScrobble;


    @Before
    public void setUp() {
        scrobbleManager = new ScrobbleManager();
        originalScrobble = TestUtils.createScrobbleWithTimestamp();
    }

    @Test
    public void updateOriginalScrobble_doesNotOverwriteTimestamp() {
        Integer originalTimestampSeconds = originalScrobble.getTimestampSeconds();

        Scrobble persistedScrobble = scrobbleManager.persist(originalScrobble);
        persistedScrobble.setTimestamp(Utils.currentTimestamp().minusHours(5));
        scrobbleManager.updateOriginalScrobble(persistedScrobble);

        assertThat(originalScrobble.getTimestampSeconds(),is(originalTimestampSeconds));
    }
    @Test
    public void updateOriginalScrobble_overwritesArtist() {
        Scrobble persistedScrobble = scrobbleManager.persist(originalScrobble);
        persistedScrobble.setArtist(RandomStringUtils.randomAlphabetic(16));
        scrobbleManager.updateOriginalScrobble(persistedScrobble);

        assertThat(originalScrobble.getArtist(),is(persistedScrobble.getArtist()));
    }
    @Test
    public void updateOriginalScrobble_overwritesTrackname() {
        Scrobble persistedScrobble = scrobbleManager.persist(originalScrobble);
        persistedScrobble.setTrackName(RandomStringUtils.randomAlphabetic(16));
        scrobbleManager.updateOriginalScrobble(persistedScrobble);

        assertThat(originalScrobble.getTrackName(),is(persistedScrobble.getTrackName()));
    }
    @Test
    public void remove_removesPersistedScrobble() {
        Scrobble persistedScrobble = scrobbleManager.persist(originalScrobble);
        assertThat(scrobbleManager.size(),is(1));
        scrobbleManager.remove(persistedScrobble);
        assertThat(scrobbleManager.size(),is(0));
    }
    @Test
    public void remove_doesNotRemoveNonPersistedScrobble() {
        scrobbleManager.persist(originalScrobble);
        assertThat(scrobbleManager.size(),is(1));
        scrobbleManager.remove(originalScrobble);
        assertThat(scrobbleManager.size(),is(1));
    }
    @Test
    public void persist_persistsScrobblesWithSameDataTwice() {
        Scrobble clone = originalScrobble.clone();
        scrobbleManager.persist(originalScrobble);
        assertThat(scrobbleManager.size(),is(1));
        scrobbleManager.persist(clone);
        assertThat(scrobbleManager.size(),is(2));
    }
    @Test
    public void getOriginalScrobble_returnsCorrectOriginalScrobbleObject() {
        Scrobble persistedScrobble = scrobbleManager.persist(originalScrobble);
        Scrobble scrobble = scrobbleManager.getOriginalScrobble(persistedScrobble);
        assertThat(scrobble,is(originalScrobble));
    }
}