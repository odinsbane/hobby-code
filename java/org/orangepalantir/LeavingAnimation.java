package org.orangepalantir;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by msmith on 05.07.17.
 */
public class LeavingAnimation  extends Application {
    int count = 0;
    int max = 55;
    StackPane root;
    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Image image = new Image(getClass().getResourceAsStream("/elephant.png"));
        primaryStage.setTitle("animated");
        ImageView imageView = new ImageView(image);
        imageView.setViewport(new Rectangle2D(0, 0, 62, 62));

        Timeline timeLine = new Timeline();

        for(int i = 55; i>0; i--){
            int x = i>20?2*(i-20):0;
            timeLine.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(60*(55-i)),
                            (evt)->snapShot(),
                            new KeyValue(
                                    imageView.viewportProperty(),
                                    new Rectangle2D(x, 0, 62, 62)
                            )
                    )
            );
        }
        timeLine.setOnFinished(evt->{
            timeLine.playFromStart();
        });
        root = new StackPane();
        root.getChildren().add(imageView);
        primaryStage.setScene(new Scene(root, 100, 100));
        primaryStage.show();
        timeLine.play();

        timeLine.setOnFinished(evt->{
            timeLine.playFromStart();
        });
    }

    void recordAnimation(Timeline timeline){
        for(int i = 0; i<max; i++){
            timeline.playFrom(new Duration(i*60));
            snapShot();
        }
    }

    void snapShot(){
        if(count>=max) return;

        SnapshotParameters params = new SnapshotParameters();

        WritableImage img = root.snapshot(params, null);

        BufferedImage buff = SwingFXUtils.fromFXImage(img, new BufferedImage((int)img.getWidth(), (int)img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR));
        //BufferedImage buff = SwingFXUtils.fromFXImage(img, new BufferedImage((int)img.getWidth(),(int)img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR));
        try {
            ImageIO.write(buff, "PNG", new File(String.format("leaving/leaving-%03d.png", count)));
            count++;
        } catch (IOException e) {
            e.printStackTrace();
            count = Integer.MAX_VALUE;
        }
    }
}