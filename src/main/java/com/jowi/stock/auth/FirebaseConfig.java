package com.jowi.stock.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(
    prefix = "security.firebase",
    name = "enabled",
    havingValue = "true"
)
public class FirebaseConfig {

  @Value("${firebase.service-account-base64}")
  private String firebaseServiceAccountBase64;

  @PostConstruct
  public void init() throws Exception {
    if (firebaseServiceAccountBase64 == null || firebaseServiceAccountBase64.isBlank()) {
      throw new IllegalStateException("Firebase service account base64 is missing");
    }

    byte[] decoded = Base64.getDecoder().decode(firebaseServiceAccountBase64);
    String json = new String(decoded, StandardCharsets.UTF_8);

    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(
            GoogleCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
            )
        )
        .build();

    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseApp.initializeApp(options);
    }
  }
}