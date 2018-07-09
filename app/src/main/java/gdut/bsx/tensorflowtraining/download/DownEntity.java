package gdut.bsx.tensorflowtraining.download;

public class DownEntity {
    public String uri;
    public String des;
    public String fileName;
    public OnDownloadListener listener;

    public DownEntity(String uri, String des, String fileName,OnDownloadListener listener) {
        this.uri = uri;
        this.des = des;
        this.fileName=fileName;
        this.listener = listener;
    }
    public interface OnDownloadListener {
        void onDownloadSuccess();
        void onDownloading(int progress);
        void onDownloadFailed();
    }

}
