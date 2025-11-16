package com.example.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private Uri selectedImageUri;
    private RecyclerView recyclerView;
    private ImageAdapter adapter;

    private List<PostItem> originalList = new ArrayList<>();

    private boolean isAscending = false;

    private static final String DJANGO_BASE_URL = "http://10.0.2.2:8000";
    private static final String DJANGO_API_PATH = "/api_root/Post/";
    private static final String TOKEN = "4988a3d8f97982de343af1b4f41550088b123d44";

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ImageAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        Button btnSort = findViewById(R.id.btn_sort);
        btnSort.setOnClickListener(v -> toggleSortOrder());

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setQueryHint("검색 (예: person, phone)");
        searchView.setIconifiedByDefault(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filterImages(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filterImages(newText);
                return true;
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            new UploadImageTask().execute(selectedImageUri);
                        }
                    }
                });
    }

    private void toggleSortOrder() {
        isAscending = !isAscending;

        if (isAscending) {
            textView.setText("오름차순 정렬됨");
            Toast.makeText(this, "오름차순 정렬 성공!", Toast.LENGTH_SHORT).show();
        } else {
            textView.setText("내림차순 정렬됨");
            Toast.makeText(this, "내림차순 정렬 성공!", Toast.LENGTH_SHORT).show();
        }

        new CloadImage().execute(DJANGO_BASE_URL + DJANGO_API_PATH);
    }

    public void onClickDownload(View v) {
        textView.setText("이미지 갱신 중...");
        new CloadImage().execute(DJANGO_BASE_URL + DJANGO_API_PATH);
    }

    public void onClickUpload(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private class CloadImage extends AsyncTask<String, Integer, List<PostItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            adapter.clearItems();
        }

        @Override
        protected List<PostItem> doInBackground(String... urls) {
            List<PostItem> postList = new ArrayList<>();
            try {
                URL urlAPI = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + TOKEN);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    is.close();

                    JSONArray aryJson = new JSONArray(result.toString());
                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject postJson = aryJson.getJSONObject(i);

                        String title = postJson.getString("title");
                        String imageUrl = postJson.getString("image");

                        if (imageUrl.contains("127.0.0.1")) {
                            imageUrl = imageUrl.replace("127.0.0.1", "10.0.2.2");
                        }

                        URL imgURL = new URL(imageUrl);
                        HttpURLConnection imgConn = (HttpURLConnection) imgURL.openConnection();
                        imgConn.setConnectTimeout(3000);
                        imgConn.setReadTimeout(3000);
                        InputStream imgStream = imgConn.getInputStream();
                        Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                        postList.add(new PostItem(title, imageBitmap));
                        imgStream.close();
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return postList;
        }

        @Override
        protected void onPostExecute(List<PostItem> items) {
            if (items == null || items.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
                return;
            }

            originalList = new ArrayList<>(items);

            if (isAscending) Collections.reverse(items);

            adapter.setItems(items);
            textView.setText("이미지 로드 성공!");
        }
    }

    private class UploadImageTask extends AsyncTask<Uri, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            String boundary = "----AndroidBoundary" + System.currentTimeMillis();
            try {
                URL url = new URL(DJANGO_BASE_URL + DJANGO_API_PATH);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Token " + TOKEN);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
                InputStream inputStream = getContentResolver().openInputStream(imageUri);

                outputStream.writeBytes("--" + boundary + "\r\n");
                outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"\r\n");
                outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.writeBytes("\r\n--" + boundary + "--\r\n");

                inputStream.close();
                outputStream.flush();
                outputStream.close();

                int responseCode = conn.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(MainActivity.this, "업로드 성공!", Toast.LENGTH_SHORT).show();
                new CloadImage().execute(DJANGO_BASE_URL + DJANGO_API_PATH);
            } else {
                Toast.makeText(MainActivity.this, "업로드 실패!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
