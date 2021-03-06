package org.orangepalantir;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
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
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    BlockingQueue<File> toLoad = new ArrayBlockingQueue<File>(100);
    boolean playing = false;
    boolean quitting = false;
    Path outdir;
    Label total, current, time, name;
    int currentIndex;
    Long first;
    Slider select;
    boolean forward = true;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    Future playTask;
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

        MenuItem parentDirectory = new MenuItem("parent directory");
        parentDirectory.addEventHandler(ActionEvent.ACTION, e->{
            DirectoryChooser chooser = new DirectoryChooser();

            File f = chooser.showDialog(primaryStage);
            if(f==null)return;
            Arrays.stream(f.listFiles(File::isDirectory)).forEach(toLoad::add);
        });

        file.getItems().addAll(directory, parentDirectory);
        root.getChildren().add(bar);

        view = new ImageView();
        List<Node> children = root.getChildren();
        children.add(view);

        Node display = buildDisplay();
        Node controls = buildControls(primaryStage);

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
                    System.out.println("taking");
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
            scheduler.shutdown();
            synchronized(view){
                view.notifyAll();
            }
        });

        player.start();


    }


    private Node buildDisplay(){
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
        return display;
    }

    private Node buildControls(Stage primaryStage){
        GridPane controls = new GridPane();

        Button play = new Button("play");
        select = new Slider(0, 1, 0);
        Button next = new Button("next");
        Button prev = new Button("prev");
        controls.add(play, 0, 0);
        controls.add(select, 1, 0, 2, 1);
        controls.add(prev, 3, 0);
        controls.add(next, 4, 0);

        Button setOuputDirectory = new Button("output...");
        Button removeCurrent = new Button("remove");
        Button removeRange = new Button("remove...");
        Button copyCurrent = new Button("copy");
        Button copyRange = new Button("copy...");
        controls.add(setOuputDirectory, 0, 1);
        controls.add(removeCurrent, 1, 1);
        controls.add(removeRange, 2, 1);
        controls.add(copyCurrent, 3, 1);
        controls.add(copyRange, 4, 1);

        next.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{nextImage(); forward=true; schedulePlay();});
        next.addEventHandler(MouseEvent.MOUSE_RELEASED, e->cancelPlay());
        prev.addEventHandler(MouseEvent.MOUSE_PRESSED, e->{previousImage();forward=false; schedulePlay();});
        prev.addEventHandler(MouseEvent.MOUSE_RELEASED, e->cancelPlay());
        play.addEventHandler(ActionEvent.ACTION, (evt)->{
            forward=true;
            play.setText(playing?"play":"stop");
            playing = !playing;
            synchronized(view){
                view.notifyAll();
            }
        });


        select.addEventHandler(MouseEvent.MOUSE_CLICKED, e->{
            int dex = (int)(select.getValue()*images.size());
            selectImage(dex);
        });


        removeCurrent.addEventHandler(ActionEvent.ACTION, (evt)->{
            int i = Integer.parseInt(current.getText());
            removeRange(i,i);
        });
        removeRange.addEventHandler(ActionEvent.ACTION, (evt)->{
            int[] endPoints = getRange("remove");
            if(endPoints!=null) {
                removeRange(endPoints[0], endPoints[1]);
            }
        });
        copyCurrent.addEventHandler(ActionEvent.ACTION, (evt)->{
            if(outdir==null){
                chooseOutdir(primaryStage);
                if(outdir==null) return;
            }
            copyImageRange(currentIndex, currentIndex, outdir);
        });
        copyRange.addEventHandler(ActionEvent.ACTION, (evt)->{
            if(outdir==null){
                chooseOutdir(primaryStage);
                if(outdir==null) return;
            }
            int[] ep = getRange("copy");
            System.out.println("copying: " + ep[0] + ", " + ep[1]);
            if(ep==null) return;
            copyImageRange(ep[0], ep[1], outdir);
        });

        setOuputDirectory.addEventHandler(ActionEvent.ACTION, (evt)->{
            chooseOutdir(primaryStage);
        });
        return controls;
    }

    private int[] getRange(String acceptButtonLabel) {
        int[] range = {-1, -1};
        Dialog<int[]> dialog= new Dialog<>();
        dialog.setTitle("enter range end points");
        TextInputControl low = new TextField("");
        TextInputControl high = new TextField("");
        Label lLabel = new Label("low (inclussive): ");
        Label hLabel = new Label("high (inclussive): ");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(lLabel, 0,0);
        grid.add(low, 1, 0);
        grid.add(hLabel, 0, 1);
        grid.add(high, 1, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType acceptButtonType = new ButtonType(acceptButtonLabel, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptButtonType, ButtonType.CANCEL);

        Node ab = dialog.getDialogPane().lookupButton(acceptButtonType);
        ab.setDisable(true);

        low.textProperty().addListener((ob, old, nv)->{
            boolean empty = nv.trim().isEmpty();
            if(empty){
                range[0] = -1;
                ab.setDisable(true);
            }
            try{
                int i = Integer.parseInt(nv);
                range[0] = i;
            } catch(Exception e){
                range[0] = -1;
                //not a good value;
                ab.setDisable(true);
            }
            boolean ready = range[0]>=0 && range[1]>=range[0] && range[1]<images.size();
            ab.setDisable(!ready);
        });

        high.textProperty().addListener((ob, old, nv)->{
            boolean empty = nv.trim().isEmpty();
            if(empty){
                range[1] = -1;
                ab.setDisable(true);
            }
            try{
                int i = Integer.parseInt(nv);
                range[1] = i;
            } catch(Exception e){
                range[1] = -1;
                //not a good value;
                ab.setDisable(true);
            }
            boolean ready = range[0]>=0 && range[1]>=range[0] && range[1]<images.size();
            ab.setDisable(!ready);
        });

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton==acceptButtonType){

                int a = Integer.parseInt(low.getText());
                int b = Integer.parseInt(high.getText());
                range[0] = a;
                range[1] = b;
                return range;
            }
            return null;
        });

        dialog.showAndWait();
        return dialog.getResult();
    }


    private void chooseOutdir(Stage stage){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("set output directory");
        File f = chooser.showDialog(stage);
        if(f!=null){
            outdir = f.toPath();
        }
    }
    private void copyImageRange(int start, int end, Path directory){
        for(int i = start; i<=end; i++){
            File old = images.get(i).source;
            Path newer = directory.resolve( old.getName() );
            try {
                Files.copy(old.toPath(), newer, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes images from the list.
     * @param start inclusive.
     * @param end inclusive.
     */
    private void removeRange(int start, int end){
        /*for(int i = 0; i<=end-start;i++){
            images.remove(start);
        }*/
        if(images.size()==0) return;
        if(start>end) return;

        FileInfo fi = images.get(currentIndex);

        int s_start = start>0?start:0;
        int s_end = end<images.size()?end:images.size()-1;
        images.subList(s_start, s_end+1).clear();

        if(images.size()>0) {
            first = images.get(0).time;
        }

        for(int i = 0; i<images.size();i++){
            images.get(i).setIndex(i);
        }

        if(fi.index<images.size()){
            //same image, or an image at the same index.
            setImage(images.get(fi.index));
        } else if(images.size()>0){
            //last image
            setImage(images.get(images.size()-1));
        }
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
        for(File f: imageFiles){
            if(images.size()==0){
                first = f.lastModified();
            }
            images.add(new FileInfo(images.size(), f, f.lastModified()));


            if(quitting){
                break;
            }
        }
        Platform.runLater(()->{
            total.setText(images.size() + "");
        });

    }

    private void nextImage(){
        if(images.size()==0) return;
        currentIndex = (currentIndex+1)%images.size();
        setImage(images.get(currentIndex));
    }

    private void cancelPlay(){
        playTask.cancel(false);
        scheduler.submit(()->{
            synchronized(view){
                playing = false;
            }
        });
    }
    public void schedulePlay(){
        playTask = scheduler.schedule(
            ()->{
                synchronized(view){
                    playing = true;
                    view.notifyAll();
                }
            }, 500, TimeUnit.MILLISECONDS
        );
    }

    private void previousImage(){
        if(images.size()==0) return;
        if(currentIndex==0){
            currentIndex  = images.size() -1;
        } else{
            currentIndex = currentIndex-1;
        }

        setImage(images.get(currentIndex));
    }

    private void setImage(FileInfo inf){
        final Image img = inf.getImage();
        Platform.runLater(()->{
            currentIndex = inf.index;
            view.setImage(img);
            updateLabels();
        });
    }

    private void selectImage(int i){
        if(i<0||i>=images.size()){
            return;
        }
        setImage(images.get(i));
    }

    private void playLoop(){
        boolean interrupted = false;
        while(!interrupted){
            if(playing){
                long start = System.currentTimeMillis();
                int n = images.size();
                if(n==0){
                    playing=false;
                    continue;
                }
                if(forward) {
                    nextImage();
                } else{
                    previousImage();
                }
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

    private void updateLabels(){
        total.setText("" + images.size());
        //Label total, current, time, name;
        current.setText("" + currentIndex);
        time.setText(images.get(currentIndex).getTimeString(first));
        name.setText(images.get(currentIndex).name);
        select.setValue(currentIndex*1.0/images.size());
    }
    class FileInfo{
        final String name;
        final Long time;
        int index;
        final int i;
        SoftReference<Image> imgRef;
        final File source;
        FileInfo(int index, File f, long time){
            source = f;
            this.i = index;
            name = f.getName();
            this.time=time;
            this.index = index;
            imgRef = new SoftReference<>(null);
        }

        public void setIndex(int i){
            this.index = i;
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

        public String getTimeString(long t){
            Duration duration = Duration.ofMillis(time-t);
            long millis = duration.toMillis()%1000;
            long secs = duration.toMillis()/1000%60;
            return String.format("%02d::%02d::%02d.%03d",duration.toHours()%24, duration.toMinutes()%60, secs, millis);
        }

        private Image loadImage(){
            return  new Image(source.toURI().toString(), 0, 480, true, false, false);
        }
    }
}
