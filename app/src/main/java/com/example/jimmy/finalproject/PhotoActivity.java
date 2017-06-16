package com.example.jimmy.finalproject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.jimmy.finalproject.helper.ImageHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 0;
    private static final int PICKER = 100;
    String imgPath;
    StorageReference mStorageRef;
    Button get_local, upload;
    ImageView photo;
    TextView infoText;
    ProgressBar imgUploadProgress;
    StorageReference riversRef;
    ImageButton Okay;
    static String name;
    String str = "";

    // The URI of the image selected to detect.
    private Uri mImageUri;
    // The image selected to detect.
    private Bitmap mBitmap;
    private EmotionServiceClient client;
    int temp = 1;
    DatabaseReference myRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        Bundle bundle = this.getIntent().getExtras();
        name = bundle.getString("name");
        mStorageRef = FirebaseStorage.getInstance().getReference();
        if (client == null) {
            client = new EmotionServiceRestClient(getString(R.string.subscription_key));
        }

        get_local = (Button)findViewById(R.id.btn_local);
        upload = (Button)findViewById(R.id.btn_upload);
        photo = (ImageView)findViewById(R.id.iv_photo);
        infoText = (TextView)findViewById(R.id.tv_info);
        imgUploadProgress = (ProgressBar)findViewById(R.id.pgb_load);
        Okay = (ImageButton)findViewById(R.id.ib_ok);

        imgUploadProgress.setVisibility(View.INVISIBLE);
        get_local.setOnClickListener(doClick);
        upload.setOnClickListener(doClick);
        Okay.setOnClickListener(doClick);
    }

    private Button.OnClickListener doClick = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btn_local:
                    checkPermission();
                    getLocalImg();
                    break;
                case R.id.btn_upload:
                    if(!TextUtils.isEmpty(imgPath)) {
                        imgUploadProgress.setVisibility(View.VISIBLE);
                        uploadImg(imgPath);
//                        Toast.makeText(PhotoActivity.this, happy_val, Toast.LENGTH_SHORT).show();
                    } else{
                        Toast.makeText(PhotoActivity.this, "請選擇照片", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.ib_ok:
                    Toast.makeText(PhotoActivity.this, "開心指數成功上傳", Toast.LENGTH_SHORT).show();
                    myRef = FirebaseDatabase.getInstance().getReference(name);
                    myRef.child("happiness").setValue(str);

                    Intent intent = new Intent();
                    intent.setClass(PhotoActivity.this, ResultActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("name", name);
                    bundle.putInt("count",1);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    break;
            }
        }
    };

    private void checkPermission(){
        int permission = ActivityCompat.checkSelfPermission(PhotoActivity.this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //未取得權限，向使用者要求允許權限
            ActivityCompat.requestPermissions(this,
                    new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            getLocalImg();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocalImg();
                } else {
                    Toast.makeText(PhotoActivity.this, "do nothing", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void getLocalImg() {
        Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
        picker.setType("image/*");
        picker.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        Intent destIntent = Intent.createChooser(picker, null);
        startActivityForResult(destIntent, PICKER);
    }

    private void uploadImg(String path){
        Uri file = Uri.fromFile(new File(path));
        StorageMetadata metadata = new StorageMetadata.Builder()    //透過 Metadata 的方式來把附加資訊加入至檔案
                .setContentDisposition("universe")
                .setContentType("image/jpg")
                .build();

        riversRef = mStorageRef.child(name + ".jpg");
        UploadTask uploadTask = riversRef.putFile(file, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                infoText.setText(exception.getMessage());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                infoText.setText("上傳成功");
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                @SuppressWarnings("VisibleForTests") int progress = (int)((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                imgUploadProgress.setProgress(progress);
                if(progress >= 100){
                    imgUploadProgress.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                mImageUri = data.getData();
                imgPath = getPath(PhotoActivity.this, mImageUri);
                if(!TextUtils.isEmpty(imgPath)){
                    Toast.makeText(PhotoActivity.this, imgPath, Toast.LENGTH_SHORT).show();
                    Glide.with(PhotoActivity.this).load(imgPath).into(photo);
                } else{
                    Toast.makeText(PhotoActivity.this, "讀取相片失敗", Toast.LENGTH_SHORT).show();
                }
                // If image is selected successfully, set the image URI and bitmap.


                mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                        mImageUri, getContentResolver());
                if (mBitmap != null) {
                    // Show the image on screen.
                    //ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                    photo.setImageBitmap(mBitmap);

                    // Add detection log.
                    Log.d("RecognizeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                            + "x" + mBitmap.getHeight());

                    doRecognize();
                }
            }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    //選完照片以後會發現回來是一個Uri,所以這時候需要一個方法來找到對應的路徑 Android SDK 4.4後改如下
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public void doRecognize() {
        get_local.setEnabled(false);

        // Do emotion detection using auto-detected faces.
        try {
            new doRequest(false).execute();
        } catch (Exception e) {
            //mEditText.append("Error encountered. Exception is: " + e.toString());
            Toast.makeText(PhotoActivity.this, "Error encountered. Exception is: " + e.toString(), Toast.LENGTH_SHORT).show();
        }

        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        if (faceSubscriptionKey.equalsIgnoreCase("Please_add_the_face_subscription_key_here")) {
            //mEditText.append("\n\nThere is no face subscription key in res/values/strings.xml. Skip the sample for detecting emotions using face rectangles\n");
            Toast.makeText(PhotoActivity.this, "\n\nThere is no face subscription key in res/values/strings.xml. Skip the sample for detecting emotions using face rectangles\n", Toast.LENGTH_SHORT).show();

        } else {
            // Do emotion detection using face rectangles provided by Face API.
            try {
                new doRequest(true).execute();
            } catch (Exception e) {
                //mEditText.append("Error encountered. Exception is: " + e.toString());
            }
        }
    }

    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
        Log.d("emotion", "Do emotion detection with known face rectangles");
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long timeMark = System.currentTimeMillis();
        Log.d("emotion", "Start face detection using Face API");
        FaceRectangle[] faceRectangles = null;
        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        FaceServiceRestClient faceClient = new FaceServiceRestClient(faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, null);
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();

            timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start emotion detection using Emotion API");
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------
            result = this.client.recognizeImage(inputStream, faceRectangles);

            String json = gson.toJson(result);
            Log.d("result", json);
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
        }
        return result;
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            if (this.useFaceRectangles == false) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            } else {
                try {
                    return processWithFaceRectangles();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence

            if (this.useFaceRectangles == false) {
                //mEditText.append("\n\nRecognizing emotions with auto-detected face rectangles...\n");
            } else {
                //mEditText.append("\n\nRecognizing emotions with existing face rectangles from Face API...\n");
            }
            if (e != null) {
                //mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    //mEditText.append("No emotion detected :(");
                } else {
                    Integer count = 0;
                    // Covert bitmap to a mutable bitmap by copying it
                    Bitmap bitmapCopy = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas faceCanvas = new Canvas(bitmapCopy);
                    faceCanvas.drawBitmap(mBitmap, 0, 0, null);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.RED);

                    for (RecognizeResult r : result) {
                        if(str.contains(String.format("\nFace #%1$d : ", count))){
                            break;
                        }
                        str = str + String.format("\nFace #%1$d : ", count) + String.format("\t happiness: %1$.5f\n", r.scores.happiness);
                        //mEditText.append(String.format("\nFace #%1$d \n", count));
                        Toast.makeText(PhotoActivity.this, String.format("\nFace #%1$d \n", count), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t anger: %1$.5f\n", r.scores.anger));
                        //Toast.makeText(PhotoActivity.this, String.format("\t anger: %1$.5f\n", r.scores.anger), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t contempt: %1$.5f\n", r.scores.contempt));
                        //Toast.makeText(PhotoActivity.this, String.format("\t contempt: %1$.5f\n", r.scores.contempt), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t disgust: %1$.5f\n", r.scores.disgust));
                        //Toast.makeText(PhotoActivity.this, String.format("\t disgust: %1$.5f\n", r.scores.disgust), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t fear: %1$.5f\n", r.scores.fear));
                        //Toast.makeText(PhotoActivity.this, String.format("\t fear: %1$.5f\n", r.scores.fear), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t happiness: %1$.5f\n", r.scores.happiness));
                        //Toast.makeText(PhotoActivity.this, String.format("\t happiness: %1$.5f\n", r.scores.happiness), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t neutral: %1$.5f\n", r.scores.neutral));
                        //Toast.makeText(PhotoActivity.this, String.format("\t neutral: %1$.5f\n", r.scores.neutral), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t sadness: %1$.5f\n", r.scores.sadness));
                        //Toast.makeText(PhotoActivity.this, String.format("\t sadness: %1$.5f\n", r.scores.sadness), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t surprise: %1$.5f\n", r.scores.surprise));
                        //Toast.makeText(PhotoActivity.this, String.format("\t surprise: %1$.5f\n", r.scores.surprise), Toast.LENGTH_SHORT).show();
                        //mEditText.append(String.format("\t face rectangle: %d, %d, %d, %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height));
                        //Toast.makeText(PhotoActivity.this, String.format("\t face rectangle: %d, %d, %d, %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height), Toast.LENGTH_SHORT).show();
                        faceCanvas.drawRect(r.faceRectangle.left,
                                r.faceRectangle.top,
                                r.faceRectangle.left + r.faceRectangle.width,
                                r.faceRectangle.top + r.faceRectangle.height,
                                paint);
                        count++;
                    }
                    //ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                    photo.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));

                }
                //mEditText.setSelection(0);
            }

            get_local.setEnabled(true);
        }
    }
}
