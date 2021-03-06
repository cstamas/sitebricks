package com.google.sitebricks.mail;

import com.google.inject.ImplementedBy;

import java.util.concurrent.TimeUnit;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@ImplementedBy(SitebricksMail.class)
public interface Mail {
  AuthBuilder clientOf(String host, int port);

  public enum Auth { PLAIN, SSL }

  public static interface AuthBuilder {
    AuthBuilder timeout(long amount, TimeUnit unit);

    MailClient connect(Auth authType, String username, String password);
  }
}
