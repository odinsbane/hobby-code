package org.orangepalantir;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by melkor on 3/17/16.
 */
public class SiteCrawler extends Application{
    static String input;
    String host;
    String hostUrl;
    Set<String> visitedLocations = new HashSet<>();
    Set<String> possibleLocations = new HashSet<>();
    Set<String> images = new HashSet<>();

    Set<String> urls = new HashSet<>();
    VBox box;
    public void boot(String host){
        this.host = host;
        hostUrl = "https://" + host;
        possibleLocations.add("/");

    }

    public void crawl() {
        while(possibleLocations.size()>0){
            Set<String> tryingLocations = new HashSet<>(possibleLocations);
            for(String loc: tryingLocations){
                if(!visitedLocations.add(loc)){
                    possibleLocations.remove(loc);
                    continue;
                }
                try {
                    grabLocations(loc);
                    Platform.runLater(()->{box.getChildren().add(new Label(loc));});
                } catch(IOException exc){
                    System.out.println("failed at loc: " + loc);
                    exc.printStackTrace();
                }
                for(String image: images){
                    add(image);
                }
            }
        }

    }


    /**
     * Finds all of the possible locations
     * @param location location from the root!
     * @throws IOException
     * @return root location.
     */
    private void grabLocations(String location) throws IOException {
        URL url = new URL("https", host, location);
        String testData = getText(url);
        //get the current root
        String root = getRoot(location);

        populateData(root, testData);
        populateImages(root, testData);

    }

    private void populateImages(String root, String testData) {
        Set<String> possibleImages = new HashSet<>();
        Pattern p = Pattern.compile("src=[\"']([^\"' ]*?(png)|(jpg)|(gif))");
        Matcher m = p.matcher(testData);
        while(m.find()){
            possibleImages.add(m.group(1));
        }
        for(String s: possibleImages){
            String better = sanitizeImage(root, s);
            if(better!=null){
                images.add(better);
            }
        }
    }

    private String sanitizeImage(String root, String s) {
        if(s.startsWith("/")){
            return s.replace(hostUrl, "");
        } else if(s.startsWith(hostUrl)){
            return s;
        } else if(s.startsWith("http")){
            return null;
        }

        return root + s;
    }

    private String getRoot(String location) {
        int i = location.indexOf("/");
        int t;
        while ((t = location.indexOf("/", i + 1)) > 0) {
            i = t;
        }
        return location.substring(0, i+1);
    }

    final static String href = "href";
    private void populateData(String currentRoot, String testData) {
        Set<String> candidateLocations = new HashSet<>();
        //grab urls.
        int t=-1;
        while((t=testData.indexOf(href, t+1))>=0){
            int open = testData.indexOf("\"", t);
            int close = -1;
            if(open<0){
                open = testData.indexOf("'", t);
                if(open<0){
                    continue;
                }
                close = testData.indexOf("'", open + 1);
            } else {
                close = testData.indexOf("\"", open + 1);
            }
            if(open>=0&&close>open){
                String p = parseLocation(currentRoot, testData.substring(open+1, close));
                if(p!=null) {
                    candidateLocations.add(p);
                }
            }

        }
        for(String loc: candidateLocations){
            possibleLocations.add(loc);

            /*if(loc.endsWith("/")){
                possibleLocations.add(loc);
            } else if(loc.endsWith("htm")){
                possibleLocations.add(loc);
            } else if(loc.endsWith("html")){
                possibleLocations.add(loc);
            }*/
        }
    }

    public String parseLocation(String currentRoot, String target){
        if(target.startsWith(hostUrl)){
            return target.replace(hostUrl, "");
        } else if(target.startsWith("/")){
            return target;
        } else if(target.startsWith("https://")||target.startsWith("http://")){
            return null;
        }

        return currentRoot + target;
    }

    private String getText(URL url) throws IOException {
        InputStream stream = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF8")));
        StringBuilder builder = new StringBuilder();
        String s;
        while((s=reader.readLine())!=null){
            builder.append(s);
        }
        return builder.toString();
    }


    public static void main(String[] args){
        input = args[0];
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        box = new VBox();
        box.getChildren().add(new Label("populating..."));
        stage.setScene(new Scene(new ScrollPane(box)));
        stage.show();
        boot(input);
        new Thread(()->{
            this.crawl();
        }).start();
    }


    public void add(String address) {
        if (urls.add(address)) {
            System.out.println(hostUrl + address);

            Platform.runLater(new Runnable(){
                @Override
                public void run() {
                    Image img = new Image(hostUrl + address);
                    box.getChildren().add(new ImageView(img));
                }
            });

        }
    }
}