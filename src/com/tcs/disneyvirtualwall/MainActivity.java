package com.tcs.disneyvirtualwall;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import com.tcs.disneyvirtualwall.youtube.PlayerViewDemoActivity;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = "CAMERA::Activity";
	private static final boolean D = false;
	private static final int BOUNDARY = 35;
	private static final double THRESHOLD = 30.0;
	private static final boolean CACHED = true;
	
	private static final int PICK_FROM_CAMERA = 0;
	private static final int PICK_FROM_ALBUM = 1;
	private static final int CROP_FROM_CAMERA = 2;
	
	private Mat mSceneDescriptors = null;
	
	private ImageView mSample = null;
	private ImageView mTarget = null;
	
	private Uri mImageCaptureUri;
	private ImageView mPhotoImageView;
	
	private Button mBtnStart;
	private Button mBtnYoutube;
	
	private Bitmap mImgFromCamera;
	private ProgressBar mProgress;
	private AsyncTask<Void, Integer, Void> mTask;

	private static int [] mResources = new int[]{
		R.drawable.disney_sample_1_portrait,
		R.drawable.disney_sample_2_portrait,
		R.drawable.disney_sample_3_portrait,
		R.drawable.disney_sample_4_portrait,

		R.drawable.disney_sample_1_landscape_80,
		R.drawable.disney_sample_2_landscape_80,
		R.drawable.disney_sample_3_landscape_80,
		R.drawable.disney_sample_4_landscape_80,
		
		R.drawable.disney_sample_1_portrait_50,
		R.drawable.disney_sample_2_portrait_50,
		R.drawable.disney_sample_3_portrait_50,
		R.drawable.disney_sample_4_portrait_50,
		
		R.drawable.goto_movie
	};
	
	private static int [] mResources2 = new int[]{
		R.drawable.disney_sample_1_landscape,
		R.drawable.disney_sample_2_landscape,
		R.drawable.disney_sample_3_landscape,
		R.drawable.disney_sample_4_landscape,
		
		R.drawable.disney_sample_1_landscape_80,
		R.drawable.disney_sample_2_landscape_80,
		R.drawable.disney_sample_3_landscape_80,
		R.drawable.disney_sample_4_landscape_80,
		
		R.drawable.disney_sample_1_landscape_50,
		R.drawable.disney_sample_2_landscape_50,
		R.drawable.disney_sample_3_landscape_50,
		R.drawable.disney_sample_4_landscape_50,
		
		R.drawable.goto_movie
	};
	
   	private int nMaxMatchNdx = 0;
   	private double nMaxMatchRate = 0.0f;
   	
    private org.opencv.android.BaseLoaderCallback mLoaderCallback = 
    		new org.opencv.android.BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    
                    mBtnStart.setEnabled(true);
                    
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		mBtnStart = (Button)findViewById(R.id.start);
		mBtnYoutube = (Button)findViewById(R.id.youtube);

        mSample = (ImageView)findViewById(R.id.sample);
		mTarget = (ImageView)findViewById(R.id.crop);
		mProgress = (ProgressBar)findViewById(R.id.progress);
		
		mBtnStart.setOnClickListener(this);
		mBtnYoutube.setOnClickListener(this);

		
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback))
	    {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
	    }
		
		Intent intent = new Intent(MainActivity.this,PlayerViewDemoActivity.class);
		intent.putExtra("video_uri", "3dnxG6fxXi8");
    	startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		final int mode = requestCode;
		
		if(resultCode != RESULT_OK){
			return;
	    }

	    switch(requestCode){
	      	case CROP_FROM_CAMERA:{
	      		// 크롭이 된 이후의 이미지를 넘겨 받습니다.
	      		// 이미지뷰에 이미지를 보여준다거나 부가적인 작업 이후에
		        // 임시 파일을 삭제합니다.
		        final Bundle extras = data.getExtras();
		  
		        if(extras != null)
		        {
		        	Bitmap photo = extras.getParcelable("data");
		        	mPhotoImageView.setImageBitmap(photo);
		        }
		  
		        // 임시 파일 삭제
		        File f = new File(mImageCaptureUri.getPath());
		        if(f.exists())
		        {
		        	f.delete();
		        }
		  
		        break;
	      	}
	  
	      	case PICK_FROM_ALBUM:{
		        // 이후의 처리가 카메라와 같으므로 일단  break없이 진행합니다.
		        // 실제 코드에서는 좀더 합리적인 방법을 선택하시기 바랍니다.
		        
		        mImageCaptureUri = data.getData();
		        
		        
	      	}
	      
	      	case PICK_FROM_CAMERA:{
		        // 이미지를 가져온 이후의 리사이즈할 이미지 크기를 결정합니다.
		        // 이후에 이미지 크롭 어플리케이션을 호출하게 됩니다.
	      		
	      		mImgFromCamera = scaleAndTrun(MainActivity.this.grabImage(mode));
  	    		mSample.setImageBitmap(mImgFromCamera);
	      		
	      		// AsyncTask는 재활용할 수 없습니다. 매번 새롭게 생성
	      	    mTask = new AsyncTask<Void, Integer, Void>()
	      	    {
	      	    	// 작업 취소시 사용하기 위한 플래그
	      	    	private boolean isCanceled = false;
	      	      
	      	    	// 작업을 시작하기 직전에 호출되는 메서드
	      	    	@Override
	      	    	protected void onPreExecute(){
	      	    		publishProgress(0);
	      	    		isCanceled = false;
	      	    		nMaxMatchNdx = 0;
	      	    	   	nMaxMatchRate = 0;
	      	    	}
	      	      
	      	    	// 백그라운드에서 작업
	      	    	@Override
	      	    	protected Void doInBackground(Void... params){
	      	    		
	      	    		
	      	     	    
		      	  		boolean isPortrait = true;
		      			int [] res = mResources;
		      			
		      			if(mImgFromCamera.getWidth() > mImgFromCamera.getHeight()){
		      				isPortrait = false;
		      				res = mResources2;
		      			}
	      	    		
	      	     	    Mat src = new Mat();
	      	         	Mat target = new Mat();
	      	         	
	      	         	BitmapFactory.Options options = new BitmapFactory.Options();
	      	         	options.inPreferredConfig = Config.ARGB_8888;
	      	         	Utils.bitmapToMat(mImgFromCamera, src);
	      	         	
	      	         	mSceneDescriptors = null;
	      	         	
	      	         	Log.v(TAG,"-------------START----------------");
	      	         	
	      	         	for(int i = 0 ;i < res.length ; i++){
		      	  	       	Bitmap input2 = scaleAndTrun(BitmapFactory.decodeResource(getResources(), res[i]));
		      	  	       	Utils.bitmapToMat(input2, target);
		      	  	       	
		      	  	       	Mat matScene = getSecenDescriptor(src);
		      	  	       	Mat matTrain = getTrainDescriptor(target,i,isPortrait);
		      	  	       	double nMatchRate = match(matScene,matTrain);
		      	  	       	
		      	  	       	Log.v(TAG, "i="+i+",rate="+nMatchRate);
		      	  	       	
		      	  	       	if(nMaxMatchRate <= nMatchRate){
		      	  	       		nMaxMatchRate = nMatchRate;
		      	  	       		nMaxMatchNdx = i;
		      	  	       	}
		      	  	       	
		      	  	       	publishProgress((int)((1.0*100/res.length)*(i+1)));
		      	  	       	
			      	  	    if(nMaxMatchRate > THRESHOLD){
			      	       		break;
			      	       	}
	      	         	}
	      	    		
	      	    		
	      	    		return null;
	      	    	}

	      	    	// publishProgress() 메서드를 통해 호출됩니다. 진행사항을 표시하는데에 쓰입니다.
	      	    	@Override
	      	    	protected void onProgressUpdate(Integer... progress){
	      	    		if(progress[0] == 0){
	      	    			mProgress.setVisibility(View.VISIBLE);
	      	    		}
	      	    		mProgress.setProgress(progress[0]);
	      	    	}
	      	      
	      	    	// 작업 완료 직후에 호출되는 메소드
	      	    	@Override
	      	    	protected void onPostExecute(Void result){
	      	    		//Toast.makeText(MainActivity.this, "완료됨", Toast.LENGTH_SHORT).show();
	      	    		//mButton.setText("start");
	      	    		publishProgress(100);
	      	    		
	      	    		TextView tv = (TextView)findViewById(R.id.txt_desc);
	      	    		tv.setText("Max Matching Rate : "+nMaxMatchRate+"%");
	      	     		//mSample.setImageBitmap(mImgFromCamera);
	      	     		
	      	     		//mTarget.setImageResource(mResources[nMaxMatchNdx]);
	      	     		Bitmap input2 = scaleAndTrun(BitmapFactory.decodeResource(getResources(), mResources[nMaxMatchNdx]));
	      	     		mTarget.setImageBitmap(input2);
	      	     		
	      	     		mProgress.setVisibility(View.GONE);
	      	     		
	      	     		if(nMaxMatchNdx == 12){	
	      	     			Intent intent = new Intent(MainActivity.this,PlayerViewDemoActivity.class);
	      	     			intent.putExtra("video_uri", "ZRlCulV7r-I");
	      	     	    	startActivity(intent);
	      	     	    	
	      	     		}
	      	    	}
	      	      
	      	    	// 외부에서 강제로 취소할때 호출되는 메소드
	      	    	@Override
	      	    	protected void onCancelled(){
	      	    		isCanceled = true;
	      	    		publishProgress(0);
	      	    		Toast.makeText(MainActivity.this, "취소됨", Toast.LENGTH_SHORT).show();
	      	    	}
	      	    };
	      	    
	      	    // 작업 시작
	      	    mTask.execute();
	      		
	      		
	      		
		        break;
	      	}
	    }
	}

	public Bitmap grabImage(int mode)
	{
	    this.getContentResolver().notifyChange(mImageCaptureUri, null);
	    ContentResolver cr = this.getContentResolver();
	    Bitmap bitmap = null;
	    try
	    {
	    	//mImageCaptureUri.get
	    	switch(mode){
	    	case PICK_FROM_CAMERA:
	    		bitmap = CameraImageUtil.SafeDecodeBitmapFile(mImageCaptureUri.getPath());
	    		break;
	    	case PICK_FROM_ALBUM:
	    		bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, mImageCaptureUri);
	    		break;
	    	}
	        
	        return bitmap;
	    }
	    catch (Exception e)
	    {
	        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
	        Log.d(TAG, "Failed to load", e);
	    }
	    
	    return null;
	}
	
	private Mat getSecenDescriptor(Mat srcMat){
		if(mSceneDescriptors == null){
			FeatureDetector detector = FeatureDetector.create(FeatureDetector.FAST);
	        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.BRIEF);
			
	        Mat srcImage = new Mat();
	        Mat quSrcImage = new Mat();
	        srcMat.copyTo(quSrcImage);
	        
	        Imgproc.cvtColor(quSrcImage, srcImage, Imgproc.COLOR_RGBA2RGB,3);
	        MatOfKeyPoint vectorSrc = new MatOfKeyPoint();
	        detector.detect(srcImage, vectorSrc );
	        
	        Mat sceneDescriptors = new Mat();
	        extractor.compute( srcImage, vectorSrc, sceneDescriptors );
	        
	        mSceneDescriptors = sceneDescriptors;
        }
		
		return mSceneDescriptors;
	}
	
	private Mat getTrainDescriptor(Mat targetMat,int i,boolean isPortrait){
		//Load From File.
		String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
		
		File dir = new File(extStorageDirectory, "tcs");
		if(!dir.exists()){
			dir.mkdir();
		}
		
		File file = null;
		if(!isPortrait){
			file = new File(extStorageDirectory, "tcs/disney_mat_"+i+"_landscape.txt");
		}else{
			file = new File(extStorageDirectory, "tcs/disney_mat_"+i+"_portrait.txt");
		}
		
		if(CACHED){
			try{
				if(file.exists() && file.length() > 0){
					
					//Log.v(TAG,"use cached file.");
					
					FileInputStream fIn = new FileInputStream(file);
			        InputStreamReader isr = new InputStreamReader(fIn);
			        
			        char[] inputBuffer = new char[(int) file.length()];
			        isr.read(inputBuffer);
			        isr.close();
			        
			        String data = new String(inputBuffer);
			        
			        String base64="";
			        int type,cols,rows;
			        
			        String [] raw = data.split("\t");
			        
			        if(raw.length == 4){
			        	rows = Integer.parseInt(raw[0]);
			        	cols = Integer.parseInt(raw[1]);
			        	type = Integer.parseInt(raw[2]);
			        	base64 = raw[3];
			        	
				        byte [] buff  = Base64.decode(base64, Base64.DEFAULT);
				        Mat trainDescriptors = new Mat(rows,cols,type);
				        trainDescriptors.put(0, 0, buff);
				        
				        return trainDescriptors;
			        }
				}
		    }catch(IOException e){
		    	
		    }
		}
		FeatureDetector detector = FeatureDetector.create(FeatureDetector.FAST);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.BRIEF);
        
    	//Target - Train-------------------------------------------------------
        Mat targetImage = new Mat();
        Mat quTargetImage = new Mat();
        targetMat.copyTo(quTargetImage);
        
        Imgproc.cvtColor(quTargetImage, targetImage, Imgproc.COLOR_RGBA2RGB,3);
        MatOfKeyPoint vectorTarget = new MatOfKeyPoint();
        detector.detect(targetImage, vectorTarget );
        
        Mat trainDescriptors = new Mat();
        extractor.compute( targetImage, vectorTarget, trainDescriptors );
        
        if(CACHED){
	        int count = (int) (trainDescriptors.total() * trainDescriptors.channels());
		    byte[] buff = new byte[count];
		    trainDescriptors.get(0, 0, buff);
		    String base64 = Base64.encodeToString(buff, Base64.DEFAULT);
		    
		    try{
				//String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
				OutputStream outStream = null;
				//File file = new File(extStorageDirectory, "mat_"+i+".txt");
			    outStream = new FileOutputStream(file);
			    OutputStreamWriter osw = new OutputStreamWriter(outStream); 
			    
			    int type = trainDescriptors.type();
			    int cols = trainDescriptors.cols();
			    int rows = trainDescriptors.rows();
			    
			    String data = ""+rows+"\t"+cols+"\t"+type+"\t"+base64;
			    osw.write(data);
			    
			    osw.flush();
			    osw.close();		    
		    }catch(IOException e){
		    	
		    }
        }
        return trainDescriptors;
	}
	
	private double match(Mat matScene, Mat matTrain){
		MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher matcherHamming = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
        List<Mat> listMat = new ArrayList<Mat>();
        listMat.add(matTrain);
        matcherHamming.add(listMat);
        matcherHamming.train();
                
        matcherHamming.match(matScene, matches); 
        
		List<DMatch> good_matches = new ArrayList<DMatch>();
		
    	List<DMatch> in_matches = matches.toList();
		int rowCount = in_matches.size();
		
		for (int i = 0; i < rowCount; i++) {
			if (in_matches.get(i).distance <= BOUNDARY) {
				good_matches.add(in_matches.get(i));
			}
		}        
        		
		return (1.0  * good_matches.size() / rowCount) * 100.0;
	}
	
    private Bitmap scaleAndTrun(Bitmap bm) {
		int MAX_DIM = 200;
		int w, h;
		if (bm.getWidth() >= bm.getHeight()) {
			w = MAX_DIM;
			h = bm.getHeight() * MAX_DIM / bm.getWidth();
		} else {
			h = MAX_DIM;
			w = bm.getWidth() * MAX_DIM / bm.getHeight();
		}
		bm = Bitmap.createScaledBitmap(bm, w, h, false);
		Bitmap img_bit = bm.copy(Bitmap.Config.ARGB_8888, false);
		return img_bit;
	}
	
    
    /**
     * 카메라에서 이미지 가져오기
     */
    private void doTakePhotoAction()
    {

      Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      
      // 임시로 사용할 파일의 경로를 생성
      String url = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
      mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));
      
      intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
      // 특정기기에서 사진을 저장못하는 문제가 있어 다음을 주석처리 합니다.
      //intent.putExtra("return-data", true);
      startActivityForResult(intent, PICK_FROM_CAMERA);
    }
    
    /**
     * 앨범에서 이미지 가져오기
     */
    private void doTakeAlbumAction()
    {
      // 앨범 호출
      Intent intent = new Intent(Intent.ACTION_PICK);
      intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
      startActivityForResult(intent, PICK_FROM_ALBUM);
    }
    
    @Override
    public void onClick(View v)
    {
    	if(v.getId() == R.id.start){
	    	DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener(){
	    		@Override
	    		public void onClick(DialogInterface dialog, int which){
	    			doTakePhotoAction();
	    		}	
	    	};
	      
	    	DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener(){
	    		@Override
	    		public void onClick(DialogInterface dialog, int which){
	    			doTakeAlbumAction();
	    		}
	    	};
	      
	    	DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener(){
	    		@Override
	    		public void onClick(DialogInterface dialog, int which){
	    			dialog.dismiss();
	    		}
	    	};
	      
	    	new AlertDialog.Builder(this)
	        	.setTitle("Choose Image")
	        	.setPositiveButton("TakePhoto", cameraListener)
	        	.setNeutralButton("Album", albumListener)
	        	.setNegativeButton("Cancel", cancelListener)
	        	.show();
	    	
    	}else if(v.getId() == R.id.youtube){
        	
        	Intent intent = new Intent(MainActivity.this,PlayerViewDemoActivity.class);
        	intent.putExtra("video_uri", "3dnxG6fxXi8");
        	startActivity(intent);
    		
        }
    }
}
