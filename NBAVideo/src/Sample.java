import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bytedeco.javacpp.opencv_videoio;
import org.json.JSONObject;

import com.baidu.aip.ocr.AipOcr;  

import org.opencv.core.Core;  
import org.opencv.core.Mat;  
import org.opencv.core.Rect;  
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;  
import org.opencv.videoio.VideoWriter;
import org.opencv.core.Size;

//之前的代码对一个5分20秒左右的视频操作，大概需要240秒左右
//现在的代码只需要30-40秒

//改进的地方如下：
//增大了步长
//用ffmpeg截取视频来替代一张张写入视频
//由于用ffmpeg截取视频，因此视频也不需要遍历，可以根据步长直接跳到需要检测的那一帧

//出现的问题：
//用ffmpeg截取视频后，再合并截取的视频，会导致只有第一段视频能看，后面部分无效。
//根据网上利用ffmpeg合并视频，则没有结果文件产生

//增大步长对准确率和召回率的影响
//之前的程序步长为1s，现在是6s
//影响准确率和召回率主要是广告、罚球和比赛的交界处
//以广告和比赛的交界为例，即前面所取的一帧为比赛时，而后一帧为广告
//那么实际比赛和广告的交界处于这两帧之间，、
//如果选择这两帧的某一帧作为一段比赛开始/结束，那么与实际交界的时间差的期望值 为： 这两帧时间差的一半
//而选择这两帧的中间位置，则与实际交界的时间差的期望值 为： 这两帧时间差的四分之一
//所以，采用中间位置作为开始/结束，6s的步长，每次广告、罚球和比赛的交界产生的误差平均为1.5s
//基本可以满足所要求的准确率和召回率

//其他改进的地方
//可以使用本地OCR程序
//对于截取比赛时长区域，不同的视频所在的区域可能不同，而此程序仅仅根据所用的测试视频，选择的比赛时长区域
//可以利用百度OCR含位置信息版，随机抽取NBA视频的某几帧，来判断视频比赛时长区域位置

//个人感想
//这几次的作业，我学到了很多新的知识，收获也很大
//不过由于不够努力，所花时间也不多，所以最后的结果不怎么样
//有空会想办法继续改进

public class Sample {
    //设置APPID/AK/SK
    public static final String APP_ID = "AppID";//AppID
    public static final String API_KEY = "API Key";//API Key
    public static final String SECRET_KEY = "Secret Key";//Secret Key
    public static double pre_time;
    public static int cnt=0;
    
    private final static String INPUTPATH = "D:\\java\\NBAVideo\\read.avi"; 
    private final static String OUTPATH = "D:\\video\\";
    private final static String FFMPEGPATH = "C:\\ffmpeg\\bin\\ffmpeg.exe  "; 
    public static int cnt1=0;
    public static double a[][]=new double[555][2];
    
    //截取视频
    public static void solve() throws IOException{
    	for(int i=0;i<cnt1;i++){           
        	Runtime runtime = Runtime.getRuntime();  
            String cut = FFMPEGPATH+" -i "+INPUTPATH+" -vcodec copy -acodec copy -ss " 
                 + a[i][0]+" -to "+a[i][1]+" "+OUTPATH+i+ ".mp4 -y";
            runtime.exec(cut); 
    	}    
        String filenameTemp ="D:\\filelist.txt";
           File filename = new File(filenameTemp);
           if (!filename.exists()) {
			   filename.createNewFile();
           }
           FileOutputStream o=null;  
           o = new FileOutputStream(filename);  
		   for(int i=0;i<cnt1;i++) {
		   o.write(("file \'D:\\video\\"+i+".mp4\'\r\n").getBytes("GBK"));
        }
        o.close();  
    }
    //合并视频
    public static void union(String dirPath, String toFilePath) throws IOException {
    	Runtime runtime = Runtime.getRuntime();  
        String cut = FFMPEGPATH+" -f concat -i "+"D:\\filelist.txt"
             +" -c copy "+toFilePath;
        runtime.exec(cut); 
    	/*
    	File dir = new File(dirPath);
        if (!dir.exists()) return;
        File videoPartArr[] = dir.listFiles();
        if (videoPartArr.length == 0) return;

        File combineFile = new File(toFilePath);
        FileOutputStream writer = new FileOutputStream(combineFile);
        byte buffer[] = new byte[10240];
        for (int i=0;i<videoPartArr.length;i++) {
	        FileInputStream reader = new FileInputStream(videoPartArr[i]);
	        int sz=0;
	        while ((sz=reader.read(buffer))!= -1) {
	             writer.write(buffer, 0, sz);
	        }
        	reader.close();
        }
        writer.close();
        */
    }
    
