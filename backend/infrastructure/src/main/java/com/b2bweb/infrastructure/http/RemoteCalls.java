package com.b2bweb.infrastructure.http;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class RemoteCalls {
  private RemoteCalls() {}

  public static void outsideTransaction() {
    if (TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("远程端口不得持有数据库事务");
  }
}
