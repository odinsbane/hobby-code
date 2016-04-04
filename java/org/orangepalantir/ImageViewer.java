package org.orangepalantir;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.awt.FileDialog;
import java.io.File;
import java.io.FileFilter;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A javafx based image viewer. This loads a directory of images and allows them to be played through.
 *
 * Created by odinsbane on 3/7/16.
 */
public class ImageViewer extends Application {
    ImageView view;
    List<FileInfo> images = new ArrayList<>();
    BlockingQueue<File> toLoad = new ArrayBlockingQueue<File>(1);
    boolean playing = false;
    boolean quitting = false;
    Label total, current, time, name;
    Long first;
    Slider select;

    String labelStyle = "-fx-min-width: %1$dpx;-fx-pref-width: %1$dpx;-fx-max-width: %1$dpx;-fx-text-alignment: right;" +
            "-fx-padding: 10px;-fx-border-width: 1 ;-fx-border-color: black;";

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Hello World!");

        VBox root = new VBox();

        MenuBar bar = new MenuBar();
        Menu file = new Menu("file");
        bar.getMenus().add(file);
        MenuItem directory = new MenuItem("input directory");

        directory.addEventHandler(ActionEvent.ACTION, e->{
            DirectoryChooser chooser = new DirectoryChooser();
            File f = chooser.showDialog(primaryStage);
            if(f!=null) {
                toLoad.add(f);
            }
        });

        file.getItems().add(directory);
        root.getChildren().add(bar);

        view = new ImageView();
        List<Node> children = root.getChildren();
        children.add(view);

        Button play = new Button("play");
        play.addEventHandler(ActionEvent.ACTION, (evt)->{
            play.setText(playing?"play":"stop");
            playing = !playing;
            synchronized(view){
                view.notifyAll();
            }
        });

        Button next = new Button("next");
        next.addEventHandler(ActionEvent.ACTION, e->nextImage());
        Button prev = new Button("prev");
        prev.addEventHandler(ActionEvent.ACTION, e->previousImage());
        select = new Slider(0, 1, 0);
        select.addEventHandler(MouseEvent.MOUSE_CLICKED, e->{
            int dex = (int)(select.getValue()*images.size());
            selectImage(dex);
        });
        HBox controls = new HBox();

        controls.getChildren().addAll(play, select, next, prev);

        HBox display = new HBox();
        total = new Label("0");
        total.setStyle(String.format(labelStyle, 75));
        current = new Label("0");
        current.setStyle(String.format(labelStyle, 75));

        time = new Label("0");
        time.setStyle(String.format(labelStyle, 200));

        name = new Label("-");
        name.setStyle(String.format(labelStyle, 250));

        Label zero = new Label("0");
        zero.setStyle(String.format(labelStyle, 75));
        display.getChildren().addAll(zero, current, total, time, name);

        children.add(display);
        children.add(controls);

        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
        Thread player = new Thread(()->{
            playLoop();
        });
        Thread reader = new Thread(()->{
            while(!quitting){
                try {
                    loadImages(toLoad.take());
                } catch (InterruptedException e) {
                    quitting=true;
                }
            }
        });
        reader.start();
        primaryStage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, evt->{
            playing = false;
            quitting = true;
            player.interrupt();
            reader.interrupt();

            synchronized(view){
                view.notifyAll();
            }
        });

        player.start();


    }
    final static List<String> imgs = Arrays.stream(new String[]{"png", "jpg", "gif"}).collect(Collectors.toList());

    private void loadImages(File s) {
        File[] imageFiles = s.listFiles(pathname -> {
            if(pathname.isDirectory()) {
                return false;
            }
            String[] borked = pathname.getName().split("\\.");
            String ext = borked[borked.length-1].toLowerCase();
            return imgs.contains(ext);
        });
        //images.clear();
        int n = imageFiles.length;
        int count = n/100;
        count = count>0?count:1;
        Pattern p = Pattern.compile("[0-9]*");

        Comparator<File> c = Comparator.comparingInt(f->{
            Matcher m = p.matcher(f.getName());
            int index = 0;
            int l = 0;
            while(m.find()){
                String tag = m.group(0);
                if(tag.length()>l){
                    index = Integer.parseInt(tag);
                    l = tag.length();
                }
            }
            return index;
        });

        Arrays.sort(imageFiles, c.thenComparing(Comparator.comparingLong(File::lastModified)));
        int i = 0;
        for(File f: imageFiles){
            if(images.size()==0){
                first = f.lastModified();
            }
            images.add(new FileInfo(images.size(), f, f.lastModified()-first));
            i++;
            if(i%count==0){
                System.out.println(i*100.0/n);
            }

            if(quitting|toLoad.size()>0){
                break;
            }
        }

        Platform.runLater(()->{
            total.setText(images.size() + "");
        });

    }

    private void nextImage(){
        if(images.size()==0) return;
        int c = getCurrentIndex();
        c = (c+1)%images.size();
        setImage(images.get(c));

    }

    private void previousImage(){
        if(images.size()==0) return;
        int c = getCurrentIndex();
        if(c==0){
            c  = images.size() -1;
        } else{
            c = c-1;
        }

        setImage(images.get(c));
    }

    private void setImage(FileInfo inf){
        final Image img = inf.getImage();
        Platform.runLater(()->{
            view.setImage(img);
            name.setText(inf.name);
            current.setText(inf.index);
            time.setText(inf.time);
            select.setValue(inf.i*1.0/images.size());
        });
    }

    private void selectImage(int i){
        if(i<0||i>=images.size()){
            return;
        }
        setImage(images.get(i));
    }

    public int getCurrentIndex(){
        return Integer.parseInt(current.getText());
    }
    private void playLoop(){
        int index = 0;
        boolean interrupted = false;
        while(!interrupted){

            if(playing){
                long start = System.currentTimeMillis();
                int n = images.size();
                if(n==0){
                    playing=false;
                    break;
                }
                nextImage();

                try {
                    long dif = System.currentTimeMillis() - start;
                    if(dif<30) {
                        Thread.sleep(30 - dif);
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            } else{
                synchronized(view){
                    try {
                        view.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
        }

    }

    class FileInfo{
        final String name;
        final String time;
        final String index;
        final int i;
        SoftReference<Image> imgRef;
        final File source;
        FileInfo(int index, File f, long time){
            source = f;
            this.i = index;
            name = f.getName();
            Duration duration = Duration.ofMillis(time);
            long millis = duration.toMillis()%1000;
            long secs = duration.toMillis()/1000%60;
            if(duration.toHours()>0){
                System.out.println("time");
            }
            this.time = String.format("%02d::%02d::%02d.%03d",duration.toHours()%24, duration.toMinutes()%60, secs, millis);
            this.index = String.format("%d", index);
            imgRef = new SoftReference<>(null);
        }

        public Image getImage(){
            final Image img = imgRef.get();
            if(img==null){
                Image reloaded =loadImage();
                imgRef = new SoftReference<>(reloaded);
                return reloaded;
            } else{
                return img;
            }
        }

        private Image loadImage(){
            return  new Image(source.toURI().toString(), 0, 480, true, false, false);
        }
    }
}
