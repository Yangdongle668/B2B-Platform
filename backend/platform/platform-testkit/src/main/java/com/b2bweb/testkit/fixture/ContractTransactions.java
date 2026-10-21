package com.b2bweb.testkit.fixture;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
/** Resource-free transaction coordinator for doubles; real providers use their database manager. */
public final class ContractTransactions {
 private ContractTransactions() {}
 public static void run(Runnable action,boolean rollback) {
  var manager=new AbstractPlatformTransactionManager() {
   protected Object doGetTransaction(){return new Object();}
   protected void doBegin(Object tx,TransactionDefinition definition){}
   protected void doCommit(DefaultTransactionStatus status){}
   protected void doRollback(DefaultTransactionStatus status){}
  };
  new TransactionTemplate(manager).executeWithoutResult(status->{action.run();if(rollback)status.setRollbackOnly();});
 }
}
