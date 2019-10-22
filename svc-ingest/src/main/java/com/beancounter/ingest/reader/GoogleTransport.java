package com.beancounter.ingest.reader;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.exception.SystemException;
import com.beancounter.ingest.config.GoogleAuthConfig;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GoogleTransport {

  private GoogleAuthConfig googleAuthConfig;

  @Autowired
  @VisibleForTesting
  void setGoogleAuthConfig(GoogleAuthConfig googleAuthConfig) {
    this.googleAuthConfig = googleAuthConfig;
  }

  @VisibleForTesting
  public NetHttpTransport getHttpTransport() {
    try {
      return GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException | IOException e) {
      throw new SystemException(e.getMessage());
    }
  }

  @VisibleForTesting
  Sheets getSheets(NetHttpTransport httpTransport) {
    Sheets service;
    try {
      service = new Sheets.Builder(httpTransport, JacksonFactory.getDefaultInstance(),
          googleAuthConfig.getCredentials(httpTransport))
          .setApplicationName("BeanCounter")
          .build();
    } catch (IOException e) {
      throw new SystemException(e.getMessage());
    }
    return service;
  }

  @VisibleForTesting
  List<List<Object>> getValues(Sheets service, String sheetId, String range) {
    ValueRange response;
    try {
      response = service.spreadsheets()
          .values()
          .get(sheetId, range)
          .execute();
    } catch (IOException e) {
      throw new SystemException(e.getMessage());
    }
    List<List<Object>> values = response.getValues();
    if (values == null || values.isEmpty()) {
      log.error("No data found.");
      throw new BusinessException(String.format("No data found for %s %s", sheetId, range));
    }

    return values;
  }


}
