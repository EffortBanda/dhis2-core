package org.hisp.dhis.security.oidc;

/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service( "dhisOidcUserService" )
public class DhisOidcUserService
    extends OidcUserService
{

    @Autowired
    public UserService userService;

    @Override
    public OidcUser loadUser( OidcUserRequest userRequest )
        throws OAuth2AuthenticationException
    {
        ClientRegistration clientRegistration = userRequest.getClientRegistration();
        ClientRegistration.ProviderDetails providerDetails = clientRegistration.getProviderDetails();
        Map<String, Object> configurationMetadata = providerDetails.getConfigurationMetadata();

        OidcUser oidcUser = super.loadUser( userRequest );

        log.warn( "Got OidcUser:" + oidcUser.toString() );

        OAuth2AccessToken accessToken = userRequest.getAccessToken();
        log.warn( "Got accessToken:" + accessToken );

        OidcUserInfo userInfo = oidcUser.getUserInfo();
        log.warn( "Got userInfo:" + userInfo );

        OidcIdToken idToken = oidcUser.getIdToken();
        log.warn( "Got idToken:" + userInfo );

        Map<String, Object> attributes = oidcUser.getAttributes();

        log.warn( "oidcUser.getAttributes(): {}", attributes );

        attributes.forEach( ( key, value )
            -> log.warn( String.format( "oidcUser.getAttributes() Key: %s Value: %s", key, value ) )
        );

        log.warn( "clientRegistration.getRegistrationId():" + clientRegistration.getRegistrationId() );

        String oidcMappingId = null;
        if ( clientRegistration.getRegistrationId().equals( "google" ) )
        {
            oidcMappingId = (String) attributes.get( "email" );
        }

        if ( clientRegistration.getRegistrationId().equals( "idporten" ) )
        {
            oidcMappingId = (String) attributes.get( "pid" );
        }

        log.warn( "Trying to look up DHIS2 user with mapping id, oidcMappingId:" + oidcMappingId );

        UserCredentials userCredentials = userService.getUserCredentialsByOpenId( oidcMappingId );
        if ( userCredentials != null )
        {
            return new DhisOidcUser( userCredentials, attributes, IdTokenClaimNames.SUB, oidcUser.getIdToken() );
        }
        else
        {
            OAuth2Error oauth2Error = new OAuth2Error(
                "could_not_map_dhis2_user",
                "Failed to map incoming OIDC sub to DHIS user.",
                null );
            throw new OAuth2AuthenticationException( oauth2Error, oauth2Error.toString() );
        }
    }
}