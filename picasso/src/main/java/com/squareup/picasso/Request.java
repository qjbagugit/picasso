package com.squareup.picasso;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Future;

import static com.squareup.picasso.Utils.createKey;

class Request implements Runnable {
  static final int DEFAULT_RETRY_COUNT = 2;

  enum Type {
    CONTENT,
    FILE,
    STREAM,
    RESOURCE
  }

  enum LoadedFrom {
    MEMORY(Color.GREEN),
    DISK(Color.YELLOW),
    NETWORK(Color.RED);

    final int debugColor;

    LoadedFrom(int debugColor) {
      this.debugColor = debugColor;
    }
  }

  final Picasso picasso;
  final String path;
  final int resourceId;
  final String key;
  final int errorResId;
  final boolean skipCache;
  final Type type;
  final WeakReference<ImageView> target;
  final PicassoBitmapOptions options;
  final List<Transformation> transformations;
  final Drawable errorDrawable;

  Future<?> future;
  Bitmap result;
  LoadedFrom loadedFrom;
  int retryCount;
  boolean retryCancelled;

  Request(Picasso picasso, String path, int resourceId, ImageView imageView,
      PicassoBitmapOptions options, List<Transformation> transformations, Type type, int errorResId,
      boolean skipCache, Drawable errorDrawable) {
    this.picasso = picasso;
    this.path = path;
    this.resourceId = resourceId;
    this.type = type;
    this.errorResId = errorResId;
    this.errorDrawable = errorDrawable;
    this.skipCache = skipCache;
    this.target = new WeakReference<ImageView>(imageView);
    this.options = options;
    this.transformations = transformations;
    this.retryCount = DEFAULT_RETRY_COUNT;
    this.key = createKey(this);
  }

  Object getTarget() {
    return target.get();
  }

  void complete() {
    if (result == null) {
      throw new AssertionError(
          String.format("Attempted to complete request with no result!\n%s", this));
    }

    ImageView imageView = target.get();
    if (imageView != null) {
      imageView.setImageBitmap(result);
    }
  }

  void error() {
    ImageView target = this.target.get();
    if (target == null) {
      return;
    }
    if (errorResId != 0) {
      target.setImageResource(errorResId);
    } else if (errorDrawable != null) {
      target.setImageDrawable(errorDrawable);
    }
  }

  @Override public void run() {
    try {
      picasso.run(this);
    } catch (final Throwable e) {
      // If an unexpected exception happens, we should crash the app instead of letting the
      // executor swallow it.
      picasso.handler.post(new Runnable() {
        @Override public void run() {
          throw new RuntimeException("An unexpected exception occurred", e);
        }
      });
    }
  }

  @Override public String toString() {
    return "Request["
        + "hashCode="
        + hashCode()
        + ", picasso="
        + picasso
        + ", path="
        + path
        + ", resourceId="
        + resourceId
        + ", target="
        + target
        + ", options="
        + options
        + ", transformations="
        + transformationKeys()
        + ", future="
        + future
        + ", result="
        + result
        + ", retryCount="
        + retryCount
        + ", loadedFrom="
        + loadedFrom
        + ']';
  }

  String transformationKeys() {
    if (transformations == null) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder(transformations.size() * 16);

    sb.append('[');
    boolean first = true;
    for (Transformation transformation : transformations) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(transformation.key());
    }
    sb.append(']');

    return sb.toString();
  }
}
