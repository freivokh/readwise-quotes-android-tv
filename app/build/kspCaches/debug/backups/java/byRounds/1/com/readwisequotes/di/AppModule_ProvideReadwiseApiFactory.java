package com.readwisequotes.di;

import com.readwisequotes.data.remote.ReadwiseApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class AppModule_ProvideReadwiseApiFactory implements Factory<ReadwiseApi> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public AppModule_ProvideReadwiseApiFactory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public ReadwiseApi get() {
    return provideReadwiseApi(okHttpClientProvider.get());
  }

  public static AppModule_ProvideReadwiseApiFactory create(
      Provider<OkHttpClient> okHttpClientProvider) {
    return new AppModule_ProvideReadwiseApiFactory(okHttpClientProvider);
  }

  public static ReadwiseApi provideReadwiseApi(OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideReadwiseApi(okHttpClient));
  }
}
