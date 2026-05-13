package com.jowi.stock.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
@ConditionalOnProperty(
    prefix = "security.firebase",
    name = "enabled",
    havingValue = "true"
)
public class FirebaseConfig {

  @PostConstruct
  public void init() throws Exception {
    InputStream serviceAccount =
        getClass().getResourceAsStream("/firebase-service-account.json");

    if (serviceAccount == null) {
      throw new IllegalStateException("firebase-service-account.json not found in classpath");
    }

    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build();

    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseApp.initializeApp(options);
    }
  }
}