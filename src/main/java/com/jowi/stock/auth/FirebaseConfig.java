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

@Configuration
@ConditionalOnProperty(
    prefix = "security.firebase",
    name = "enabled",
    havingValue = "true"
)
public class FirebaseConfig {

  @Value("${firebase.service-account-json}")
  private String firebaseServiceAccountJson;

  @PostConstruct
  public void init() throws Exception {
    if (firebaseServiceAccountJson == null || firebaseServiceAccountJson.isBlank()) {
      throw new IllegalStateException("Firebase service account JSON is missing");
    }

    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(
            GoogleCredentials.fromStream(
                new ByteArrayInputStream(
                    firebaseServiceAccountJson.getBytes(StandardCharsets.UTF_8)
                )
            )
        )
        .build();

    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseApp.initializeApp(options);
    }
  }
}