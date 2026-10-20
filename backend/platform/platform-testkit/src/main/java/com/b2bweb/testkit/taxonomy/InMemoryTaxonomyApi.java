package com.b2bweb.testkit.taxonomy;

import com.b2bweb.api.common.*;
import com.b2bweb.api.content.ContentApi;
import com.b2bweb.api.site.*;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;

public final class InMemoryTaxonomyApi implements TaxonomyApi {
  private final MemoryStore store;
  private final SiteContextProvider sites;
  private final ContentApi content;

  public InMemoryTaxonomyApi(MemoryStore store, SiteContextProvider sites, ContentApi content) {
    this.store = store;
    this.sites = sites;
    this.content = content;
  }

  private String root() {
    return "taxonomy:" + sites.requireSiteId() + ":";
  }

  public void seed(VocabularyInfo vocabulary) {
    store.atomic(
        rows -> {
          rows.put(root() + "vocabulary:" + vocabulary.vocabularyKey(), vocabulary);
          return null;
        });
  }

  public void seed(TermInfo term, Map<String, String> names) {
    if (vocabulary(term.vocabularyKey()).isEmpty())
      throw new NotFoundException(TaxonomyErrors.VOCABULARY_NOT_FOUND, "词汇表不存在");
    if (term.parentId() != null && findTerm(term.parentId()).isEmpty())
      throw new NotFoundException(TaxonomyErrors.TERM_NOT_FOUND, "父分类不存在");
    store.atomic(
        rows -> {
          rows.put(root() + "term:" + term.termId(), term);
          rows.put(root() + "names:" + term.termId(), Map.copyOf(names));
          return null;
        });
  }

  public Optional<VocabularyInfo> vocabulary(String key) {
    return store.atomic(
        rows -> Optional.ofNullable((VocabularyInfo) rows.get(root() + "vocabulary:" + key)));
  }

  public List<VocabularyInfo> vocabularies() {
    return store.atomic(
        rows -> {
          String prefix = root() + "vocabulary:";
          return rows.entrySet().stream()
              .filter(e -> e.getKey().startsWith(prefix))
              .map(e -> (VocabularyInfo) e.getValue())
              .sorted(
                  Comparator.comparingInt(VocabularyInfo::sort)
                      .thenComparing(VocabularyInfo::vocabularyKey))
              .toList();
        });
  }

  public List<VocabularyInfo> vocabulariesFor(String type) {
    return vocabularies().stream().filter(v -> v.applicableTypes().contains(type)).toList();
  }

  public Optional<TermInfo> findTerm(long id) {
    return store.atomic(rows -> Optional.ofNullable((TermInfo) rows.get(root() + "term:" + id)));
  }

  private void batch(Collection<?> values) {
    if (values.size() > 500) throw new IllegalArgumentException("分类批次最多500项");
  }

  public Map<Long, TermInfo> findTerms(Collection<Long> ids) {
    batch(ids);
    Map<Long, TermInfo> result = new LinkedHashMap<>();
    for (long id : ids) findTerm(id).ifPresent(term -> result.put(id, term));
    return Map.copyOf(result);
  }

  @SuppressWarnings("unchecked")
  public Map<Long, TermView> termViews(Collection<Long> ids, String locale) {
    batch(ids);
    return store.atomic(
        rows -> {
          Map<Long, TermView> result = new LinkedHashMap<>();
          for (var term : findTerms(ids).values()) {
            Map<String, String> names =
                (Map<String, String>) rows.get(root() + "names:" + term.termId());
            String name = locale.equals("zh-CN") ? term.sourceName() : names.get(locale);
            result.put(
                term.termId(),
                new TermView(
                    term.termId(),
                    term.vocabularyKey(),
                    term.parentId(),
                    term.depth(),
                    locale,
                    name,
                    term.landingContentId()));
          }
          return Map.copyOf(result);
        });
  }

  public List<TermInfo> ancestors(long id) {
    List<TermInfo> result = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    var term =
        findTerm(id)
            .orElseThrow(() -> new NotFoundException(TaxonomyErrors.TERM_NOT_FOUND, "分类项不存在"));
    Long parent = term.parentId();
    while (parent != null) {
      if (!visited.add(parent)) throw new IllegalStateException("分类树存在环");
      var found =
          findTerm(parent)
              .orElseThrow(() -> new NotFoundException(TaxonomyErrors.TERM_NOT_FOUND, "父分类不存在"));
      result.add(found);
      parent = found.parentId();
    }
    Collections.reverse(result);
    return List.copyOf(result);
  }

