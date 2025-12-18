package com.readwisequotes.di;

import com.readwisequotes.data.local.AppDatabase;
import com.readwisequotes.data.local.QuoteDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideQuoteDaoFactory implements Factory<QuoteDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideQuoteDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public QuoteDao get() {
    return provideQuoteDao(databaseProvider.get());
  }

  public static AppModule_ProvideQuoteDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideQuoteDaoFactory(databaseProvider);
  }

  public static QuoteDao provideQuoteDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideQuoteDao(database));
  }
}
