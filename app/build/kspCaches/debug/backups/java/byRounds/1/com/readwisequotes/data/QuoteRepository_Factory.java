package com.readwisequotes.data;

import com.readwisequotes.data.local.QuoteDao;
import com.readwisequotes.data.remote.ReadwiseApi;
import com.readwisequotes.settings.SettingsManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class QuoteRepository_Factory implements Factory<QuoteRepository> {
  private final Provider<QuoteDao> quoteDaoProvider;

  private final Provider<ReadwiseApi> apiProvider;

  private final Provider<SettingsManager> settingsManagerProvider;

  public QuoteRepository_Factory(Provider<QuoteDao> quoteDaoProvider,
      Provider<ReadwiseApi> apiProvider, Provider<SettingsManager> settingsManagerProvider) {
    this.quoteDaoProvider = quoteDaoProvider;
    this.apiProvider = apiProvider;
    this.settingsManagerProvider = settingsManagerProvider;
  }

  @Override
  public QuoteRepository get() {
    return newInstance(quoteDaoProvider.get(), apiProvider.get(), settingsManagerProvider.get());
  }

  public static QuoteRepository_Factory create(Provider<QuoteDao> quoteDaoProvider,
      Provider<ReadwiseApi> apiProvider, Provider<SettingsManager> settingsManagerProvider) {
    return new QuoteRepository_Factory(quoteDaoProvider, apiProvider, settingsManagerProvider);
  }

  public static QuoteRepository newInstance(QuoteDao quoteDao, ReadwiseApi api,
      SettingsManager settingsManager) {
    return new QuoteRepository(quoteDao, api, settingsManager);
  }
}
