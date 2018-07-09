package gdut.bsx.tensorflowtraining.download;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


//input：uri listener context
public class DownLoadManager {

    private final OkHttpClient mClient;
    private ArrayList<DownEntity> mTasks;

    private  static  DownLoadManager instance;
    private DownLoadManager(){
        mClient = new OkHttpClient();
        mTasks= new ArrayList<>();
    }
    public static DownLoadManager getInstance(){
        if(instance==null){
            synchronized (DownLoadManager.class){
                if(instance==null){
                    instance=new DownLoadManager();
                }
            }
        }
        return instance;
    }


    public DownLoadManager addTasks(ArrayList<DownEntity> tasks) {
        mTasks.clear();
        mTasks.addAll(tasks);
        return this;
    }

    public void startDownLoad(){
        for (int i = 0; i <mTasks.size(); i++) {
            downLoad(mTasks.get(i));
        }
    }

    private void downLoad(final DownEntity entity) {
        Request request = new Request.Builder().url(entity.uri).build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                entity.listener.onDownloadFailed();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                String savePath = isExistDir(entity.des);
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    File file = new File(savePath, entity.fileName);
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        entity.listener.onDownloading(progress);
                    }
                    fos.flush();
                    // 下载完成
                    entity.listener.onDownloadSuccess();
                } catch (Exception e) {
                    entity.listener.onDownloadFailed();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });


    }
    private String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }
}
