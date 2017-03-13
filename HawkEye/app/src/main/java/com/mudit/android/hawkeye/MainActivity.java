package com.mudit.android.hawkeye;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;

import com.wang.avi.*;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements AsyncResponse{

    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private RecyclerView recyclerView;
    private ResultAdapter resultAdapter;
    private List<Result> output =new ArrayList<>();
    private Cursor cursor;
    private AVLoadingIndicatorView avi;
    private TextView loadtext;
    private StreamPlayer streamPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.imageView = (ImageView)this.findViewById(R.id.imageView1);
   //     Button photoButton = (Button) this.findViewById(R.id.button1);
        recyclerView=(RecyclerView)this.findViewById(R.id.recycler_View);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultAdapter= new ResultAdapter(this);
        recyclerView.setAdapter(resultAdapter);
        loadtext=(TextView)this.findViewById(R.id.loadtext);
        avi =(AVLoadingIndicatorView)this.findViewById(R.id.indicator);
     stopAnim();
        /*   photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });*/
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
           // Bitmap photo = (Bitmap) data.getExtras().get("data");
            //imageView.setImageBitmap(photo);
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
   /* public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }*/
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            // TODO perform some logging or show user feedback
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
         cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
        //    cursor.close();
            return path;
        }
        // this is our fallback here
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

        if (item.getItemId() == R.id.action_camera) {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);


            }

        return super.onOptionsItemSelected(item);
    }

    void startAnim(){
        avi.show();
        loadtext.setVisibility(View.VISIBLE);
        // or avi.smoothToShow();
    }

    void stopAnim(){
        avi.hide();
        loadtext.setVisibility(View.GONE);
        // or avi.smoothToHide();
    }
    private TextToSpeech initTextToSpeechService(){
        TextToSpeech service = new TextToSpeech();
        String username = "username";
        String password = "password";
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
}