  public Map<Long, List<Long>> ancestorIds(Collection<Long> ids) {
    batch(ids);
    Map<Long, List<Long>> result = new LinkedHashMap<>();
    for (long id : ids)
      if (findTerm(id).isPresent())
        result.put(id, ancestors(id).stream().map(TermInfo::termId).toList());
    return Map.copyOf(result);
  }

  public List<TermInfo> children(String vocabulary, Long parent) {
    return store.atomic(
        rows -> {
          String prefix = root() + "term:";
          return rows.entrySet().stream()
              .filter(e -> e.getKey().startsWith(prefix))
              .map(e -> (TermInfo) e.getValue())
              .filter(
                  t -> t.vocabularyKey().equals(vocabulary) && Objects.equals(t.parentId(), parent))
              .sorted(Comparator.comparingInt(TermInfo::sort).thenComparingLong(TermInfo::termId))
              .toList();
        });
  }

  public boolean isSameOrDescendant(long term, long ancestor) {
    return findTerm(term).isPresent()
        && (term == ancestor || ancestors(term).stream().anyMatch(t -> t.termId() == ancestor));
  }

  public Optional<TermInfo> findBySourceNamePath(String vocabulary, List<String> names) {
    Long parent = null;
    TermInfo result = null;
    for (String name : names) {
      var found =
          children(vocabulary, parent).stream()
              .filter(t -> t.sourceName().equals(name))
              .findFirst();
      if (found.isEmpty()) return Optional.empty();
      result = found.get();
      parent = result.termId();
    }
    return Optional.ofNullable(result);
  }

  public Optional<TermInfo> findByLandingContent(long id) {
    return store.atomic(
        rows -> {
          String prefix = root() + "term:";
          return rows.entrySet().stream()
              .filter(e -> e.getKey().startsWith(prefix))
              .map(e -> (TermInfo) e.getValue())
              .filter(t -> Objects.equals(t.landingContentId(), id))
              .findFirst();
        });
  }

  @SuppressWarnings("unchecked")
  public List<TermAssignment> assignments(long content) {
    return store.atomic(
        rows -> {
          String prefix = root() + "assignment:" + content + ":";
          return rows.entrySet().stream()
              .filter(e -> e.getKey().startsWith(prefix))
              .flatMap(e -> ((List<TermAssignment>) e.getValue()).stream())
              .sorted(
                  Comparator.comparing(TermAssignment::vocabularyKey)
                      .thenComparingInt(TermAssignment::sort)
                      .thenComparingLong(TermAssignment::termId))
              .toList();
        });
  }

  public Map<Long, List<TermAssignment>> assignmentsOf(Collection<Long> ids, String vocabulary) {
    batch(ids);
    Map<Long, List<TermAssignment>> result = new LinkedHashMap<>();
    for (long id : ids)
      result.put(
          id,
          assignments(id).stream()
              .filter(a -> vocabulary == null || a.vocabularyKey().equals(vocabulary))
              .toList());
    return Map.copyOf(result);
  }

  public void replaceAssignments(long id, String vocabulary, List<AssignmentInput> values) {
    var info =
        content
            .findContent(id)
            .orElseThrow(
                () ->
                    new NotFoundException(com.b2bweb.api.content.ContentErrors.NOT_FOUND, "内容不存在"));
    var v =
        vocabulary(vocabulary)
            .orElseThrow(
                () -> new NotFoundException(TaxonomyErrors.VOCABULARY_NOT_FOUND, "词汇表不存在"));
    if (!v.applicableTypes().contains(info.contentType()))
      throw new BusinessException(TaxonomyErrors.VOCABULARY_NOT_APPLICABLE, "词汇表不适用于内容类型");
    if (!v.multiple() && values.size() > 1)
      throw new BusinessException(TaxonomyErrors.MULTIPLE_NOT_ALLOWED, "词汇表只允许选择一项");
    if (v.hierarchical()
        && (!values.isEmpty() && values.stream().filter(AssignmentInput::primary).count() != 1
            || v.required() && values.isEmpty()))
      throw new BusinessException(TaxonomyErrors.PRIMARY_INVALID, "必须且只能有一个主分类");
    if (values.stream().map(AssignmentInput::termId).distinct().count() != values.size())
      throw new IllegalArgumentException("分类指派不能重复");
    for (var value : values)
      if (findTerm(value.termId()).filter(t -> t.vocabularyKey().equals(vocabulary)).isEmpty())
        throw new NotFoundException(TaxonomyErrors.TERM_NOT_FOUND, "分类项不存在");
    store.atomic(
        rows -> {
          rows.put(
              root() + "assignment:" + id + ":" + vocabulary,
              values.stream()
                  .map(
                      value ->
                          new TermAssignment(
                              id, value.termId(), vocabulary, value.primary(), value.sort()))
                  .toList());
          return null;
        });
  }
}
