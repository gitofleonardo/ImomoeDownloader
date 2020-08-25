package cn.huangchengxi.imomoedown;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.lang.reflect.Method;
import java.net.URI;
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

    private WebEngine webEngine;

    /**
     * initialize the components
     */
    public void init(){
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
        Button aboutBtn=new Button("",new ImageView("caution.png"));
        aboutBtn.setAlignment(Pos.CENTER_LEFT);
        aboutBtn.setOnAction(actionEvent -> {
            showAboutInfo();
        });
        bottomPane.getChildren().add(aboutBtn);
    }
    private void makeSceneAutoAdjust(){
        gridPane.prefWidthProperty().bind(rootPane.widthProperty());
        gridPane.prefHeightProperty().bind(rootPane.heightProperty());
    }
    public void goUrl(){
        String url=urlField.getText();
        if (url==null || url.equals("")){
            showEmptyAlert();
            return;
        }
        webEngine.load(url);
    }
    private void checkEngine(){
        if (webEngine==null){
            webEngine=webView.getEngine();
        }
    }
    private void showEmptyAlert(){
        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("Check you url format and try again!!");
        alert.showAndWait();
    }
    public void refreshPage(){
        webEngine.reload();
    }
    public void backHistory(){
        WebHistory history=webEngine.getHistory();
        int backIndex=history.getCurrentIndex()>0?-1:0;
        history.go(backIndex);
    }
    public void forwardHistory(){
        try{
            webEngine.getHistory().go(1);
        }catch (Exception ignored){}
    }
    public void check(){
        String url=checkDownloadAvailability();
        if (url!=null){
            showDownloadAvailable(url);
        }else{
            showDownloadNotAvailable();
        }
    }
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
    private String getUrlFromPlay2(){
        try{
            Document document=webEngine.getDocument();
            Element element=document.getElementById("play2");
            return element.getAttribute("src");
        }catch (Exception e){
            return null;
        }
    }
    private void showDownloadNotAvailable(){
        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("This page does not contains available video to download.");
        alert.showAndWait();
    }
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
    public void downloadOnBrowser(){
        String url=checkDownloadAvailability();
        if (url!=null){
            downloadOnBrowser(url);
        }else{
            showDownloadNotAvailable();
        }
    }
    public void downloadOnBrowser(String url){
        try{
            //Desktop.getDesktop().browse(new URI(url));
            browse(url);
        }catch (Exception e){
            showErrorOpeningBrowser();
        }
    }
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

    private void showErrorOpeningBrowser(){
        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("Failed to open url on system web browser.");
        alert.showAndWait();
    }
    private void showAboutInfo(){
        String aboutInfo="This application is designed only for imomoe.jp. It won't work on other site. This application is totally free, so be careful of the liars, you can find sourcecode of this app on github.com";
        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setContentText(aboutInfo);
        alert.setResizable(true);
        alert.show();
    }
}
