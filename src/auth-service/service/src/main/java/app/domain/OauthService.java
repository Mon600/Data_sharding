package app.domain;

import app.domain.model.GoogleDataDto;
import app.domain.model.GoogleOauthDto;
import app.domain.model.UserDto;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Getter
@Setter
public class OauthService {

    private final String googleClientId;
    private final String googleClientSecret;

    private final NetHttpTransport transport;
    private final JsonFactory jsonFactory;
    private final GoogleIdTokenVerifier verifier;


    public OauthService(
            @Value("${CLIENT_ID}") String googleClientId,
            @Value("${CLIENT_SECRET}") String googleClientSecret) throws Exception {

        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;


        this.transport = GoogleNetHttpTransport.newTrustedTransport();
        this.jsonFactory = GsonFactory.getDefaultInstance();

        this.verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    UserService userService;


    public GoogleDataDto getDataFromGoogle(GoogleOauthDto googleOauthDto) throws IOException, GeneralSecurityException {
        GoogleTokenResponse googleTokenResponse = new GoogleAuthorizationCodeTokenRequest(
                transport,
                jsonFactory,
                googleClientId,
                googleClientSecret,
                googleOauthDto.code(),
                googleOauthDto.redirectUri()
        ).execute();

        String idTokenString = googleTokenResponse.getIdToken();
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new SecurityException("Invalid token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        if (!emailVerified) {
            throw new SecurityException("Email not verified");
        }
        String googleId = payload.getSubject();
        String name = (String) payload.get("given_name");
        String surname = (String) payload.get("family_name");
        return new GoogleDataDto(name, surname, email, googleId);
    }
}
