package cn.huangchengxi.imomoedown;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ItemController {
    @FXML
    public Label filename;
    @FXML
    public ImageView successIcon;
    @FXML
    public ProgressBar progress;

    public void init(){
        successIcon.setImage(new Image("success.png"));
    }
}
