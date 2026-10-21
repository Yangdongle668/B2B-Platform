package com.b2bweb.infrastructure;

import com.b2bweb.api.port.*;
import com.b2bweb.infrastructure.virus.ClamAvVirusScanner;
import com.b2bweb.testkit.contract.port.VirusScannerContract;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class ClamAvContractTest extends VirusScannerContract {
  ServerSocket server;
  ExecutorService workers;
  VirusScanner scanner;
  volatile boolean running;
  volatile boolean online;

  @BeforeEach
  void start() throws Exception {
    server = new ServerSocket(0, 20, InetAddress.getLoopbackAddress());
    running = true;
    online = true;
    workers = Executors.newVirtualThreadPerTaskExecutor();
    workers.submit(
        () -> {
          while (running)
            try {
              var socket = server.accept();
              workers.submit(() -> respond(socket));
            } catch (IOException e) {
              if (running) throw new UncheckedIOException(e);
            }
        });
    scanner =
        new ClamAvVirusScanner(
            "127.0.0.1",
            server.getLocalPort(),
            Duration.ofSeconds(2),
            1_000_000,
            Clock.systemUTC());
  }

  void respond(Socket socket) {
    try (socket) {
      if(!online)return;
      var in = new DataInputStream(socket.getInputStream());
      var command = new ByteArrayOutputStream();
      for (int c; (c = in.read()) > 0; ) command.write(c);
      String text = command.toString(StandardCharsets.US_ASCII), response;
      if (text.equals("zPING")) response = "PONG";
      else if (text.equals("zVERSION")) response = "ClamAV fixture/1";
      else {
        var body = new ByteArrayOutputStream();
        for (int n; (n = in.readInt()) != 0; ) {
          if (n < 0 || n > 8192) throw new IOException();
          body.write(in.readNBytes(n));
        }
        response =
            body.toString(StandardCharsets.UTF_8).contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE")
                ? "stream: Eicar-Test-Signature FOUND"
                : "stream: OK";
      }
      socket.getOutputStream().write((response + "\0").getBytes(StandardCharsets.UTF_8));
    } catch (IOException ignored) {
    }
  }

  @AfterEach
  void stop() throws Exception {
    running = false;
    server.close();
    workers.shutdownNow();
    workers.close();
  }

  protected VirusScanner api() {
    return scanner;
  }

  protected void available(boolean value) {
    online=value;
  }
}
