package download.jack.com.mydownload;

import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private EditText et;
    private String urlPath;
    private int threadCount = 3;
    private Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et = (EditText) findViewById(R.id.et);
    }

    public void clickDownloadButton(View view) {
        urlPath = et.getText().toString().trim();
        if(TextUtils.isEmpty(urlPath)) {
            Toast.makeText(this,"网址不能为空",Toast.LENGTH_SHORT).show();
            return;
        }
        //开启线程下载,获取文件的长度
        startDownloadThread(urlPath);
    }

    private void startDownloadThread(final String urlPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlPath);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    int responseCode = conn.getResponseCode();
                    if(responseCode==200) {
                        int contentLength = conn.getContentLength();
                        //创建同样大小的空文件
                        File file = new File(Environment.getExternalStorageDirectory()+File.separator+getSaveName(urlPath));
                        RandomAccessFile randomFile = new RandomAccessFile(file,"rw");
                        randomFile.setLength(contentLength);
                        //开启线程开始下载
                        int blockSize = contentLength/threadCount;
                        for (int threadId = 0; threadId < threadCount; threadId++) {
                            int startIndex = threadId*blockSize;
                            int endIndex = (threadId + 1)*blockSize - 1;
                            if(threadId == threadCount - 1) {
                                endIndex = contentLength - 1;
                            }
                            new DownloadThread(startIndex,endIndex,urlPath,threadId).start();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    class DownloadThread extends Thread {
        private int startIndex;
        private int endIndex;
        private String urlPath;
        private int threadId;

        DownloadThread(int startIndex,int endIndex,String urlPath,int threadId) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.urlPath = urlPath;
            this.threadId = threadId;
        }
        @Override
        public void run() {
            URL url = null;
            try {
                url = new URL(urlPath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("range", "bytes=" + startIndex+ "-" + endIndex);
                int responseCode = conn.getResponseCode();
                if(responseCode == 206) {
                    //向文件中写入数据
                    InputStream in = conn.getInputStream();
                    File file = new File(Environment.getExternalStorageDirectory()+File.separator+getSaveName(urlPath));
                    RandomAccessFile randomFile = new RandomAccessFile(file,"rw");
                    randomFile.seek(startIndex);
                    byte[] bytes = streamToByte(in);
                    randomFile.write(bytes);
                    in.close();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,threadId+"下载完了",Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //将流转换为字节
    public byte[] streamToByte(InputStream in) throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] by = new byte[1024];
        int len = 0;
        while((len = in.read(by))>0) {
            out.write(by,0,len);
            out.flush();
        }
        byte[] bytes = out.toByteArray();
        out.close();
        return bytes;
    }

    //获取保存的文件名字
    private String getSaveName(String urlPath) {
        int lastIndexOf = urlPath.lastIndexOf("/");
        return urlPath.substring(lastIndexOf + 1);
    }
}
