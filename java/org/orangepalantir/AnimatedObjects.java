package org.orangepalantir;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by 29.03.17.
 */
public class AnimatedObjects extends Application {
    int count;
    StackPane root;
    Canvas canvas;
    int max = 100;
    Scene main;
    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Image image = new Image(getClass().getResourceAsStream("/object-test.png"));
        Image boat = new WritableImage(image.getPixelReader(), 0, 0, 64, 64);
        Image fin = new WritableImage(image.getPixelReader(), 64, 0, 64, 64);
        WritableImage backFin = new WritableImage(64, 64);


        PixelWriter pw = backFin.getPixelWriter();
        for(int i = 0; i<fin.getWidth(); i++){
            for(int j = 0; j<fin.getHeight(); j++){
                pw.setArgb(i, j, fin.getPixelReader().getArgb((int)fin.getWidth()-i-1, j));
            }
        }
        primaryStage.setTitle("floating");

        ImageView boatView = new ImageView(boat);
        ImageView finView = new ImageView(fin);
        ImageView backView = new ImageView(backFin);

        finView.setTranslateY(22);
        backView.setScaleX(0.35);
        backView.setScaleY(0.35);
        backView.setTranslateY(-25);
        Timeline timeLine = new Timeline();

        for(int i = 0; i<max; i++){
            timeLine.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(30*i),
                            new KeyValue(
                                    boatView.rotateProperty(),
                                    8*Math.sin(Math.PI*i/25.0)
                            )
                    )

            );

            timeLine.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(30*i),
                            new KeyValue(
                                    boatView.translateXProperty(),
                                    3*Math.sin(Math.PI*(i + 20)/50.0)
                            )
                    )

            );

            timeLine.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(30*i),
                            new KeyValue(
                                    boatView.translateYProperty(),
                                    4*Math.sin(Math.PI*(i + 50)/50.0) + -5
                            )
                    )

            );

            timeLine.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(30*i),
                            new KeyValue(
                                    finView.translateXProperty(),
                                    4*i - 100
                            )
                    )

            );

            timeLine.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(30*i),
                            new KeyValue(
                                    backView.translateXProperty(),
                                    100-2*i
                            )
                    )

            );
            timeLine.getCuePoints().put("" + i, Duration.millis(30*i + 1));
        }
        canvas = new Canvas(64, 64);

        timeLine.setOnFinished(evt->{
            System.out.println("finito");
            if(count==0){
                recordAnimation(timeLine);
            } else {
                timeLine.playFromStart();
            }
        });
        root = new StackPane();
        main = new Scene(root, 100, 100);

        canvas.getGraphicsContext2D().setFill(new Color(1,1,1,0));
        canvas.getGraphicsContext2D().fillRect(0, 0, 64, 64);
        root.getChildren().add(canvas);
        root.getChildren().add(backView);
        root.getChildren().add(boatView);
        root.getChildren().add(finView);

        primaryStage.setScene(main);
        primaryStage.show();

        //timeLine.jumpTo("" + 50);
        timeLine.play();
    }

    void recordAnimation(Timeline timeline){
        System.out.println("recording");
        for(int i = 0;i<max;i++){
            timeline.playFrom(i+"");
            snapShot();
        }
        System.out.println("finished");
    }

    void snapShot(){
        if(count>=max) return;
        SnapshotParameters params = new SnapshotParameters();

        WritableImage img = root.snapshot(params, null);
        Bounds rect = canvas.getBoundsInParent();
        Bounds rect2 = root.getBoundsInParent();
        Image prepped = new WritableImage(
                img.getPixelReader(),
                (int)(rect.getMinX()-rect2.getMinX()),
                (int)(rect.getMinY()-rect2.getMinY()),
                (int)rect.getWidth(),
                (int)rect.getHeight()
        );
        BufferedImage buff = SwingFXUtils.fromFXImage(prepped, new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR));
        //BufferedImage buff = SwingFXUtils.fromFXImage(img, new BufferedImage((int)img.getWidth(),(int)img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR));
        try {
            ImageIO.write(buff, "PNG", new File(String.format("animated-%03d.png", count)));
            count++;
        } catch (IOException e) {
            e.printStackTrace();
            count = Integer.MAX_VALUE;
        }
    }

}
