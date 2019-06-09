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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
class LastfmApiCallLimiter {
    private final LastfmConfiguration config;

    private static long millisOfFirstCall = 0;
    private static int callsInLastSecond = 0;

    synchronized void considerCallLimit() {
        if (callsInLastSecond == 0){
            millisOfFirstCall = System.currentTimeMillis();
        }
        if (callsInLastSecond == config.getApiCallLimitPerSecond()){
            long passedMillis = System.currentTimeMillis() - millisOfFirstCall;
            long waitInterval = config.getMillisForCallLimit() - passedMillis;

            if (waitInterval > 0){
                log.debug("Reached maximum call limit. Waiting {} ms before next call.",waitInterval);
                Utils.sleep(waitInterval);
            }

            millisOfFirstCall = System.currentTimeMillis();
            callsInLastSecond = 0;
        }
        callsInLastSecond++;
    }


}