    //判断字符串是否为数字
    public static boolean isNumber(String str) {
        Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }
    //获取当前帧的比赛剩余时间
    public static double check(Mat frame) {
    	/*
    	 * 如果要删除加时部分
    	 * 选择截取包含比赛节次的区域
    	 * 利用百度文字识别API进行识别
    	 * 如果得到的字符串为"OT"，则停止视频读取写入。
    	 */
    	//截取剩余比赛时长区域
    	int height = frame.rows();
        int width  = frame.cols();
        Rect rect = new Rect((int)width*15/31,(int)height*13/15,(int)width/20,(int)height/9);
        Mat roi_img = new Mat(frame,rect);
        Imgcodecs.imwrite("D:\\img\\"+cnt+".jpg",roi_img);
        
        //利用百度API进行文字识别
    	//初始化一个AipOcr
    	AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
        String path = "D:\\img\\"+cnt+".jpg";
        cnt++;
        JSONObject res = client.basicGeneral(path, new HashMap<String, String>());
       // System.out.println(res.toString(2));
        JSONObject json=new JSONObject(res.toString(2));
        int num=Integer.valueOf(json.get("words_result_num").toString());
        double cur_time;
        if(num==0) {
        	cur_time=-1;
        }else {
        	String string=(String) json.getJSONArray("words_result").getJSONObject(0).get("words");
        	//System.out.println(string);
        	if(string.contains(":")) {
        		String temp[]=string.split(":");
        		if(isNumber(temp[0])&&isNumber(temp[1])) cur_time=1.0*Double.parseDouble(temp[0])*60+Double.parseDouble(temp[1]);
        		else cur_time=-1;
        	}else {
        		if(isNumber(string)) cur_time=Double.parseDouble(string);
        		else cur_time=-1;
        	}
        }
        return cur_time;
    }
    
	public static void main(String[] args) throws InterruptedException, IOException {
    	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    	//打开视频文件
    	//long startTime=System.currentTimeMillis();   //获取开始时间
    	
    	VideoCapture cap = new VideoCapture("reed.mp4");
    	pre_time=9990.0;
    	if(cap.isOpened()){//判断视频是否打开
	    	//总帧数  
	    	double frameCount = cap.get(opencv_videoio.CV_CAP_PROP_FRAME_COUNT);
	    	//System.out.println(frameCount);
	    	
	    	//帧率  
	    	double fpsx = cap.get(opencv_videoio.CV_CAP_PROP_FPS);  
	    	int fps=(int)(fpsx+0.5);
	    	//System.out.println(fps);
	    	
	    	//时间长度  
	    	double len = frameCount / fps;
	    	//System.out.println(len);
	    	Double d_s=new Double(len);
	    	//System.out.println(d_s.intValue());
	    	
	    	//写入视频
	    	//Size sz=new Size(cap.get(opencv_videoio.CV_CAP_PROP_FRAME_WIDTH),cap.get(opencv_videoio.CV_CAP_PROP_FRAME_HEIGHT));
	    	//VideoWriter writer = new VideoWriter("write.avi",VideoWriter.fourcc('D','I','V','X') , fpsx , sz , true);
	    	
	    	Mat frame = new Mat();
	    	boolean fg=false;
	    	for(int i=1;i<=frameCount/(6*fps);i++){
	    		cap.set(opencv_videoio.CV_CAP_PROP_POS_MSEC ,i*6000);  
	    		if(cap.read(frame)){  
	    			//if(fg==true) writer.write(frame);
	    			double now_time=check(frame);
	    			//if(now_time<-1) break;
	    			if(now_time<0) {//当前帧为广告
	    				if(fg==true) {//如果之前为比赛时间，那么记录结束时间
	    					a[cnt1++][1]=i*6-3;
	    				}
	    				fg=false;
	    			}else {
	    				if(now_time==pre_time){
	    					if(fg==true) {
	    						a[cnt1++][1]=i*6-9;
	    					}
	    					fg=false;
	    				}else{
	    					if(fg==false) {
		    					a[cnt1][0]=i*6-3;
	    					}
	    					fg=true;		
	    				}
	    			}
	    			if(now_time>0) pre_time=now_time;
	    		}
	    	}
	    	solve();
	    	union("D:\\video","D:\\java\\NBAVideo\\write.mp4");
    	}else {
    		System.out.println("Open failed!");
    	}
    	//关闭视频文件  
    	cap.release();
    	
    	//long endTime=System.currentTimeMillis(); //获取结束时间
    	//System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
    }
}
