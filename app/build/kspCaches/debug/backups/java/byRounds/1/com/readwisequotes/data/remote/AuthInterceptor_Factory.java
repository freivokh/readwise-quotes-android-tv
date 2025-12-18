package com.readwisequotes.data.remote;

import com.readwisequotes.settings.SettingsManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<SettingsManager> settingsManagerProvider;

  public AuthInterceptor_Factory(Provider<SettingsManager> settingsManagerProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(settingsManagerProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<SettingsManager> settingsManagerProvider) {
    return new AuthInterceptor_Factory(settingsManagerProvider);
  }

  public static AuthInterceptor newInstance(SettingsManager settingsManager) {
    return new AuthInterceptor(settingsManager);
  }
}
