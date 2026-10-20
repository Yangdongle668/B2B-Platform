package com.b2bweb.testkit.port;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

public final class FakeVirusScanner implements VirusScanner {
  private final Clock clock;
  private boolean available = true;

  public FakeVirusScanner(Clock clock) {
    this.clock = clock;
  }

  public void setAvailable(boolean value) {
    available = value;
  }

  public boolean available() {
    return available;
  }

  public ScanResult scan(InputStream input, long size, String name) {
    if (!available)
      throw new ExternalServiceException(
          InfraErrors.VIRUS_SCANNER_UNAVAILABLE, "测试扫描器不可用", true, null);
    try {
      long count = 0;
      boolean infected = false;
      String tail = "";
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        count += read;
        String chunk = tail + new String(buffer, 0, read, StandardCharsets.ISO_8859_1);
        infected |= chunk.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE");
        tail = chunk.substring(Math.max(0, chunk.length() - 64));
      }
      if (count != size) throw new IllegalArgumentException("扫描长度不一致");
      return new ScanResult(
          infected ? ScanVerdict.INFECTED : ScanVerdict.CLEAN,
          infected ? "EICAR-Test-File" : null,
          "fake-virus-scanner",
          clock.instant());
    } catch (IOException e) {
      throw new ExternalServiceException(InfraErrors.VIRUS_SCANNER_UNAVAILABLE, "测试扫描失败", true, e);
    }
  }
}
