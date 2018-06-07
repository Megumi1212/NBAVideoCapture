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

//由于诸多原因，该程序并没有使用一个比较优秀的算法
//虽然基本可以满足准确率和召回率90%（可能会有一些情况出现错误），不过运行速度较慢
//有空会想办法改进

public class Sample {
    //设置APPID/AK/SK
    public static final String APP_ID = "你的AppID";//AppID
    public static final String API_KEY = "你的API Key";//API Key
    public static final String SECRET_KEY = "你的Secret Key";//Secret Key
    public static double pre_time;
    public static int cnt=0;
     
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
    
	public static void main(String[] args) throws InterruptedException {
    	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    	//打开视频文件
    	VideoCapture cap = new VideoCapture("read.avi");
    	pre_time=0.0;
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
	    	Size sz=new Size(cap.get(opencv_videoio.CV_CAP_PROP_FRAME_WIDTH),cap.get(opencv_videoio.CV_CAP_PROP_FRAME_HEIGHT));
	    	VideoWriter writer = new VideoWriter("write.avi",VideoWriter.fourcc('D','I','V','X') , fpsx , sz , true);
	    	
	    	Mat frame = new Mat();
	    	boolean fg=false;
	    	for(int i=1;i<=frameCount;i++){
	    		//读取下一帧画面  
	    		if(cap.read(frame)){  
	    			if(fg==true) writer.write(frame);
	    			if(i%fps==0) {
	    				double now_time=check(frame);
	    				//if(now_time<-1) break;
	    				if(now_time<0) fg=false;
	    				else {
	    					if(now_time!=pre_time) fg=true;
	    					else fg=false;
	    				}
	    				if(now_time>0) pre_time=now_time;
	    			}
	    		}
	    	}
    	}else {
    		System.out.println("Open failed!");
    	}
    	//关闭视频文件  
    	cap.release();  
    }
}
