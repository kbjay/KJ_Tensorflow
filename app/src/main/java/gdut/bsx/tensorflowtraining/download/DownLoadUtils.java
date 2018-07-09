package gdut.bsx.tensorflowtraining.download;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.DOWNLOAD_SERVICE;

public class DownLoadUtils {
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();;
    public static List<Download> mList = new ArrayList<>();
    private static DownloadManager downloadManager;
    private Context mContext;
    private OnDownLoad mListener;

    public void initData(Context context) {
        mContext = context;
        for (int i = 0; i < urls.models.length; i++) {
            Download download = new Download();
            download.setDownLoadUrl(urls.models[i]);
            download.setFileName(urls.filename[i]);
            mList.add(download);
        }
    }
    public void downLoad()
    {
        for(int i = 0; i < mList.size(); i++)
            cachedThreadPool.execute(new DownLoadTask(mList.get(i), mContext));
    }
    public class DownLoadTask implements Runnable {
        private Download downLoad;

        public DownLoadTask(Download download, Context context) {
            this.downLoad = download;
            mContext = context;
        }

        @Override
        public void run() {
            downLoadFile(downLoad);
        }

        private void downLoadFile(Download downLoad) {
            //1.得到下载对象
            downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
            //2.创建下载请求对象，并且把下载的地址放进去
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downLoad.getDownLoadUrl()));
           // request.setDestinationInExternalPublicDir(MainActivity.MODEL_PATH,downLoad.getFileName());
            //3.给下载的文件指定路径
            request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, downLoad.getFileName());
            //4.设置显示在文件下载Notification（通知栏）中显示的文字。6.0的手机Description不显示
            request.setTitle(downLoad.getTitle());
            request.setDescription(downLoad.getDescription());

            //6.设置在什么连接状态下执行下载操作
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
            //7. 设置为可被媒体扫描器找到
            request.allowScanningByMediaScanner();
            //8. 设置为可见和可管理
            request.setVisibleInDownloadsUi(true);

            long lastDownloadId = downloadManager.enqueue(request);
            downLoad.setLastDownLoadId(lastDownloadId);
            //注册广播接收者，监听下载状态
            mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            for(int i = 0; i < mList.size(); i++){
                checkStatus(mList.get(i));
            }
        }
    };
    //检查下载状态
    private void checkStatus(Download download) {
        DownloadManager.Query query = new DownloadManager.Query();
        //通过下载的id查找
        query.setFilterById(download.getLastDownLoadId());
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            Log.d("kb_jay","status="+status);
            switch (status) {

                //下载暂停
                case DownloadManager.STATUS_PAUSED: break;
                //下载延迟
                case DownloadManager.STATUS_PENDING: break;
                //正在下载
                case DownloadManager.STATUS_RUNNING: break;
                //下载完成
                case DownloadManager.STATUS_SUCCESSFUL:
                    //下载完成
                    if(mListener!=null){
                        mListener.onSuccess();
                    }
                    break;
                //下载失败
                case DownloadManager.STATUS_FAILED:
                    Toast.makeText(mContext, "下载失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        c.close();
    }
    public interface  OnDownLoad{
        void onSuccess();
        void onFailed();
    }
    public void setOnDownLoad(OnDownLoad onDownLoad){
        mListener = onDownLoad;
    }
}
