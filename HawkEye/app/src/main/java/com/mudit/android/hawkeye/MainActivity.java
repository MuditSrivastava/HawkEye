package com.mudit.android.hawkeye;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.wang.avi.*;

import static android.R.attr.data;
import static android.R.attr.path;
import static com.ibm.watson.developer_cloud.android.library.camera.CameraHelper.REQUEST_IMAGE_CAPTURE;

public class MainActivity extends AppCompatActivity implements AsyncResponse{

    private static final int CAMERA_REQUEST = 1888;
    private static int RESULT_LOAD_IMAGE = 1;
    private ImageView imageView;
    private RecyclerView recyclerView;
    private ResultAdapter resultAdapter;
    private List<Result> output =new ArrayList<>();
    private Cursor cursor;
    private AVLoadingIndicatorView avi;
    private TextView loadtext;
    private StreamPlayer streamPlayer;
    private  int permissionCheck1, permissionCheck2,permissionCheck3;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE=245;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE=121;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA=174;
    private Uri photoURI;
    private File photoFile;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionCheck1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionCheck2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionCheck3 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA  );
        this.imageView = (ImageView)this.findViewById(R.id.imageView1);
        recyclerView=(RecyclerView)this.findViewById(R.id.recycler_View);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultAdapter= new ResultAdapter(this);
        recyclerView.setAdapter(resultAdapter);
        loadtext=(TextView)this.findViewById(R.id.loadtext);
        avi =(AVLoadingIndicatorView)this.findViewById(R.id.indicator);
        stopAnim();

        checkPermisson();


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK ) {
            if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Uri tempUri = data.getData();
                File finalFile = new File(getPath(tempUri));
                Picasso.with(this)
                        .load(finalFile)
                        .placeholder(R.drawable.ph1)
                        .into(imageView);
                startAnim();
                FetchTask search = new FetchTask(finalFile,this);
                search.execute();

            }
            else {
                        Picasso.with(this)
                        .load(photoFile)
                        .placeholder(R.drawable.ph1)
                        .into(imageView);
                startAnim();
                FetchTask search = new FetchTask(photoFile, this);
                search.execute();
            }

        }
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            Uri tempUri = data.getData();

            File finalFile = new File(getPath(tempUri));

            Picasso.with(this)
                    .load(finalFile)
                    .placeholder(R.drawable.ph1)
                    .into(imageView);
            startAnim();
            FetchTask search = new FetchTask(finalFile,this);
            search.execute();

        }
    }
    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        }
        return uri.getPath();
    }

    @Override
    public void processFinish(List<Result> output) {
        this.output=output;
        setResultAdapter();


    }

    @Override
    public void onDestroy(){
        if(cursor!=null){
            cursor.close();}
        super.onDestroy();
    }
    private void setResultAdapter(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                resultAdapter.setResultList(output);
                tts_task();
                stopAnim();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.acitivity_main_menu, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(this.isInternetConnectionPresent()){


        if (item.getItemId() == R.id.action_camera) {
            if(permissionCheck1==PackageManager.PERMISSION_GRANTED && permissionCheck2==PackageManager.PERMISSION_GRANTED && permissionCheck3==PackageManager.PERMISSION_GRANTED) {

                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                }else{

                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                         photoURI = FileProvider.getUriForFile(this,
                                "com.mudit.android.hawkeye.fileprovider",
                                photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }


            }}
            else{
                checkPermisson();

            }

        }
        if(item.getItemId()==R.id.action_browse){
            if(permissionCheck1==PackageManager.PERMISSION_GRANTED && permissionCheck2==PackageManager.PERMISSION_GRANTED) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
            else{
                checkPermisson();

            }

        }}
        else{
            Toast Connect_Internet= Toast.makeText(this,"Internet Connection Required",Toast.LENGTH_LONG);
            Connect_Internet.show();
        }

        return super.onOptionsItemSelected(item);
    }

    void startAnim(){
        avi.show();
        loadtext.setVisibility(View.VISIBLE);
    }

    void stopAnim(){
        avi.hide();
        loadtext.setVisibility(View.GONE);
    }
    private TextToSpeech initTextToSpeechService(){
        TextToSpeech service = new TextToSpeech();
        String username = BuildConfig.TTS_Username;
        String password = BuildConfig.TTS_Password;
        service.setUsernameAndPassword(username, password);
        return service;
    }

    private void tts_task(){
        initTextToSpeechService();

        WatsonTask task = new WatsonTask();
        task.execute();

    }

    private class WatsonTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... textToSpeak) {

            TextToSpeech textToSpeech = initTextToSpeechService();
            streamPlayer = new StreamPlayer();
            List<String> results = new ArrayList<>();

            if(output.size()>3){
                for(int i=0;i<3;i++) {
                    results.add(output.get(i).getname());
                    streamPlayer.playStream(textToSpeech.synthesize(String.valueOf(results.get(i)), Voice.EN_MICHAEL).execute());
                    if(i!=2){
                        streamPlayer.playStream(textToSpeech.synthesize(String.valueOf(" Or"), Voice.EN_MICHAEL).execute());

                    }

                }}
            else{
                for(int i=0;i<output.size();i++) {
                    results.add(output.get(i).getname());
                    streamPlayer.playStream(textToSpeech.synthesize(String.valueOf(results.get(i)), Voice.EN_MICHAEL).execute());
                    if(i!=output.size()-1){
                        streamPlayer.playStream(textToSpeech.synthesize(String.valueOf(" Or"), Voice.EN_MICHAEL).execute());

                    }
                }
            }

            return null;
        }
    }
    public boolean isInternetConnectionPresent() {
        ConnectivityManager connectivityManager = ((ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnectedOrConnecting());
    }

  public void checkPermisson(){
      if(permissionCheck1!=PackageManager.PERMISSION_GRANTED ) {
          ActivityCompat.requestPermissions(this,
                  new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_STORAGE);
      }
      if( permissionCheck2!=PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this,
                  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
      }
      if( permissionCheck3!=PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this,
                  new String[]{Manifest.permission.CAMERA},MY_PERMISSIONS_REQUEST_CAMERA);
      }

  }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

}
