package cn.huangchengxi.imomoedown;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author huang....
 * This application is only design for imomoe.jp
 * It won't work on other website
 */

public class Controller {
    @FXML
    public TextField urlField;
    @FXML
    public Button goBtn;
    @FXML
    public WebView webView;
    @FXML
    public Button checkBtn;
    @FXML
    public Button downloadBtn;
    @FXML
    public Button refreshBtn;
    @FXML
    public AnchorPane rootPane;
    @FXML
    public GridPane gridPane;
    @FXML
    public HBox historyBtnPane;
    @FXML
    public StackPane bottomPane;
    @FXML
    public Button aboutBtn;
    @FXML
    public Button downloadInfoBtn;
    public TextField filenamePrefix;
    public TextField filenameSuffix;
    public TextField directory;
    public Button selectDirBtn;

    private WebEngine webEngine;

    private DownloadHelper downloadHelper=DownloadHelper.newInstance();

    private String defaultLocation="/home/huangchengxi/Downloads";

    /**
     * initialize the components
     */
    public void init(Stage stage){
        checkEngine();
        webEngine.load("http://imomoe.jp");
        makeSceneAutoAdjust();
        Button backBtn=new Button("",new ImageView("back.png"));
        backBtn.setOnAction(actionEvent -> {
            backHistory();
        });
        Button forwardBtn=new Button("",new ImageView("forward.png"));
        forwardBtn.setOnAction(actionEvent ->{
            forwardHistory();
        });
        historyBtnPane.getChildren().addAll(backBtn,forwardBtn);
        aboutBtn.setGraphic(new ImageView("caution.png"));
        downloadInfoBtn.setGraphic(new ImageView("ic_download.png"));
        selectDirBtn.setOnAction(e->{
            DirectoryChooser chooser=new DirectoryChooser();
            File file=chooser.showDialog(stage);
            if (file!=null){
                defaultLocation=file.getAbsolutePath();
                directory.setText(file.getAbsolutePath());
            }
        });
    }

    /**
     * make scene auto adjust when main window is resized
     */
    private void makeSceneAutoAdjust(){
        gridPane.prefWidthProperty().bind(rootPane.widthProperty());
        gridPane.prefHeightProperty().bind(rootPane.heightProperty());
    }

    /**
     * browse the given url
     */
    public void goUrl(){
        String url=urlField.getText();
        if (url==null || url.equals("")){
            showEmptyAlert();
            return;
        }
        webEngine.load(url);
    }

    /**
     * check if the engine is initialized when the app first started
     */
    private void checkEngine(){
        if (webEngine==null){
            webEngine=webView.getEngine();
        }
    }

    /**
     * show alert dialog when go url is empty
     *
     */
    private void showEmptyAlert(){
        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("Check you url format and try again!!");
        alert.showAndWait();
    }

    /**
     * refresh current page
     */
    public void refreshPage(){
        webEngine.reload();
    }

    /**
     * go back through history stack
     */
    public void backHistory(){
        WebHistory history=webEngine.getHistory();
        int backIndex=history.getCurrentIndex()>0?-1:0;
        history.go(backIndex);
    }

    /**
     * go forward through history stack
     */
    public void forwardHistory(){
        try{
            webEngine.getHistory().go(1);
        }catch (Exception ignored){}
    }

    /**
     * check if the page contains available video
     */
    public void check(){
        String url=checkDownloadAvailability();
        if (url!=null){
            showDownloadAvailable(url);
        }else{
            showDownloadNotAvailable();
        }
    }

    /**
     * check if current page contains available video for downloading,
     * when there is, the download will be start automatically, if not, show alert
     * @return video url, null if not available
     */
    private String checkDownloadAvailability(){
        String url=getUrlFromPlay2();
        if (url==null){
            return null;
        }
        Pattern pattern=Pattern.compile("vid=(https*://[\\s\\S]+?)&");
        Matcher matcher=pattern.matcher(url);
        if (matcher.find()){
            return matcher.group(1);
        }else{
            return null;
        }
    }

    /**
     * get url from current page
     * @return video urla
     */
    private String getUrlFromPlay2(){
        try{
            Document document=webEngine.getDocument();
            Element element=document.getElementById("play2");
            return element.getAttribute("src");
        }catch (Exception e){
            return null;
        }
    }

    /**
     * show alert if download not available
     */
    private void showDownloadNotAvailable(){
        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("This page does not contains available video to download.");
        alert.showAndWait();
    }

    /**
     * show alert if download available, and show the url of this video
     * @param url, url of video
     */
    private void showDownloadAvailable(String url){
        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Available!!");
        alert.setHeaderText("This video is available to download");
        Hyperlink link=new Hyperlink(url);
        link.setOnAction(actionEvent -> {
            downloadOnBrowser(url);
        });
        alert.getDialogPane().setContent(link);
        alert.showAndWait();
    }

