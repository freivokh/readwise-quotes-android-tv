package com.readwisequotes;

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
public final class QuoteDreamService_MembersInjector implements MembersInjector<QuoteDreamService> {
  private final Provider<SettingsManager> settingsManagerProvider;

  private final Provider<QuoteRepository> quoteRepositoryProvider;

  public QuoteDreamService_MembersInjector(Provider<SettingsManager> settingsManagerProvider,
      Provider<QuoteRepository> quoteRepositoryProvider) {
    this.settingsManagerProvider = settingsManagerProvider;
    this.quoteRepositoryProvider = quoteRepositoryProvider;
  }

  public static MembersInjector<QuoteDreamService> create(
      Provider<SettingsManager> settingsManagerProvider,
      Provider<QuoteRepository> quoteRepositoryProvider) {
    return new QuoteDreamService_MembersInjector(settingsManagerProvider, quoteRepositoryProvider);
  }

  @Override
  public void injectMembers(QuoteDreamService instance) {
    injectSettingsManager(instance, settingsManagerProvider.get());
    injectQuoteRepository(instance, quoteRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.readwisequotes.QuoteDreamService.settingsManager")
  public static void injectSettingsManager(QuoteDreamService instance,
      SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }

  @InjectedFieldSignature("com.readwisequotes.QuoteDreamService.quoteRepository")
  public static void injectQuoteRepository(QuoteDreamService instance,
      QuoteRepository quoteRepository) {
    instance.quoteRepository = quoteRepository;
  }
}
