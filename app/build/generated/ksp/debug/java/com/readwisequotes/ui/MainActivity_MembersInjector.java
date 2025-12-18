package com.readwisequotes.ui;

import com.readwisequotes.data.QuoteRepository;
import com.readwisequotes.settings.SettingsManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SettingsManager> settingsManagerProvider;

  private final Provider<QuoteRepository> quoteRepositoryProvider;

  public MainActivity_MembersInjector(Provider<SettingsManager> settingsManagerProvider,
      Provider<QuoteRepository> quoteRepositoryProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
    this.quoteRepositoryProvider = quoteRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SettingsManager> settingsManagerProvider,
      Provider<QuoteRepository> quoteRepositoryProvider) {
    return new MainActivity_MembersInjector(settingsManagerProvider, quoteRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
    injectQuoteRepository(instance, quoteRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.readwisequotes.ui.MainActivity.settingsManager")
  public static void injectSettingsManager(MainActivity instance, SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }

  @InjectedFieldSignature("com.readwisequotes.ui.MainActivity.quoteRepository")
  public static void injectQuoteRepository(MainActivity instance, QuoteRepository quoteRepository) {
    instance.quoteRepository = quoteRepository;
  }
}
