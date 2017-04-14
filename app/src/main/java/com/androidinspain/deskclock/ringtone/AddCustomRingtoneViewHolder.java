/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.androidinspain.deskclock.ringtone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidinspain.deskclock.ItemAdapter;

import static android.view.View.GONE;

final class AddCustomRingtoneViewHolder extends ItemAdapter.ItemViewHolder<AddCustomRingtoneHolder>
        implements View.OnClickListener {

    static final int VIEW_TYPE_ADD_NEW = Integer.MIN_VALUE;
    static final int CLICK_ADD_NEW = VIEW_TYPE_ADD_NEW;

    private AddCustomRingtoneViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        final View selectedView = itemView.findViewById(com.androidinspain.deskclock.R.id.sound_image_selected);
        selectedView.setVisibility(GONE);

        final TextView nameView = (TextView) itemView.findViewById(com.androidinspain.deskclock.R.id.ringtone_name);
        nameView.setText(itemView.getContext().getString(com.androidinspain.deskclock.R.string.add_new_sound));
        nameView.setAlpha(0.63f);

        final ImageView imageView = (ImageView) itemView.findViewById(com.androidinspain.deskclock.R.id.ringtone_image);
        imageView.setImageResource(com.androidinspain.deskclock.R.drawable.ic_add_white_24dp);
        imageView.setAlpha(0.63f);
    }

    @Override
    public void onClick(View view) {
        notifyItemClicked(AddCustomRingtoneViewHolder.CLICK_ADD_NEW);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {

        private final LayoutInflater mInflater;

        Factory(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            final View itemView = mInflater.inflate(com.androidinspain.deskclock.R.layout.ringtone_item_sound, parent, false);
            return new AddCustomRingtoneViewHolder(itemView);
        }
    }
}