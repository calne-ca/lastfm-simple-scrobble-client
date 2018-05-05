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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

public class LastfmApiCallLimiterTest {
    private LastfmApiCallLimiter callLimiter;
    private LastfmConfiguration config;

    private long graceTime = 5L;

    @Before
    public void setUp(){
        config = new LastfmConfiguration();
        config.setMillisForCallLimit(100);
        callLimiter = new LastfmApiCallLimiter(config);
    }

    @Test
    public void doesNotBlockAfterFiveCalls() {
        long start = System.currentTimeMillis();

        for (int i = 0; i < config.getApiCallLimitPerSecond(); i++) {
            callLimiter.considerCallLimit();
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(duration,is(lessThan(config.getMillisForCallLimit())));
    }

    @Test
    public void blocksAfterSixCalls() {
        long start = System.currentTimeMillis();

        for (int i = 0; i < config.getApiCallLimitPerSecond() + 1; i++) {
            callLimiter.considerCallLimit();
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(duration,is(greaterThanOrEqualTo(config.getMillisForCallLimit() - graceTime)));
    }

    @Test
    public void blocksTwiceAfter11Calls() {
        long start = System.currentTimeMillis();

        for (int i = 0; i < config.getApiCallLimitPerSecond() * 2 + 1; i++) {
            callLimiter.considerCallLimit();
        }

        long duration = System.currentTimeMillis() - start;

        assertThat(duration,is(greaterThanOrEqualTo(config.getMillisForCallLimit() * 2 - graceTime)));
    }
}