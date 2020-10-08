package cn.huangchengxi.imomoedown;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public class DownloadController {
    @FXML
    public ListView<DownloadHelper.Download> downloadList;

    private DownloadHelper helper=DownloadHelper.instance();

    public void init(){
        downloadList.setItems(helper.mapAsList());
        downloadList.setCellFactory(downloadListView -> new DownloadCell());
    }
    static class DownloadCell extends ListCell<DownloadHelper.Download>{
        private ChangeListener<? super Float> listener;

        @Override
        protected void updateItem(DownloadHelper.Download download, boolean b) {
            super.updateItem(download, b);

            if (!b && download!=null){
                FXMLLoader loader=new FXMLLoader();
                loader.setLocation(getClass().getResource("item_download.fxml"));
                try {
                    Parent root=loader.load();
                    setGraphic(root);
                    ItemController controller=loader.getController();
                    controller.init();
                    listener= (ChangeListener<Float>) (observableValue, aFloat, t1) -> {
                        controller.progress.progressProperty().setValue(t1);
                        if (t1>=1.0){
                            controller.successIcon.visibleProperty().setValue(true);
                        }
                    };
                    download.getPercent().addListener(listener);
                    controller.filename.setText(download.getFilename());
                    controller.progress.progressProperty().setValue(download.getPercent().getValue());
                    if (download.getPercent().getValue()>=1.0){
                        controller.successIcon.visibleProperty().setValue(true);
                    }
                }catch (Exception ignored){}
            }else if (b){
                setText(null);
                setGraphic(null);
            }
        }
    }
}
