package com.b2bweb.testkit.form;

import com.b2bweb.api.form.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.util.*;

public final class InMemoryFormApi implements FormApi {
  final MemoryStore store;
  final SiteContextProvider sites;

  public InMemoryFormApi(MemoryStore store, SiteContextProvider sites) {
    this.store = store;
    this.sites = sites;
  }

  String prefix() {
    return sites.requireSiteId() + ":form:";
  }

  public void addForm(FormSummary form) {
    store.atomic(
        rows -> {
          rows.put(prefix() + "definition:" + form.formId(), form);
          return null;
        });
  }

  public void addSubmission(SubmissionView submission) {
    store.atomic(
        rows -> {
          rows.put(prefix() + "submission:" + submission.submissionId(), submission);
          return null;
        });
  }

  public Optional<FormSummary> findForm(long id) {
    return store.atomic(
        rows -> Optional.ofNullable((FormSummary) rows.get(prefix() + "definition:" + id)));
  }

  public Optional<FormSummary> findForm(String key) {
    return forms(null).stream().filter(f -> f.formKey().equals(key)).findFirst();
  }

  public List<FormSummary> forms(FormPurpose purpose) {
    String prefix = prefix() + "definition:";
    return store.atomic(
        rows ->
            rows.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> (FormSummary) e.getValue())
                .filter(f -> purpose == null || f.purpose() == purpose)
                .sorted(Comparator.comparingLong(FormSummary::formId))
                .toList());
  }

  public Optional<SubmissionView> findSubmission(long id) {
    return store.atomic(
        rows -> Optional.ofNullable((SubmissionView) rows.get(prefix() + "submission:" + id)));
  }
}
