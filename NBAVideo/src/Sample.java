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

//�������ԭ�򣬸ó���û��ʹ��һ���Ƚ�������㷨
//��Ȼ������������׼ȷ�ʺ��ٻ���90%�����ܻ���һЩ������֣������������ٶȽ���
//�пջ���취�Ľ�

public class Sample {
    //����APPID/AK/SK
    public static final String APP_ID = "���AppID";//AppID
    public static final String API_KEY = "���API Key";//API Key
    public static final String SECRET_KEY = "���Secret Key";//Secret Key
    public static double pre_time;
    public static int cnt=0;
     
    //�ж��ַ����Ƿ�Ϊ����
    public static boolean isNumber(String str) {
        Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }
    //��ȡ��ǰ֡�ı���ʣ��ʱ��
    public static double check(Mat frame) {
    	/*
    	 * ���Ҫɾ����ʱ����
    	 * ѡ���ȡ���������ڴε�����
    	 * ���ðٶ�����ʶ��API����ʶ��
    	 * ����õ����ַ���Ϊ"OT"����ֹͣ��Ƶ��ȡд�롣
    	 */
    	//��ȡʣ�����ʱ������
    	int height = frame.rows();
        int width  = frame.cols();
        Rect rect = new Rect((int)width*15/31,(int)height*13/15,(int)width/20,(int)height/9);
        Mat roi_img = new Mat(frame,rect);
        Imgcodecs.imwrite("D:\\img\\"+cnt+".jpg",roi_img);
        
        //���ðٶ�API��������ʶ��
    	//��ʼ��һ��AipOcr
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
    	//����Ƶ�ļ�
    	VideoCapture cap = new VideoCapture("read.avi");
    	pre_time=0.0;
    	if(cap.isOpened()){//�ж���Ƶ�Ƿ��
	    	//��֡��  
	    	double frameCount = cap.get(opencv_videoio.CV_CAP_PROP_FRAME_COUNT);
	    	//System.out.println(frameCount);
	    	
	    	//֡��  
	    	double fpsx = cap.get(opencv_videoio.CV_CAP_PROP_FPS);  
	    	int fps=(int)(fpsx+0.5);
	    	//System.out.println(fps);
	    	
	    	//ʱ�䳤��  
	    	double len = frameCount / fps;
	    	//System.out.println(len);
	    	Double d_s=new Double(len);
	    	//System.out.println(d_s.intValue());
	    	
	    	//д����Ƶ
	    	Size sz=new Size(cap.get(opencv_videoio.CV_CAP_PROP_FRAME_WIDTH),cap.get(opencv_videoio.CV_CAP_PROP_FRAME_HEIGHT));
	    	VideoWriter writer = new VideoWriter("write.avi",VideoWriter.fourcc('D','I','V','X') , fpsx , sz , true);
	    	
	    	Mat frame = new Mat();
	    	boolean fg=false;
	    	for(int i=1;i<=frameCount;i++){
	    		//��ȡ��һ֡����  
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
    	//�ر���Ƶ�ļ�  
    	cap.release();  
    }
}