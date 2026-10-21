package com.b2bweb.infrastructure.virus;

import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.http.RemoteCalls;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

/** clamd INSTREAM. Never owns or closes the caller's stream. */
public final class ClamAvVirusScanner implements VirusScanner {
  private final InetSocketAddress address;
  private final int timeout;
  private final long maximum;
  private final Clock clock;

  public ClamAvVirusScanner(String host, int port, Duration timeout, long maximum, Clock clock) {
    this.address = new InetSocketAddress(host, port);
    this.timeout = Math.toIntExact(timeout.toMillis());
    if (this.timeout < 1 || maximum < 1) throw new IllegalArgumentException("扫描配置无效");
    this.maximum = maximum;
    this.clock = clock;
  }

  private Socket connect() throws IOException {
    var socket = new Socket();
    try {
      socket.connect(address, timeout);
      socket.setSoTimeout(timeout);
      return socket;
    } catch (IOException e) {
      socket.close();
      throw e;
    }
  }

  public boolean available() {
    RemoteCalls.outsideTransaction();
    try (var socket = connect()) {
      socket.getOutputStream().write("zPING\0".getBytes(StandardCharsets.US_ASCII));
      return read(socket).equals("PONG");
    } catch (IOException e) {
      return false;
    }
  }

  public ScanResult scan(InputStream content, long size, String fileName) {
    RemoteCalls.outsideTransaction();
    if (size < 0) throw new IllegalArgumentException("文件大小不能为负");
    if (size > maximum) return result(ScanVerdict.UNSCANNABLE, null, "unknown");
    try (var socket = connect();
        var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()) {
      var deadline =
          scheduler.schedule(
              () -> {
                try {
                  socket.close();
                } catch (IOException ignored) {
                }
              },
              timeout,
              java.util.concurrent.TimeUnit.MILLISECONDS);
      try {
        var out = new DataOutputStream(socket.getOutputStream());
        out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
        byte[] buffer = new byte[8192];
        long count = 0;
        for (int n; (n = content.read(buffer)) != -1; ) {
          count += n;
          if (count > maximum) return result(ScanVerdict.UNSCANNABLE, null, "unknown");
          out.writeInt(n);
          out.write(buffer, 0, n);
        }
        out.writeInt(0);
        out.flush();
        String response = read(socket);
        String version = version();
        if (response.endsWith(": OK")) return result(ScanVerdict.CLEAN, null, version);
        if (response.endsWith(" FOUND"))
          return result(
              ScanVerdict.INFECTED,
              response.substring(response.indexOf(": ") + 2, response.length() - 6),
              version);
        if (response.contains("size limit")
            || response.contains("Heuristics.Limits")
            || response.contains("Encrypted"))
          return result(ScanVerdict.UNSCANNABLE, null, version);
        throw new IOException();
      } finally {
        deadline.cancel(false);
        scheduler.shutdownNow();
      }
    } catch (IOException e) {
      throw new ExternalServiceException(
          InfraErrors.VIRUS_SCANNER_UNAVAILABLE, "病毒扫描服务暂时不可用", true, null);
    }
  }

  private String version() throws IOException {
    try (var socket = connect()) {
      socket.getOutputStream().write("zVERSION\0".getBytes(StandardCharsets.US_ASCII));
      return read(socket);
    }
  }

  private static String read(Socket socket) throws IOException {
    var out = new ByteArrayOutputStream();
    for (int value;
        (value = socket.getInputStream().read()) != -1 && value != 0 && value != '\n'; ) {
      if (out.size() >= 4096) throw new IOException();
      out.write(value);
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  private ScanResult result(ScanVerdict verdict, String signature, String version) {
    return new ScanResult(verdict, signature, version, clock.instant());
  }
}
