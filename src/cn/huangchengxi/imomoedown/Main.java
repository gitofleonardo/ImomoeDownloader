package cn.huangchengxi.imomoedown;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader=new FXMLLoader();
        loader.setLocation(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("ImomoeDown");
        primaryStage.setScene(new Scene(root,1200,800));
        Controller controller=loader.getController();
        controller.init();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
