package org.orangepalantir;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Created by on 29.03.17.
 */
public class AnimateSheet extends Application {

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Image image = new Image(getClass().getResourceAsStream("/animation-test.png"));
        primaryStage.setTitle("animated");
        ImageView imageView = new ImageView(image);
        imageView.setViewport(new Rectangle2D(0, 0, 64, 64));

        Timeline timeLine = new Timeline();

        for(int i = 0; i<20*20; i++){
            int x = (i%20)*64;
            int y = (i/20)*64;
            timeLine.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(30*i),
                            new KeyValue(
                                    imageView.viewportProperty(),
                                    new Rectangle2D(x, y, 64, 64)
                            )
                    )
            );
        }
        timeLine.setOnFinished(evt->{
            timeLine.playFromStart();
        });
        StackPane root = new StackPane();
        root.getChildren().add(imageView);
        primaryStage.setScene(new Scene(root, 100, 100));
        primaryStage.show();
        timeLine.play();
    }
}
