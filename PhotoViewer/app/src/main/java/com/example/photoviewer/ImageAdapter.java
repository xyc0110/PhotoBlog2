package com.example.photoviewer;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<PostItem> itemList;
    private List<PostItem> originalList;
    private Context context;

    public ImageAdapter(Context context, List<PostItem> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.originalList = new ArrayList<>(itemList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostItem item = itemList.get(position);

        holder.imageView.setImageBitmap(item.image);
        holder.txtTitle.setText(item.title);

        holder.imageView.setOnClickListener(v -> showFullScreenImage(item.image));

        holder.btnSave.setOnClickListener(v -> saveImageToGallery(item.image));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void clearItems() {
        itemList.clear();
        notifyDataSetChanged();
    }

    public void setItems(List<PostItem> newItems) {
        itemList.clear();
        itemList.addAll(newItems);
        originalList = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    public void filterImages(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            itemList = new ArrayList<>(originalList);
            notifyDataSetChanged();
            return;
        }

        keyword = keyword.toLowerCase();
        List<PostItem> filtered = new ArrayList<>();

        for (PostItem p : originalList) {
            if (p.title.toLowerCase().contains(keyword)) {
                filtered.add(p);
            }
        }

        itemList = filtered;
        notifyDataSetChanged();
    }

    private void showFullScreenImage(Bitmap image) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullImageView = dialog.findViewById(R.id.fullscreenImageView);
        fullImageView.setImageBitmap(image);
        fullImageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveImageToGallery(Bitmap bitmap) {
        try {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoViewer_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoViewer");
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream out = resolver.openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView txtTitle;
        Button btnSave;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            txtTitle = itemView.findViewById(R.id.imageTitle);
            btnSave = itemView.findViewById(R.id.btnSave);
        }
    }
}
