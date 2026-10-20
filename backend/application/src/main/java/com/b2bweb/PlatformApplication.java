package com.b2bweb;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication(
    excludeName =
        "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration")
public class PlatformApplication {
  public static void main(String[] args) {
    for (String arg : args)
      if (arg.startsWith("--mode=")) {
        if (!arg.equals("--mode=migrate")) throw new IllegalArgumentException("不支持的命令行模式");
        migrate();
        return;
      }
    SpringApplication.run(PlatformApplication.class, args);
  }

  private static void migrate() {
    com.b2bweb.core.PlatformCommands.migrate(System.getenv());
  }
}
