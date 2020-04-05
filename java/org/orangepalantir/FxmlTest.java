package org.orangepalantir;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.event.ActionEvent;


public class FxmlTest extends Application {
    @FXML public Button button;
    @FXML public Text text;

    @FXML
    public void clicked(ActionEvent event){
        text.setText("The text has been set." + event);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent parent = FXMLLoader.load(FxmlTest.class.getResource("fxmltest.fxml"));
        Scene scene = new Scene(parent, 240, 100);
        stage.setScene(scene);
        stage.show();
    }
}
