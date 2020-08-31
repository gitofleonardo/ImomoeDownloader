package cn.huangchengxi.imomoedown;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DownloadForm extends Stage {
    private DownloadController controller;

    DownloadForm(){
        FXMLLoader loader=new FXMLLoader();
        loader.setLocation(getClass().getResource("download.fxml"));
        try {
            Parent root=loader.load();
            setResizable(false);
            setTitle("Download");
            setScene(new Scene(root,500,500));
            controller=loader.getController();
            controller.init();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
