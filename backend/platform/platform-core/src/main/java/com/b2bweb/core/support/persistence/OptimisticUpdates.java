package com.b2bweb.core.support.persistence;

import com.b2bweb.api.common.*;
import java.util.function.*;

public final class OptimisticUpdates {
  private OptimisticUpdates() {}

  /**
   * The caller performs the conditional update in its transaction; currentVersion must use the same
   * site-scoped repository.
   */
  public static void check(
      int affected, Supplier<Integer> currentVersion, ErrorCode conflict, ErrorCode missing) {
    if (affected == 1) return;
    if (affected != 0) throw new IllegalStateException("条件更新影响了多条记录");
    Integer current = currentVersion.get();
    if (current == null) throw new NotFoundException(missing, "记录不存在");
    throw new VersionConflictException(conflict, "记录已被其他操作修改", current);
  }
}
