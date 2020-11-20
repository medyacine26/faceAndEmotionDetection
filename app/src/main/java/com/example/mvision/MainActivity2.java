package com.example.mvision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity2 extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    JavaCameraView javaCameraView;
    File cascFile;
    CascadeClassifier faceDetector;
    Mat mRgba,mGray;

    final int DELAY=1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        javaCameraView=findViewById(R.id.javCamView);

        if(!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0,this,baseCallback);
        }
        else
        {
            Log.d("opencv","loaded");

            baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }

        javaCameraView.setCvCameraViewListener(this);




    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        mRgba=new Mat();
        mGray=new Mat();

    }

    @Override
    public void onCameraViewStopped() {

        mRgba.release();
        mGray.release();
    }
    boolean sendingPicture=false;
    String res="";
    void sendImage(final byte[] byteArray)
    {




                String serverUrl="http://10.0.2.2:5000/getEmotion";
                OkHttpClient client = new OkHttpClient();


                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("image", "image.png",
                                RequestBody.create(byteArray, MediaType.parse("image/*png")))
                        .build();

                try
                {


                    Request request = new Request.Builder()
                            .url(serverUrl)
                            .post(requestBody)
                            .build();


                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            //Log.d("http response","response: "+response.body().string());
                            res = Objects.requireNonNull(response.body()).string()+"";

                        }

                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {

                            e.printStackTrace();

                        }


                    });

                   // emotionTv.setText(res);

                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }


    }
    MatOfRect faceDetections;
    Rect rect;
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();

        faceDetections= new MatOfRect();

        faceDetector.detectMultiScale(mRgba,faceDetections);


//        for(Rect rect: faceDetections.toArray())
//        {
        if(faceDetections.toList().size()>0)
        {


            rect=faceDetections.toList().get(0);
            Bitmap bmp = null;
            bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgba, bmp);



            Imgproc.rectangle(mRgba,new Point(rect.x,rect.y),new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0,255,0));
            Imgproc.putText(mRgba,res,new Point(rect.x+rect.width,rect.y), Core.FONT_HERSHEY_SIMPLEX,1,new Scalar(0,255,0));

            try {

                if(!sendingPicture)
                {

                    sendingPicture=true;




                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    sendImage(byteArray);

                    stream.flush();
                    stream.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    sendingPicture=false;

                                }
                            },DELAY);

                        }
                    });



                }
            }
            catch (CvException e){Log.d("Exception",e.getMessage());} catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            res="";
        }
       // }


        return mRgba;
    }

    private BaseLoaderCallback baseCallback=new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {


                    InputStream is=getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File casecadeDir=getDir("cascade", Context.MODE_PRIVATE);
                    cascFile=new File(casecadeDir,"haarcascade_frontalface_alt2.xml");
                    try {
                        FileOutputStream fos= new FileOutputStream(cascFile);
                        byte[] buffer=new byte[4096];
                        int bytesRead;
                        while((bytesRead=is.read(buffer))!=-1)
                        {
                            fos.write(buffer,0,bytesRead);
                        }

                        is.close();
                        fos.close();

                        faceDetector=new CascadeClassifier(cascFile.getAbsolutePath());
                        if(faceDetector.empty())
                        {
                            faceDetector=null;

                        }
                        else
                        {
                            casecadeDir.delete();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    javaCameraView.enableView();

                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;

            }


        }
    };
}