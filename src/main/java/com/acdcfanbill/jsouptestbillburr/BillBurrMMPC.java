/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.acdcfanbill.jsouptestbillburr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author billconn
 */
public class BillBurrMMPC {

    final static String baseURL = "http://billburr.libsyn.com/webpage/page/1/size/10000";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        String directory = new java.io.File(".").getCanonicalPath();
        List<Element> fileList = new ArrayList();

        Document doc = Jsoup.connect(baseURL).get();

        Elements anchorTags = doc.select("a");
        for (Element tag : anchorTags) {
            String curr = tag.text();
            if (curr.endsWith(".mp3")) {
                fileList.add(tag);
            }
        }

        System.out.println(fileList.size() + " possible files to get.");
        fileList = RemoveUnneeded(fileList, directory);
        System.out.println(fileList.size() + " actual files to get");

        for(Element file : fileList) {
            DLFile(directory, file.absUrl("href"), file.text());
        }
    }

    private static boolean DoesFileExist(String dir, String file) {
        return new File(dir, file).exists();
    }

    private static List<Element> RemoveUnneeded(List<Element> files, String directory) {
        //check current directory for these files so we don't get extras

        List<Element> toRemove = new ArrayList();
        for (Element file : files) {
            if (DoesFileExist(directory, file.text())) {
                toRemove.add(file);
            }
        }
        for (Element tR : toRemove) {
            files.remove((tR));
        }
        return files;
    }

    private static void DLFile(String directory, String theUrl, String finalName) {
        try {
            URL url = new URL(theUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("Cookie", "cookie-name");

            //add request header
            httpConn.setRequestProperty("User-Agent", "test");
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                int contentLength = httpConn.getContentLength();

                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();

                // opens an output stream to save into file
                File tempFile = File.createTempFile("tempfile", ".bin");
                FileOutputStream outputStream = new FileOutputStream(tempFile);

                int bytesRead = -1;
                int currTotal = 0;
                byte[] buffer = new byte[4096];
                long start = System.currentTimeMillis();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    currTotal = currTotal + bytesRead;
                    if (System.currentTimeMillis() - start > 500) {
                        start = System.currentTimeMillis();
                        updateProgress(finalName, (double) currTotal / contentLength);
                    }
                    outputStream.write(buffer, 0, bytesRead);
                }
                System.out.println();

                outputStream.close();
                inputStream.close();

                httpConn.disconnect();

                //Move temp file to cwd with right name
                File newFile = new File(directory, finalName);
                Files.move(tempFile.toPath(), newFile.toPath());

            } else {
                System.out.println("No file to download");
            }
        } catch (IOException e) {

        }
    }

    static void updateProgress(String prepend, double progressPercentage) {
        final int width = 15; // progress bar width in chars

        System.out.print("\r" + prepend + "\t [");
        int i = 0;
        for (; i <= (int) (progressPercentage * width); i++) {
            System.out.print(".");
        }
        for (; i < width; i++) {
            System.out.print(" ");
        }
        System.out.print("]");
    }
}
