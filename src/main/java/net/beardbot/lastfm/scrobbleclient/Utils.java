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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Date;

class Utils {
    static ZonedDateTime epochSecondsToDateTime(int epochSeconds){
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds),ZoneOffset.UTC);
    }

    static int dateTimeToEpochSeconds(ZonedDateTime dateTime){
        return (int) (dateTime.toInstant().toEpochMilli()/1000);
    }

    static int dateToEpochSeconds(Date date){
        return (int) (date.getTime() / 1000);
    }

    static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }

    static ZonedDateTime currentTimestamp(){
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    static boolean isInFuture(Temporal temporal){
        if (temporal == null){
            return false;
        }

        Duration duration = Duration.between(temporal,currentTimestamp());
        return duration.isNegative();
    }
}
