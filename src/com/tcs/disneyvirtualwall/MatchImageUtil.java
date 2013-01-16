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

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import com.tcs.disneyvirtualwall.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

public class MatchImageUtil {
	private static final String TAG = "CAMERA::Activity";
	private static final boolean D = false;
	private static final int BOUNDARY = 35;
	private static final double THRESHOLD = 30.0;
	private static final boolean CACHED = true;
	
	private static Mat mSceneDescriptors = null;
	
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
		R.drawable.disney_sample_4_portrait_50
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
		R.drawable.disney_sample_4_landscape_50
	};
	
	private Context mctx = null;
	
	public MatchImageUtil(Context ctx){
		mctx = ctx;
	}
	
	public int findObject(Mat secenMat){
		
		boolean isPortrait = false;
		int [] res = mResources2;
		
		Mat src = new Mat();
    	Mat target = new Mat();
    	
    	Bitmap sceneImg = Bitmap.createBitmap( secenMat.cols(), secenMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(secenMat,sceneImg);
    	
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inPreferredConfig = Config.ARGB_8888;
    	Utils.bitmapToMat(sceneImg, src);
    	
    	mSceneDescriptors = null;
    	
    	Log.v(TAG,"-------------START----------------");
    	
    	long startTime = (new Date()).getTime();
    	
    	double maxRate = 0.0;
    	int maxNdx = 0;
    	
    	for(int i = 0 ;i < res.length ; i++){
  	       	Bitmap input2 = scaleAndTrun(BitmapFactory.decodeResource(mctx.getResources(), res[i]));
  	       	Utils.bitmapToMat(input2, target);
  	       	
  	       	Mat matScene = getSecenDescriptor(src);
  	       	Mat matTrain = getTrainDescriptor(target,i,isPortrait);
  	       	double nMatchRate = match(matScene,matTrain);
  	       	
  	       	Log.v(TAG, "i="+i+",rate="+nMatchRate);
  	       	
  	       	if(maxRate <= nMatchRate){
  	       		maxRate = nMatchRate;
  	       		maxNdx = i;
  	       	}
  	       	
  	       	if(maxRate > THRESHOLD){
  	       		break;
  	       	}
    	}
    	
    	long endTime = (new Date()).getTime();
    	
    	Log.v(TAG,"execute time = "+(endTime-startTime));
    	
    	return ( maxRate > THRESHOLD ? maxNdx : -1 ) ;
		
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
}
