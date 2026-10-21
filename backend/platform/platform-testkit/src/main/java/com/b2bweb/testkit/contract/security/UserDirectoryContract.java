package com.b2bweb.testkit.contract.security;
import static org.assertj.core.api.Assertions.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.port.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Shared consumer behavior; provider harnesses supply only the API and fixture controls. */
public abstract class UserDirectoryContract {
 protected abstract UserDirectory api();
 protected abstract void site(long id);
 protected abstract void seed(UserSummary user,boolean administrator,Set<Long> sites);
 protected abstract void grant(long user,String permission,DataScope data,LocaleScope locales);
 @Test public void batchMissingAndSiteLocaleMembership() {
  site(1);seed(new UserSummary(11,"用户","user@example.test",true),false,Set.of(1L));seed(new UserSummary(12,"管理员","admin@example.test",true),true,Set.of());grant(11,"content:edit",DataScope.OWN,LocaleScope.of(Set.of("zh-CN")));
  assertThat(api().find(99999)).isEmpty();assertThat(api().findAll(List.of())).isEmpty();assertThat(api().findAll(List.of(11L,99999L))).containsOnlyKeys(11L);
  assertThat(api().usersWithPermission("content:edit","en")).extracting(UserSummary::userId).containsExactly(12L);assertThat(api().isActiveMember(11)).isTrue();
  site(2);assertThat(api().isActiveMember(11)).isFalse();
 }

}
