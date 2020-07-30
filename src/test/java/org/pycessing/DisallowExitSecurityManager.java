package org.pycessing;

import java.security.Permission;

public class DisallowExitSecurityManager extends SecurityManager {
  private final SecurityManager delegatedSecurityManager;
  
  public DisallowExitSecurityManager(final SecurityManager originalSecurityManager) {
    this.delegatedSecurityManager = originalSecurityManager;
  }

  @Override
  public void checkExit(final int statusCode) {
    throw new SecurityException("System.exit called with status: " + statusCode);
  }
  
  @Override
  public void checkPermission(Permission perm) {
    if (delegatedSecurityManager != null) {
      delegatedSecurityManager.checkPermission(perm);
    }
  }
  
  @Override
  public void checkPermission(Permission perm, Object context) {
    if (delegatedSecurityManager != null) {
      delegatedSecurityManager.checkPermission(perm, context);
    }
  }
}
