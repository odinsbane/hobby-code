package org.orangepalantir;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by msmith on 3/17/16.
 */
public class MakeHttpRequest {
    List<String> headers = new ArrayList<>();
    List<String> content = new ArrayList<>();
    int requestsMade = 0;
    final static int MAX_REQUESTS = 5;
    String host;
    String address;
    private final byte[] buffer = new byte[64000];
    Charset charset = Charset.forName("UTF8");
    final static byte R = '\r';
    final static byte N = '\n';
    public MakeHttpRequest(String host, String address){
        this.host = host;
        this.address = address;
    }
    public void httpRequest() throws IOException {
        System.out.println("requesting");
        requestsMade++;
        if(requestsMade>=MAX_REQUESTS){
            throw new IOException("Max Requests exceed.");
        }

        try {
            Socket s = new Socket(InetAddress.getByName(host), 80);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(String.format("GET %s HTTP/1.1 \r\n", address));
            writer.write(String.format("Host: %s \r\n\r\n", host));
            writer.flush();
            BufferedInputStream reader = new BufferedInputStream(in);
            String line = readHttpLine(reader);
            final String code = line.split(" ")[1];
            headers.add(line);
            switch(code){
                case "200":
                    loadWebpage(reader);
                    break;
                case "301":
                case "302":
                    redirect(reader, host);
                    break;
                default:
                    s.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadWebpage(InputStream reader) throws IOException {
        String line;
        //read header:
        int contentLength = -1;
        while ((line = readHttpLine(reader)) != null) {
            if(line.isEmpty()){
                break;
            }

            headers.add(line);
            if(contentLength<0&&line.startsWith("Content-Length: ")){
                contentLength = Integer.parseInt(line.replace("Content-Length: ", ""), 10);
            }
        }

        for(String l: headers){
            System.out.println(l);
        }
        if(contentLength<0) {
            readChunkedWebPage(reader);
        }else{
            readContentKnown(reader, contentLength);
        }
    }


    public String readHttpLine(InputStream reader) throws IOException {
        for(int i = 0; i<buffer.length; i++){

            byte c = (byte)reader.read();
            buffer[i] = c;
            if(i>0&&buffer[i-1]==R && buffer[i]==N){
                return new String(buffer, 0, i-1, charset);
            }

        }
        return null;
    }


    public void readContentKnown(InputStream reader, int contentLength) throws IOException {
        int read = 0;
        while(read<contentLength){
            read += reader.read(buffer, read, contentLength-read);
        }
        content.add(new String(buffer, 0, contentLength, charset));
    }
    public String readHttpChunk(InputStream reader, int size) throws IOException {
        int read = 0;
        System.out.println(size);
        while(read<size){
            read += reader.read(buffer, read, size-read);
        }
        //read the end of the line.
        if(reader.read()!=R||reader.read()!=N){
            return null;
        }

        return new String(buffer, 0, size, charset);
    }


    public int getChunkSize(InputStream reader) throws IOException {
        return Integer.parseInt(readHttpLine(reader), 16);
    }

    public void readChunkedWebPage(InputStream reader) throws IOException {
        int toRead = getChunkSize(reader);
        while (toRead>0) {
            content.add(readHttpChunk(reader, toRead));
            toRead = getChunkSize(reader);
        }
        reader.close();
    }

    public void redirect(InputStream reader, String host) throws IOException {
        String line;
        while ((line = readHttpLine(reader)) != null) {
            if(line.isEmpty()){
                break;
            }
            if(line.startsWith("Location: http://")){
                String add = line.replace("Location: http://", "");
                int last = add.indexOf("/");
                this.host = add.substring(0, last);
                this.address = add.substring(last);
                makeRequest();
                return;
            }
            headers.add(line);
        }

        throw new IOException("unable to determine redirect location.");


    }

    public static void main(String[] args){
        MakeHttpRequest requester = new MakeHttpRequest("reddit.com", "/");
        try {
            requester.makeRequest();
        } catch (IOException e) {

        }

    }

    public void makeRequest() throws IOException {
        httpRequest();
        for(String l: headers){
            System.out.println(l);
        }
        for(String l: content){
            System.out.println(l);
        }
    }

}
