package com.grill_bros.backend.service.authenticationservice;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String googleClientId;

    public GoogleIdToken.Payload verify(String idTokenString)
            throws Exception {

        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                )
                        .setAudience(List.of(googleClientId))
                        .build();

        GoogleIdToken googleIdToken =
                verifier.verify(idTokenString);

        if (googleIdToken == null) {
            throw new BadCredentialsException("Invalid Google token");
        }

        return googleIdToken.getPayload();
    }
}
