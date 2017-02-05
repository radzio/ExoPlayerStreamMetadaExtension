package net.droidlabs.exoplayer.icystream;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import okhttp3.CacheControl;
import okhttp3.Call;

public final class OkHttpDataSourceFactory implements HttpDataSource.Factory {

  private final Call.Factory callFactory;
  private final String userAgent;
  private final TransferListener<? super DataSource> listener;
  private final CacheControl cacheControl;

  /**
   * @param callFactory A {@link Call.Factory} (typically an {@link okhttp3.OkHttpClient}) for use
   * by the sources created by the factory.
   * @param userAgent The User-Agent string that should be used.
   * @param listener An optional listener.
   */
  public OkHttpDataSourceFactory(Call.Factory callFactory, String userAgent,
      TransferListener<? super DataSource> listener) {
    this(callFactory, userAgent, listener, null);
  }

  /**
   * @param callFactory A {@link Call.Factory} (typically an {@link okhttp3.OkHttpClient}) for use
   * by the sources created by the factory.
   * @param userAgent The User-Agent string that should be used.
   * @param listener An optional listener.
   * @param cacheControl An optional {@link CacheControl} for setting the Cache-Control header.
   */
  public OkHttpDataSourceFactory(Call.Factory callFactory, String userAgent,
      TransferListener<? super DataSource> listener, CacheControl cacheControl) {
    this.callFactory = callFactory;
    this.userAgent = userAgent;
    this.listener = listener;
    this.cacheControl = cacheControl;
  }

  @Override
  public OkHttpDataSource createDataSource() {
    return new OkHttpDataSource(callFactory, userAgent, null, listener, cacheControl);
  }
}