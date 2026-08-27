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

    // Sacamos TODO whitespace (espacios y saltos de línea): el caso típico de
    // falla es pegar el base64 partido en líneas de 76 chars (sin -w0), que
    // rompe el decoder con "Illegal base64 character". Limpiándolo, ese error
    // deja de pasar. Si aún así no es base64 válido, damos un mensaje humano.
    String cleaned = firebaseServiceAccountBase64.replaceAll("\\s", "");
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(cleaned);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "FIREBASE_SERVICE_ACCOUNT_BASE64 no es base64 válido. ¿Pegaste el JSON "
          + "crudo en vez del base64, o el valor quedó con caracteres raros? "
          + "Generalo con: base64 -w0 service-account.json", e);
    }
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
