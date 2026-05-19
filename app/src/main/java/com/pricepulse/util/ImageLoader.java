package com.pricepulse.util;

import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pricepulse.R;

// helper για να μη σκαει το glide οταν το url ειναι κενο/σπασμενο
public final class ImageLoader {

    private ImageLoader() {}

    public static void load(ImageView view, String url) {
        if (view == null) return;
        Glide.with(view)
                .load(url == null || url.trim().isEmpty() ? null : url)
                .placeholder(R.drawable.bg_rounded_image)
                .error(R.drawable.ic_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(view);
    }

    public static void load(ImageView view, Uri uri) {
        if (view == null) return;
        Glide.with(view)
                .load(uri)
                .placeholder(R.drawable.bg_rounded_image)
                .error(R.drawable.ic_image)
                .into(view);
    }
}
