import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MultithreadHashing {
    static PrintStream out;
    static List<Distribs> listfiles = new ArrayList<Distribs>();
    static List<String> files = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        out = new PrintStream(new FileOutputStream("ListDistributionstoFile.txt", true));
        System.setErr(out);

        long m = System.currentTimeMillis();

        System.err.println("soft start: " + df.format(new Date(System.currentTimeMillis())));

        properties.load(new FileInputStream("config.prop"));
        String distribspath = properties.get("distribspath").toString();
        String outfile = properties.get("outfile").toString();
        int threadcount = Integer.parseInt(properties.getProperty("threadcount"));


        try {

            List<String> files = processFilesFromFolder(new File(distribspath));
            //List<Distribs> listdistribs = processFilesFromFolder(new File(distribspath));
            if (!files.isEmpty()) {
                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadcount);

                for (int i = 0; i < files.size(); i++) {
                    //System.err.println("path: " + files.get(i) + " t:" + df.format(new Date(System.currentTimeMillis())));
                    CalcHash queue1 = new CalcHash(files.get(i));

                    executor.execute(queue1);
                }
//TODO заменить files.size() на executor.getTaskCount()
                while (executor.getCompletedTaskCount() != files.size()) {
                    System.err.print("count=" + executor.getTaskCount() + "," + executor.getCompletedTaskCount());
                    System.err.printf("  Elapsed minutes: " + TimeUnit.MILLISECONDS.toMinutes((System.currentTimeMillis() - m)));
                    System.err.println();
                    Thread.sleep(5000);
                }
                executor.shutdown();
                executor.awaitTermination(60, TimeUnit.SECONDS);
            }


            pushtofile(listfiles, outfile);


        } catch (InterruptedException e) {
            System.err.println("InterruptedException" + e.getMessage());

        }
        System.err.println("soft stop: " + df.format(new Date(System.currentTimeMillis())));


        out.close();
    }

    public static List<String> processFilesFromFolder(File folder) {


        try {

            File[] folderEntries = folder.listFiles();

            for (File entry : folderEntries) {
                if (entry.isDirectory()) {
                    processFilesFromFolder(entry);
                    continue;
                }

                files.add(entry.getPath());

            }

        } catch (SecurityException e) {
            System.err.println("processFilesFromFolder error: " + e);
        }
        return files;
    }


    public static void pushtofile(List<Distribs> fileslist, String filename) {

        try {
            if (!fileslist.isEmpty()) {
                Gson gson = new Gson();
                String json = gson.toJson(fileslist);
                InputStream initialStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                File targetFile = new File(filename);
                OutputStream outStream = new FileOutputStream(targetFile);

                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = initialStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            System.err.println("function : " + e.getMessage());
        }
    }


}

class Distribs {

    protected String filePath;
    protected String fileHash;
    protected long fileCreatedtime;
    protected long size;

}


class CalcHash extends Thread {

    String filepath;

    CalcHash(String filepath) {// Конструктор, аргумент- массив имен сотрудников
        this.filepath = filepath;

    }

    @Override
    public void run() { // Этот метод будет вызван при старте потока
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        try {

            Path filePath = Paths.get(filepath);
            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);

            Distribs distrib = new Distribs();
            distrib.filePath = filepath;

            distrib.fileCreatedtime = attr.creationTime().to(TimeUnit.MILLISECONDS);
            distrib.size = attr.size();

            distrib.fileHash = hash(filepath, 8 * 1024);

            MultithreadHashing.listfiles.add(distrib);

        } catch (IOException e) {
            System.err.println("CalcHash IOException error: " + e);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("CalcHash NoSuchAlgorithmException error: " + e);
        }


    }

    public static String hash(String filepath, int bufferSize) throws IOException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256"); //SHA, MD2, MD5, SHA-256, SHA-384...
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(filepath));

        byte[] buffer = new byte[bufferSize];
        int sizeRead = -1;
        while ((sizeRead = in.read(buffer)) != -1) {
            md.update(buffer, 0, sizeRead);
        }
        in.close();
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }


}



