package com.b2bweb.testkit.notification;

import com.b2bweb.api.common.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.site.*;
import com.b2bweb.testkit.common.MemoryStore;
import java.time.*;
import java.util.*;

public final class InMemoryNotificationApi implements NotificationApi {
  final MemoryStore store;
  final SiteContextProvider sites;
  final Clock clock;
  final IdGenerator ids;
  final Map<String, Set<String>> templates = new HashMap<>();

  record Message(NotificationRequest request, Recipient recipient, MessageStatusView status) {}

  public InMemoryNotificationApi(
      MemoryStore store, SiteContextProvider sites, Clock clock, IdGenerator ids) {
    this.store = store;
    this.sites = sites;
    this.clock = clock;
    this.ids = ids;
  }

  String prefix() {
    return sites.requireSiteId() + ":notification:";
  }

  public void template(String key, String locale, Set<String> variables) {
    templates.put(sites.requireSiteId() + ":" + key + ":" + locale, Set.copyOf(variables));
  }

  public boolean templateAvailable(String key, String locale) {
    return templates.containsKey(sites.requireSiteId() + ":" + key + ":" + locale);
  }

  public List<Long> send(NotificationRequest request) {
    String prefix = prefix();
    return store.atomic(
        rows -> {
          if (request.dedupKey() != null) {
            String key = prefix + "dedup:" + request.dedupKey();
            if (rows.containsKey(key)) {
              var ids = (List<?>) rows.get(key);
              return ids.stream().map(id -> (Long) id).toList();
            }
          }
          String locale = request.locale() == null ? "zh-CN" : request.locale();
          if (!templateAvailable(request.templateKey(), locale) && request.fallbackLocale() != null)
            locale = request.fallbackLocale();
          var vars =
              templates.get(sites.requireSiteId() + ":" + request.templateKey() + ":" + locale);
          if (vars == null)
            throw new BusinessException(NotificationErrors.TEMPLATE_NOT_FOUND, "通知模板不存在");
          for (String key : vars)
            if (!request.model().hasNonNull(key))
              throw new BusinessException(NotificationErrors.MODEL_INVALID, "缺少模板变量");
          if (request.recipients().isEmpty())
            throw new BusinessException(NotificationErrors.RECIPIENT_INVALID, "缺少收件人");
          List<Long> result = new ArrayList<>();
          for (Recipient recipient : request.recipients()) {
            long id = ids.nextId();
            var status =
                new MessageStatusView(
                    id,
                    request.templateKey(),
                    NotificationChannel.EMAIL,
                    MessageStatus.PENDING,
                    0,
                    null,
                    null,
                    clock.instant());
            rows.put(prefix + "message:" + id, new Message(request, recipient, status));
            result.add(id);
          }
          if (request.dedupKey() != null)
            rows.put(prefix + "dedup:" + request.dedupKey(), List.copyOf(result));
          return List.copyOf(result);
        });
  }

  public void webhook(String endpoint) {
    store.atomic(
        rows -> {
          rows.put(prefix() + "webhook:" + endpoint, endpoint);
          return null;
        });
  }

  public List<Long> sendWebhookEvent(WebhookEventRequest request) {
    String prefix = prefix();
    return store.atomic(
        rows -> {
          String key = prefix + "webhook-dedup:" + request.dedupKey();
          if (request.dedupKey() != null && rows.containsKey(key))
            return ((List<?>) rows.get(key)).stream().map(v -> (Long) v).toList();
          List<Long> result = new ArrayList<>();
          for (String endpoint : new ArrayList<>(rows.keySet()))
            if (endpoint.startsWith(prefix + "webhook:")) {
              long id = ids.nextId();
              rows.put(
                  prefix + "webhook-message:" + id,
                  new MessageStatusView(
                      id,
                      request.eventType(),
                      NotificationChannel.WEBHOOK,
                      MessageStatus.PENDING,
                      0,
                      null,
                      null,
                      clock.instant()));
              result.add(id);
            }
          if (request.dedupKey() != null) rows.put(key, List.copyOf(result));
          return List.copyOf(result);
        });
  }

  public Optional<MessageStatusView> status(long id) {
    return store.atomic(
        rows -> {
          var value = rows.get(prefix() + "message:" + id);
          if (value instanceof Message m) return Optional.of(m.status());
          return Optional.ofNullable(
              (MessageStatusView) rows.get(prefix() + "webhook-message:" + id));
        });
  }

  public List<NotificationRequest> sent() {
    String prefix = prefix() + "message:";
    return store.atomic(
        rows ->
            rows.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> ((Message) e.getValue()).request())
                .toList());
  }
}
