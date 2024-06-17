package com.example.myapplication;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    private static final int READ_MEDIA_IMAGES_PERMISSION_CODE = 1001;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 1002;
    //private static final String UPLOAD_URL = "http://127.0.0.1:8000/api_root/Post/";
//    private static final String UPLOAD_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final String UPLOAD_URL = "http://sohy3110.pythonanywhere.com/api_root/Post/";
    Uri imageUri = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(//...코드 계속
                new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    String filePath = getRealPathFromURI(imageUri); executorService.execute(() -> {
                        String uploadResult; try {
                            uploadResult = uploadImage(filePath); } catch (IOException e) {
                            uploadResult = "Upload failed: " + e.getMessage(); } catch (JSONException e) {
                            throw new RuntimeException(e); }
                        String finalUploadResult = uploadResult;
                        handler.post(() -> Toast.makeText(MainActivity.this, finalUploadResult, Toast.LENGTH_LONG).show()); });
                } }
    );
    @Override
    protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setContentView(R.layout.activity_main);
        Button uploadButton = findViewById(R.id.uploadButton); uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                                READ_MEDIA_IMAGES_PERMISSION_CODE);
                    }
                    else{
                        openImagePicker(); }
                }else{
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                    }
                    else{
                        openImagePicker(); }
                } }
        }); }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_MEDIA_IMAGES_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            }else{
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent); }
    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null); if (cursor == null) {
            return contentUri.getPath(); }else{
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA); String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
    }


    private String uploadImage(String imageUrl) throws IOException, JSONException {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String twoHyphens = "--";
        String lineEnd = "\r\n";

        File imageFile = new File(imageUrl);
        FileInputStream fileInputStream = new FileInputStream(imageFile);

        URL url = new URL(UPLOAD_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Authorization", "JWT b181ce4155b7413ebd1d86f1379151a7e035f8bd");

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());


        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd);
        outputStream.writeBytes(lineEnd);
        outputStream.write("안드로이드-REST API 테스트".getBytes("UTF-8"));
        outputStream.writeBytes(lineEnd);

        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd);
        outputStream.writeBytes(lineEnd);
        outputStream.write("안드로이드로 작성된 REST API 테스트 입력입니다.".getBytes("UTF-8"));
        outputStream.writeBytes(lineEnd);

        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"created_date\"" + lineEnd);
        outputStream.writeBytes(lineEnd);
        outputStream.write("2024-06-03T18:34:00+09:00".getBytes("UTF-8"));
        outputStream.writeBytes(lineEnd);

        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"published_date\"" + lineEnd);
        outputStream.writeBytes(lineEnd);
        outputStream.write("2024-06-03T18:34:00+09:00".getBytes("UTF-8"));
        outputStream.writeBytes(lineEnd);

        // 파일 데이터 전송
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + imageFile.getName() + "\"" + lineEnd);
        outputStream.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(imageFile.getName()) + lineEnd);
        outputStream.writeBytes(lineEnd);

        int bytesAvailable = fileInputStream.available();
        int bufferSize = Math.min(bytesAvailable, 1 * 1024 * 1024);
        byte[] buffer = new byte[bufferSize];
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bytesRead);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, 1 * 1024 * 1024);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

        fileInputStream.close();
        outputStream.flush();
        outputStream.close();

        int serverResponseCode = connection.getResponseCode();
        if (serverResponseCode == 200  || serverResponseCode == 201) {
            Log.e("uploadImage", "Success");
            connection.disconnect();
            return "Upload successful";
        } else {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                reader.close();
                Log.e("uploadImage", "Failed to upload, response code: " + serverResponseCode + ", error: " + errorResponse.toString());
                connection.disconnect();
                return "Upload failed: " + errorResponse.toString();
            } else {
                Log.e("uploadImage", "Failed to upload, response code: " + serverResponseCode + ", but no error message received.");
                connection.disconnect();
                return "Upload failed: server responded with code " + serverResponseCode + ", but no error message received.";
            }
        }
    }

}