package com.b2bweb.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.b2bweb.api.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Import(ProblemsIntegrationTest.Fixtures.class)
class ProblemsIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @TestConfiguration
  static class Fixtures {
    @Bean
    Endpoints problemEndpoints() {
      return new Endpoints();
    }
  }

  @io.swagger.v3.oas.annotations.Hidden
  @RestController
  static class Endpoints {
    @GetMapping("/api/public/v1/system/problem-fixture")
    Object error(@RequestParam String kind) {
      switch (kind) {
        case "conflict":
          throw new VersionConflictException(CoreErrors.SETTING_VERSION_CONFLICT, "设置已被修改", 7);
        case "validation":
          throw new ValidationException(
              CoreErrors.SETTING_INVALID,
              "设置内容不符合约束",
              List.of(new FieldError("value", "FORMAT", "字段格式不合法")));
        case "argument":
          throw new IllegalArgumentException("internal-sensitive-detail");
        default:
          throw new IllegalStateException("internal-sensitive-detail");
      }
    }

    record Input(@NotBlank String title) {}

    @PostMapping("/api/public/v1/system/problem-fixture")
    Input validate(@Valid @RequestBody Input value) {
      return value;
    }
  }

  @BeforeEach
  void seed() {
    jdbc.update(
        "INSERT INTO sys_site(id,code,name,theme_key,timezone,status)"
            + " VALUES(771,'problems','错误测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE"
            + " status='ACTIVE'");
    jdbc.update(
        "INSERT INTO sys_site_domain(id,site_id,domain,type)"
            + " VALUES(7710,771,'problems.example.test','PRIMARY') ON DUPLICATE KEY UPDATE"
            + " domain=VALUES(domain)");
  }

  @Test
  void optimisticConflictKeepsCurrentVersionAndChineseDetail() throws Exception {
    mvc.perform(
            get("/api/public/v1/system/problem-fixture")
                .header("Host", "problems.example.test")
                .param("kind", "conflict"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.currentVersion").value(7))
        .andExpect(jsonPath("$.detail").value("设置已被修改"))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void structuredValidationKeepsFieldNamesAndChineseMessages() throws Exception {
    mvc.perform(
            get("/api/public/v1/system/problem-fixture")
                .header("Host", "problems.example.test")
                .param("kind", "validation"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].field").value("value"))
        .andExpect(jsonPath("$.errors[0].message").value("字段格式不合法"));
  }

  @Test
  void beanValidationAndMalformedJsonNeverExposeEnglishOrRawInput() throws Exception {
    mvc.perform(
            post("/api/public/v1/system/problem-fixture")
                .header("Host", "problems.example.test")
                .contentType("application/json")
                .content("{\"title\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].message").value("字段不能为空"));
    mvc.perform(
            post("/api/public/v1/system/problem-fixture")
                .header("Host", "problems.example.test")
                .contentType("application/json")
                .content("{internal-sensitive-detail"))
        .andExpect(status().isBadRequest())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal-sensitive-detail"))));
  }

  @Test
  void unexpectedErrorsAndArgumentFailuresReturnSafeChineseProblems() throws Exception {
    for (String kind : List.of("argument", "unexpected"))
      mvc.perform(
              get("/api/public/v1/system/problem-fixture")
                  .header("Host", "problems.example.test")
                  .param("kind", kind))
          .andExpect(status().is(kind.equals("argument") ? 400 : 500))
          .andExpect(jsonPath("$.detail").isString())
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString("internal-sensitive-detail"))))
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString("java.lang"))));
  }
}
