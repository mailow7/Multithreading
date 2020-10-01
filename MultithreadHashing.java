import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    static List<String> listfiles = new ArrayList<>();


    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        out = new PrintStream(new FileOutputStream("ListDistributionstoFile.txt", true));
        System.setErr(out);
        System.err.println("soft start: " + df.format(new Date(System.currentTimeMillis())));

        properties.load(new FileInputStream("config.prop"));
        String distribspath = properties.get("distribspath").toString();
        String outfile = properties.get("outfile").toString();
        int threadcount = Integer.parseInt(properties.getProperty("threadcount"));

        try {
            List<String> listdistribs = processFilesFromFolder(new File(distribspath));
            if (!listdistribs.isEmpty()) {
                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadcount);
                for (String i : listdistribs) {
                    System.err.println("path: " + i + " t:" + df.format(new Date(System.currentTimeMillis())));
                    CalcHash queue1 = new CalcHash(i);

                    executor.execute(queue1);
                }

                while (listdistribs.size() != executor.getCompletedTaskCount()) {
                    System.err.println("count=" + executor.getTaskCount() + "," + executor.getCompletedTaskCount());
                    Thread.sleep(5000);
                }
                executor.shutdown();
                executor.awaitTermination(60, TimeUnit.SECONDS);
            }

            Fileslist.pushtofile(outfile);


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
                Path filePath = Paths.get(entry.getPath());

                listfiles.add(filePath.toString());

            }

        } catch (SecurityException e) {
            System.err.println("processFilesFromFolder error: " + e);
        }
        return listfiles;
    }

}

class Fileslist {
    static public HashMap<String, String> filesMap = new HashMap<>();


    public static void setFilesMap(String filepath, String filehash) {

        filesMap.put(filepath, filehash);
        System.err.println(filepath + filehash);
    }

    public static void pushtofile(String filename) {

        //push map to file
        System.err.println("start write to file : ");

        try {
            if (!filesMap.isEmpty()) {
                System.err.println("mapa not empty ");
                Gson gson = new Gson();
                String json = gson.toJson(filesMap);
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

class CalcHash extends Thread {

    String filepath;

    CalcHash(String filepath) {// Конструктор, аргумент- массив имен сотрудников
        this.filepath = filepath;

    }

    @Override
    public void run() { // Этот метод будет вызван при старте потока
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        try {

            System.err.println(filepath + " thread start: " + df.format(new Date(System.currentTimeMillis())));
            String hash = hash(filepath, 8 * 1024);
            System.err.println(filepath + " thread stop: " + df.format(new Date(System.currentTimeMillis())));
            Fileslist.setFilesMap(filepath, hash);

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

