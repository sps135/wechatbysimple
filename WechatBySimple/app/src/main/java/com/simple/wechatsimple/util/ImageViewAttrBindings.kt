package com.simple.wechatsimple.util

import android.databinding.BindingAdapter
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

object ImageViewAttrBindings {
    @BindingAdapter(value = arrayOf("app:imageUrl", "app:placeHolder", "app:error"), requireAll = false)
    @JvmStatic
    fun loadImage(imageView: ImageView, url: String, holderDrawable: Drawable, errorDrawable: Drawable) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        Glide.with(imageView.context)
                .load(url)
                .apply(RequestOptions.placeholderOf(holderDrawable))
                .apply(RequestOptions.errorOf(errorDrawable))
                .into(imageView)
    }

    @BindingAdapter(value = arrayOf("app:imageUrl", "app:placeHolder", "app:error", "app:radius"), requireAll = false)
    @JvmStatic
    fun loadImage(imageView: ImageView, url: String, holderDrawable: Drawable, errorDrawable: Drawable, radius: Int) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        Glide.with(imageView.context)
                .load(url)
                .apply(RequestOptions.placeholderOf(holderDrawable))
                .apply(RequestOptions.errorOf(errorDrawable))
                .apply(RequestOptions.bitmapTransform(GlideRoundTransform(imageView.context, radius)))
                .into(imageView)
    }

    @BindingAdapter(value = arrayOf("app:bubblePercent", "app:showText"), requireAll = false)
    @JvmStatic
    fun bubblePercent(imageView: BubbleImageView, percent: Int, showText: Boolean) {
        imageView.setProgressVisible(showText)
        if (showText) {
            imageView.setPercent(percent)
        }
    }
}