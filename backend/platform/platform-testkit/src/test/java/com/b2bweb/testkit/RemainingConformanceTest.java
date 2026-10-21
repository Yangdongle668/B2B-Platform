package com.b2bweb.testkit;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.testkit.conformance.*;
import com.b2bweb.testkit.example.SampleExtensions;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Nested;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

class RemainingConformanceTest {
  final TestPlatform p = TestPlatform.standard().useSite(1);
  final SampleExtensions sample = new SampleExtensions(p);
  final JsonMapper json = JsonMapper.builder().build();

  @Nested
  class Types extends ContentTypeProviderConformance {
    protected ContentTypeProvider provider() {
      return sample.new ContentTypes();
    }

    protected RouteHintsContext context() {
      return new RouteHintsContext(1, 11, "SAMPLE", "zh-CN");
    }
  }

  @Nested
  class Publishing extends PublishContributorConformance {
    protected PublishContributor provider() {
      return sample.new Publishing();
    }

    protected void seed() {
      sample.seed(
          1,
          11,
          "zh-CN",
          json.createObjectNode()
              .put("title", "标题")
              .set("image", json.createObjectNode().put("$media", "42")));
    }

    protected PublishContext context() {
      return new PublishContext() {
        public long siteId() {
          return 1;
        }

        public long contentId() {
          return 1;
        }

        public long localizationId() {
          return 11;
        }

        public String contentType() {
          return "SAMPLE";
        }

        public String locale() {
          return "zh-CN";
        }

        public TranslationMode translationMode() {
          return TranslationMode.MANUAL;
        }

        public long sourceLocalizationId() {
          return 11;
        }

        public AssemblyPurpose purpose() {
          return AssemblyPurpose.PUBLISH;
        }

        public int revisionNo() {
          return 1;
        }

        public boolean firstPublish() {
          return true;
        }

        public ActorRef actor() {
          return ActorRef.SYSTEM;
        }

        public Instant now() {
          return p.clock().instant();
        }

        public Optional<JsonNode> publishedPart(String key) {
          return Optional.empty();
        }

        public Optional<JsonNode> overridePart(String key) {
          return Optional.empty();
        }
      };
    }
  }

  @Nested
  class Media extends MediaReferenceCollectorConformance {
    protected MediaReferenceCollector provider() {
      return sample.new MediaReferences();
    }

    protected MediaReferenceContext context() {
      return new MediaReferenceContext(1, 1, "SAMPLE", 11, "zh-CN");
    }

    protected void seed() {
      sample.seed(
          1,
          11,
          "zh-CN",
          json.createObjectNode().set("image", json.createObjectNode().put("$media", "42")));
    }

    protected Set<Long> expectedIds() {
      return Set.of(42L);
    }
  }

  @Nested
  class Personal extends PersonalDataProviderConformance {
    protected PersonalDataProvider provider() {
      return sample.new PersonalData();
    }

    protected PersonalDataRequest request(long site) {
      return new PersonalDataRequest(1, site, "contract@example.test", ActorRef.SYSTEM);
    }

    protected void seedTwoSites() {
      sample.personal(1, "contract@example.test", json.createObjectNode().put("name", "甲"));
      sample.personal(2, "contract@example.test", json.createObjectNode().put("name", "乙"));
    }

    protected int exportedRecords(JsonNode value) {
      return value.isEmpty() ? 0 : 1;
    }
  }

  @Nested
  class Notifications extends NotificationTemplateProviderConformance {
    protected NotificationTemplateProvider provider() {
      return sample.new Notifications();
    }
  }

  @Nested
  class Sources extends DataSourceProviderConformance {
    protected DataSourceProvider provider() {
      return sample.new DataSource();
    }

    protected DataSourceRequest request(int limit) {
      return new DataSourceRequest(
          1,
          "zh-CN",
          DeliveryMode.LIVE,
          json.createObjectNode().put("limit", limit),
          null,
          null,
          "items",
          limit,
          null);
    }

    protected void seedPublishedAndUnpublished() {
      sample.resource(1, "published", "zh-CN", json.createObjectNode().put("$content", "42"));
      sample.resource(1, "other-language", "en", json.createObjectNode().put("$content", "43"));
      sample.seed(1, 44, "zh-CN", json.createObjectNode().put("title", "draft"));
    }

    protected Set<String> expectedPublishedIds() {
      return Set.of("42");
    }
  }

  @Nested
  class Templates extends TemplateProviderConformance {
    protected TemplateProvider provider() {
      return sample.new Templates();
    }
  }
}
