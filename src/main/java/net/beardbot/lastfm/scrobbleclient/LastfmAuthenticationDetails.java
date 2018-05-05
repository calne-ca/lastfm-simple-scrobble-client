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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.beardbot.lastfm.scrobbleclient.exception.LastfmInsufficientAuthenticationDataException;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LastfmAuthenticationDetails {
    private String apiKey;
    private String sharedSecret;
    private String username;
    private String password;

    boolean hasAllData(){
        try {
            assureAllPermissions();
            return true;
        } catch (LastfmInsufficientAuthenticationDataException e){
            return false;
        }
    }

    boolean hasDataForDirectLogin(){
        try {
            assurePermissionForDirectLogin();
            return true;
        } catch (LastfmInsufficientAuthenticationDataException e){
            return false;
        }
    }

    boolean hasDataForPublicUserData(){
        try {
            assurePermissionForPublicUserData();
            return true;
        } catch (LastfmInsufficientAuthenticationDataException e){
            return false;
        }
    }

    void assureAllPermissions(){
        assurePermissionForDirectLogin();
        assurePermissionForPublicUserData();

        if(StringUtils.isBlank(sharedSecret)){
            throw new LastfmInsufficientAuthenticationDataException("This operation requires the shared secret of the given API key for performing API calls.");
        }
    }

    void assurePermissionForPublicUserData(){
        if(StringUtils.isBlank(username)){
            throw new LastfmInsufficientAuthenticationDataException("This operation requires an username for performing user related API calls.");
        }
        if(StringUtils.isBlank(apiKey)){
            throw new LastfmInsufficientAuthenticationDataException("This operation requires an API kex for performing API calls.");
        }
    }

    void assurePermissionForDirectLogin(){
        if(StringUtils.isBlank(username)){
            throw new LastfmInsufficientAuthenticationDataException("This operation requires an username for directly logging in to Last.fm.");
        }
        if(StringUtils.isBlank(password)){
            throw new LastfmInsufficientAuthenticationDataException("This operation requires a password for directly logging in to Last.fm.");
        }
    }
}
