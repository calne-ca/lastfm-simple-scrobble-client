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

import net.beardbot.lastfm.scrobbleclient.exception.LastfmInsufficientAuthenticationDataException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class LastfmAuthenticationDetailsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void hasAllData_isTrue_whenAllDataIsSet() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        assertThat(authenticationDetails.hasAllData(),is(true));
    }
    @Test
    public void hasAllData_isFalse_whenNecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setApiKey(null);
        assertThat(authenticationDetails.hasAllData(),is(false));

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setSharedSecret(null);
        assertThat(authenticationDetails.hasAllData(),is(false));

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setUsername(null);
        assertThat(authenticationDetails.hasAllData(),is(false));

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setPassword(null);
        assertThat(authenticationDetails.hasAllData(),is(false));
    }

    @Test
    public void hasDataForPublicUserData_isTrue_whenAllDataIsSet() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        assertThat(authenticationDetails.hasDataForPublicUserData(),is(true));
    }
    @Test
    public void hasDataForPublicUserData_isFalse_whenNecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setApiKey(null);
        assertThat(authenticationDetails.hasDataForPublicUserData(),is(false));

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setUsername(null);
        assertThat(authenticationDetails.hasDataForPublicUserData(),is(false));
    }
    @Test
    public void hasDataForPublicUserData_isTrue_whenUnnecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setSharedSecret(null);
        assertThat(authenticationDetails.hasDataForPublicUserData(),is(true));

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setPassword(null);
        assertThat(authenticationDetails.hasDataForPublicUserData(),is(true));
    }

    @Test
    public void hasDataForDirectLogin_isTrue_whenAllDataIsSet() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        assertThat(authenticationDetails.hasDataForDirectLogin(),is(true));
    }
    @Test
    public void hasDataForDirectLogin_isFalse_whenNecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setUsername(null);
        assertThat(authenticationDetails.hasDataForDirectLogin(),is(false));

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setPassword(null);
        assertThat(authenticationDetails.hasDataForDirectLogin(),is(false));
    }
    @Test
    public void hasDataForDirectLogin_isTrue_whenUnnecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setApiKey(null);
        assertThat(authenticationDetails.hasDataForDirectLogin(),is(true));

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setSharedSecret(null);
        assertThat(authenticationDetails.hasDataForDirectLogin(),is(true));
    }

    @Test
    public void assureAllPermissions_doesNotThrowException_whenAllDataIsSet() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.assureAllPermissions();
    }
    @Test
    public void assureAllPermissions_throwsException_whenNecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        try{
            authenticationDetails.setApiKey(null);
            authenticationDetails.assureAllPermissions();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
        try{
            authenticationDetails = TestUtils.createSufficientAuthDetails();
            authenticationDetails.setSharedSecret(null);
            authenticationDetails.assureAllPermissions();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
        try{
            authenticationDetails = TestUtils.createSufficientAuthDetails();
            authenticationDetails.setUsername(null);
            authenticationDetails.assureAllPermissions();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
        try{
            authenticationDetails = TestUtils.createSufficientAuthDetails();
            authenticationDetails.setPassword(null);
            authenticationDetails.assureAllPermissions();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
    }

    @Test
    public void assurePermissionForPublicUserData_doesNotThrowException_whenAllDataIsSet() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.assurePermissionForPublicUserData();
    }
    @Test
    public void assurePermissionForPublicUserData_throwsException_whenNecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        try{
            authenticationDetails.setApiKey(null);
            authenticationDetails.assurePermissionForPublicUserData();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
        try{
            authenticationDetails = TestUtils.createSufficientAuthDetails();
            authenticationDetails.setUsername(null);
            authenticationDetails.assurePermissionForPublicUserData();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
    }
    @Test
    public void assurePermissionForPublicUserData_throwsException_whenUnnecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setSharedSecret(null);
        authenticationDetails.assurePermissionForPublicUserData();

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setPassword(null);
        authenticationDetails.assurePermissionForPublicUserData();
    }

    @Test
    public void assurePermissionForDirectLogin_doesNotThrowException_whenAllDataIsSet() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.assurePermissionForDirectLogin();
    }
    @Test
    public void assurePermissionForDirectLogin_throwsException_whenNecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        try{
            authenticationDetails.setUsername(null);
            authenticationDetails.assurePermissionForDirectLogin();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
        try{
            authenticationDetails = TestUtils.createSufficientAuthDetails();
            authenticationDetails.setPassword(null);
            authenticationDetails.assurePermissionForDirectLogin();
            fail();
        } catch (LastfmInsufficientAuthenticationDataException ignored){}
    }
    @Test
    public void assurePermissionForDirectLogin_throwsException_whenUnnecessaryDataIsMissing() {
        LastfmAuthenticationDetails authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setApiKey(null);
        authenticationDetails.assurePermissionForDirectLogin();

        authenticationDetails = TestUtils.createSufficientAuthDetails();
        authenticationDetails.setSharedSecret(null);
        authenticationDetails.assurePermissionForDirectLogin();
    }
}