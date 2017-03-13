package com.mudit.android.hawkeye;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class FetchTask extends AsyncTask <Void,Void,Void> {

    private AsyncResponse asyncResponse;
    File file;
    List<Result> search_result = new ArrayList<>();

    public FetchTask(File file, AsyncResponse asyncResponse) {
        this.file = file;
        this.asyncResponse=asyncResponse;
    }

    @Override
    protected Void doInBackground(Void... params) {


        VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
        service.setApiKey("api_key_here");
        Uri uri = Uri.parse("android.resource://your.package.here/drawable/image_name");
        // File file1 = new File(Environment.getExternalStoragePublicDirectory("/Canvas Vision"), "1993141.jpg" );

        System.out.println("Classify an image");
        ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                .images(file)
                .build();
        VisualClassification result = service.classify(options).execute();

        System.out.println(result);


        if (result.getImages() != null) {
            List<VisualClassifier> resultClasses = result.getImages().get(0).getClassifiers();
            if (resultClasses.size() > 0) {
                VisualClassifier classifier = resultClasses.get(0);
                List<VisualClassifier.VisualClass> classList = classifier.getClasses();
                if (classList.size() > 0) {

                    for(int i=0;i<classList.size();i++) {

                        Result r =new Result(classList.get(i).getName(),classList.get(i).getScore());
                        search_result.add(r);
                        Log.e(TAG, "classifier name: " + r);
                    }
                    if(search_result!=null){
                        asyncResponse.processFinish(search_result);
                    }




                }
            }
        }

        return null;
    }
}