    public void downloadCurrentVideo(){
        String url=checkDownloadAvailability();
        //String url="https://gss3.baidu.com/6LZ0ej3k1Qd3ote6lo7D0j9wehsv/tieba-smallvideo/60_a435cdde645dfbc07d6b03dcbff01f66.mp4";
        if (url==null){
            showDownloadNotAvailable();
            return;
        }
        String dir=directory.getText();
        String prefix=filenamePrefix.getText();
        String suffix=filenameSuffix.getText();

        Pattern pattern=Pattern.compile("^[0-9]+$");
        Matcher matcher=pattern.matcher(suffix);
        if (!matcher.find()){
            suffix="0";
            filenameSuffix.setText("0");
        }else{
            filenameSuffix.setText((Integer.parseInt(suffix)+1)+"");
        }

        if (directory==null || directory.getText().equals("")){
            showDirectoryNotSelected();
            return;
        }

        DownloadHelper.Download download=new DownloadHelper.Download(url, dir,prefix+"_"+suffix, new DownloadHelper.DCallback() {
            @Override
            public void onStart(DownloadHelper.Download download) {
                //System.out.println("start");
            }

            @Override
            public void onPause(DownloadHelper.Download download) {
                //System.out.println("pause");
            }

            @Override
            public void onStop(DownloadHelper.Download download) {
                //System.out.println("stop");
            }

            @Override
            public void onRestart(DownloadHelper.Download download) {
                //System.out.println("restart");
            }

            @Override
            public void onWaiting(DownloadHelper.Download download) {
                //System.out.println("waiting");
            }

            @Override
            public void onStarting(DownloadHelper.Download download) {
                //System.out.println("starting");
            }

            @Override
            public void onProgressUpdated(DownloadHelper.Download download) {
                //System.out.println("update progress");
            }

            @Override
            public void onError(DownloadHelper.Download download) {
                //System.out.println("error");
            }

            @Override
            public void onDone(DownloadHelper.Download download) {
                //System.out.println("done,size:"+download.getTotalSize());
            }
        });
        downloadHelper.commitDownload(download);
    }

    private void showDirectoryNotSelected(){
        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("Directory Not Selected.");
        alert.showAndWait();
    }

    /**
     * call system browser to download current video
     */
    public void downloadOnBrowser(){
        String url=checkDownloadAvailability();
        if (url!=null){
            downloadOnBrowser(url);
        }else{
            showDownloadNotAvailable();
        }
    }

    /**
     * call system browser to download video
     * @param url, url of the video
     */
    public void downloadOnBrowser(String url){
        try{
            //Desktop.getDesktop().browse(new URI(url));
            browse(url);
        }catch (Exception e){
            showErrorOpeningBrowser();
        }
    }

    /**
     * open system browser
     * @param url, url to open
     * @throws Exception,
     */
    private void browse(String url) throws Exception {
        // 获取操作系统的名字
        String osName = System.getProperty("os.name", "");
        if (osName.startsWith("Mac OS")) {
            // 苹果的打开方式
            Class fileMgr = Class.forName("com.apple.eio.FileManager");
            Method openURL = fileMgr.getDeclaredMethod("openURL",
                    new Class[] { String.class });
            openURL.invoke(null, new Object[] { url });
        } else if (osName.startsWith("Windows")) {
            // windows的打开方式。
            Runtime.getRuntime().exec(
                    "rundll32 url.dll,FileProtocolHandler " + url);
        } else {
            // Unix or Linux的打开方式
            String[] browsers = { "firefox", "opera", "konqueror", "epiphany",
                    "mozilla", "netscape" };
            String browser = null;
            for (int count = 0; count < browsers.length && browser == null; count++)
                // 执行代码，在brower有值后跳出，
                // 这里是如果进程创建成功了，==0是表示正常结束。
                if (Runtime.getRuntime()
                        .exec(new String[] { "which", browsers[count] })
                        .waitFor() == 0)
                    browser = browsers[count];
            if (browser == null)
                throw new Exception("Could not find web browser");
            else
                // 这个值在上面已经成功的得到了一个进程。
                Runtime.getRuntime().exec(new String[] { browser, url });
        }
    }

    /**
     * show error alert if failed to open browser
     *
     */
    private void showErrorOpeningBrowser(){
        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("Failed to open url on system web browser.");
        alert.showAndWait();
    }

    /**
     * show information about this app
     */
    public void showAboutInfo(){
        String aboutInfo="This application is designed only for imomoe.jp. It won't work on other site. This application is totally free, so be careful of the liars, you can find sourcecode of this app on github.com";
        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setContentText(aboutInfo);
        alert.setResizable(true);
        alert.show();
    }

    /**
     * show download progress
     */
    public void showDownloadInfo(){
        DownloadForm downloadForm=new DownloadForm();
        downloadForm.show();
    }
}
