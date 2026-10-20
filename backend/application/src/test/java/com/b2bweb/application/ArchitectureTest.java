package com.b2bweb.application;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static org.assertj.core.api.Assertions.*;

import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.*;
import com.tngtech.archunit.lang.*;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  static final String[] MODULES = {
    "article",
    "content",
    "delivery",
    "form",
    "graph",
    "inquiry",
    "media",
    "navigation",
    "notification",
    "page",
    "product",
    "search",
    "seo",
    "taxonomy",
    "tracking",
    "translation",
    "url"
  };

  static ArchRule coreSupportOnly() {
    return noClasses()
        .that()
        .resideInAnyPackage(
            java.util.Arrays.stream(MODULES)
                .map(id -> "com.b2bweb." + id + "..")
                .toArray(String[]::new))
        .should()
        .dependOnClassesThat(
            new com.tngtech.archunit.base.DescribedPredicate<JavaClass>(
                "are Core implementation outside support") {
              public boolean test(JavaClass type) {
                return type.getPackageName().startsWith("com.b2bweb.core.")
                    && !type.getPackageName().startsWith("com.b2bweb.core.support");
              }
            });
  }

  static ArchRule sameModuleOnly(String module) {
    return noClasses()
        .that()
        .resideInAPackage("com.b2bweb." + module + "..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            java.util.Arrays.stream(MODULES)
                .filter(id -> !id.equals(module))
                .map(id -> "com.b2bweb." + id + "..")
                .toArray(String[]::new));
  }

  @Test
  void productionClassesRespectBusinessAndCoreBoundaries() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.b2bweb");
    coreSupportOnly().check(classes);
    for (String module : MODULES) sameModuleOnly(module).check(classes);
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "com.baomidou..", "org.mybatis..")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  void compiledInvalidModuleFailsBothActualArchitectureRules() {
    var invalid =
        new ClassFileImporter()
            .importClasses(com.b2bweb.inquiry.fixtures.IllegalBoundaryExample.class);
    assertThatThrownBy(() -> coreSupportOnly().check(invalid))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SiteContexts");
    assertThatThrownBy(() -> sameModuleOnly("inquiry").check(invalid))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("ProductModule");
  }
}
