package com.mojang.authlib.legacy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.launcher.LauncherConstants;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.launcher.LauncherConstants.URL_LEGACY_AUTHENTICATION;

public class LegacyUserAuthentication extends HttpUserAuthentication {
    private static final URL AUTHENTICATION_URL = LauncherConstants.constantURL(URL_LEGACY_AUTHENTICATION);
    private static final int AUTHENTICATION_VERSION = 14;
    private static final int RESPONSE_PART_PROFILE_NAME = 2;
    private static final int RESPONSE_PART_SESSION_TOKEN = 3;
    private static final int RESPONSE_PART_PROFILE_ID = 4;

    private String sessionToken;

    LegacyUserAuthentication(final LegacyAuthenticationService authenticationService) {
        super(authenticationService);
    }

    @Override
    public void logIn() throws AuthenticationException {
        if (StringUtils.isBlank(this.getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if (StringUtils.isBlank(this.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }
        final Map<String, Object> args = new HashMap<>();
        args.put("user", this.getUsername());
        args.put("password", this.getPassword());
        args.put("version", AUTHENTICATION_VERSION);
        String response;
        try {
            response = this.getAuthenticationService().performPostRequest(LegacyUserAuthentication.AUTHENTICATION_URL, HttpAuthenticationService.buildQuery(args), "application/x-www-form-urlencoded").trim();
        } catch (IOException e) {
            throw new AuthenticationException("Authentication server is not responding", e);
        }
        final String[] split = response.split(":");
        if (split.length != 5) {
            throw new InvalidCredentialsException(response);
        }
        final String profileId = split[RESPONSE_PART_PROFILE_ID];
        final String profileName = split[RESPONSE_PART_PROFILE_NAME];
        final String sessionToken = split[RESPONSE_PART_SESSION_TOKEN];
        if (StringUtils.isBlank(profileId) || StringUtils.isBlank(profileName) || StringUtils.isBlank(sessionToken)) {
            throw new AuthenticationException("Unknown response from authentication server: " + response);
        }
        this.setSelectedProfile(new GameProfile(UUIDTypeAdapter.fromString(profileId), profileName));
        this.sessionToken = sessionToken;
        this.setUserType(UserType.LEGACY);
    }

    @Override
    public void logOut() {
        super.logOut();
        this.sessionToken = null;
    }

    @Override
    public boolean canPlayOnline() {
        return this.isLoggedIn() && this.getSelectedProfile() != null && this.getAuthenticatedToken() != null;
    }

    @Override
    public GameProfile[] getAvailableProfiles() {
        if (this.getSelectedProfile() != null) {
            return new GameProfile[]{this.getSelectedProfile()};
        }
        return new GameProfile[0];
    }

    @Override
    public void selectGameProfile(final GameProfile profile) throws AuthenticationException {
        throw new UnsupportedOperationException("Game profiles cannot be changed in the legacy authentication service");
    }

    @Override
    public String getAuthenticatedToken() {
        return this.sessionToken;
    }

    @Override
    public String getUserID() {
        return this.getUsername();
    }

    @Override
    public LegacyAuthenticationService getAuthenticationService() {
        return (LegacyAuthenticationService) super.getAuthenticationService();
    }
}
