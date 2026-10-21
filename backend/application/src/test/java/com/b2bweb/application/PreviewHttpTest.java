package com.b2bweb.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.b2bweb.api.content.*;
import com.b2bweb.api.translation.TranslationMode;
import com.b2bweb.api.common.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class PreviewHttpTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean ContentApi content;
  final JsonMapper json = JsonMapper.builder().build();

  @BeforeEach void seed() {
    reset(content);
    jdbc.update("INSERT INTO sys_site(id,code,name,theme_key,timezone,status) VALUES(759,'preview-http','预览测试','industrial','UTC','ACTIVE') ON DUPLICATE KEY UPDATE status='ACTIVE'");
    jdbc.update("INSERT INTO sys_site_domain(id,site_id,domain,type) VALUES(7590,759,'preview.example.test','PRIMARY') ON DUPLICATE KEY UPDATE domain=VALUES(domain)");
    var localization = mock(LocalizationInfo.class);
    when(localization.translationMode()).thenReturn(TranslationMode.MANUAL);
    when(content.findLocalization(13L)).thenReturn(Optional.of(localization));
  }
  private void token(PreviewPurpose purpose) {
    when(content.resolvePreviewToken("valid")).thenReturn(Optional.of(new PreviewTarget(759,12,13,"zh-CN",new PreviewMode.Draft(),Instant.now().plusSeconds(60),purpose,117L)));
    when(content.assemblePreview(any())).thenReturn(new PreviewSnapshot(12,13,"PAGE","zh-CN",json.createObjectNode(),List.of()));
  }
  @Test void editorRequiresServerResolvedPurposeAndAllowsNoClientIdentity() throws Exception {
    for (var purpose : List.of(PreviewPurpose.PREVIEW, PreviewPurpose.SHARE)) {
      token(purpose);
      mvc.perform(post("/api/public/v1/delivery/preview").header("Host","preview.example.test").contentType("application/json").content("{\"token\":\"valid\",\"partOverrides\":{}}"))
          .andExpect(status().isNotFound()).andExpect(header().string("Cache-Control","no-store"));
    }
    verify(content, never()).assemblePreview(any());
    token(PreviewPurpose.EDITOR);
    mvc.perform(post("/api/public/v1/delivery/preview").header("Host","preview.example.test").contentType("application/json").content("{\"token\":\"valid\",\"purpose\":\"EDITOR\",\"partOverrides\":{}}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/public/v1/delivery/preview").header("Host","preview.example.test").contentType("application/json").content("{\"token\":\"valid\",\"partOverrides\":{}}"))
        .andExpect(status().isOk()).andExpect(header().string("CDN-Cache-Control","no-store")).andExpect(header().string("X-Robots-Tag","noindex, nofollow"));
    var capture = org.mockito.ArgumentCaptor.forClass(PreviewRequest.class); verify(content).assemblePreview(capture.capture());
    assertThat(capture.getValue().purpose()).isEqualTo(PreviewPurpose.EDITOR);
  }
  @Test void previewAllowsReadOnlySharingButNeverMetadataOverrides() throws Exception {
    token(PreviewPurpose.SHARE);
    mvc.perform(get("/api/public/v1/delivery/preview").header("Host","preview.example.test").param("token","valid")).andExpect(status().isOk());
    mvc.perform(post("/api/public/v1/delivery/preview").header("Host","preview.example.test").contentType("application/json").content("{\"token\":\"valid\",\"partOverrides\":{\"card\":{}}}"))
        .andExpect(status().isNotFound());
    token(PreviewPurpose.EDITOR);
    mvc.perform(post("/api/public/v1/delivery/preview").header("Host","preview.example.test").contentType("application/json").content("{\"token\":\"valid\",\"partOverrides\":{\"card\":{}}}"))
        .andExpect(status().isUnprocessableEntity());
  }
  @Test void dependencyFailureIs503AndRevocationOrCrossSiteIs404() throws Exception {
    when(content.resolvePreviewToken("down")).thenThrow(new IllegalStateException("private infrastructure detail"));
    mvc.perform(get("/api/public/v1/delivery/preview").header("Host","preview.example.test").param("token","down"))
        .andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After","120")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private infrastructure detail"))));
    when(content.resolvePreviewToken("valid")).thenReturn(Optional.empty());
    mvc.perform(get("/api/public/v1/delivery/preview").header("Host","preview.example.test").param("token","valid")).andExpect(status().isNotFound());
    when(content.resolvePreviewToken("other")).thenReturn(Optional.of(new PreviewTarget(760,12,13,"zh-CN",new PreviewMode.Draft(),Instant.now().plusSeconds(60),PreviewPurpose.SHARE,null)));
    mvc.perform(get("/api/public/v1/delivery/preview").header("Host","preview.example.test").param("token","other")).andExpect(status().isNotFound());
  }
  @Test void oversizedAndMalformedBodiesAreRejectedBeforeAssembly() throws Exception {
    mvc.perform(post("/api/public/v1/delivery/preview").header("Host","preview.example.test").contentType("application/json").content("x".repeat(1_048_577))).andExpect(status().isPayloadTooLarge());
    mvc.perform(post("/api/public/v1/delivery/preview").header("Host","preview.example.test").contentType("application/json").content("{")) .andExpect(status().isBadRequest());
    verify(content, never()).assemblePreview(any());
  }
}
