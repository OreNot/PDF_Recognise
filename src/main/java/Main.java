

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.tess4j.util.PdfUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;

public class Main {

    private static final String IMAGES_FOLDER = "D:\\imagesFolder\\";
    private static final String CITY_FILE_PATH = "D:\\cityes.txt";
    private static final String DICTIONARY_FOLDER = "D:\\tessdata";
    private static String UPLOAD_PATH = "D:\\OrdStorage";
    static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss ");
    static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    protected static String logPath = "D:\\OrdLog.txt";
    static final String ERROR_SCAN_FOLDER = "D:\\errorScanFolder\\";
    protected static String catnum = "";
    protected static int allCount;
    protected static AtomicInteger successCount = new AtomicInteger(0);
    protected static AtomicInteger errorCount = new AtomicInteger(0);
    protected static List<String> cityList = new ArrayList<>();
    protected static String dpi = "150";
    protected static String mailFolderName;
    private static View view;
    static Date startDate;

    public static void main(String... args) {
        //while (true) {

        try {
            cityList = cityRead();
        }catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }

        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.http.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");

        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.WARN);
        Logger.getLogger("httpclient.wire.header").setLevel(Level.WARN);
        Logger.getLogger("httpclient.wire.content").setLevel(Level.WARN);
        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.ERROR);
        Logger.getLogger("httpclient").setLevel(Level.ERROR);
        //System.setProperty("jna.library.path", "ERROR");

        view = new View("Добавление в архив");
        view.pack();
        view.setVisible(true);

        //starting();
       // }
        //}
    }

    public static void starting() {
        Date answerDate = new Date();
        startDate = new Date();

        view.compileButton.setEnabled(false);

        // if (answerDate.getMinutes() % 5 == 0 && answerDate.getSeconds() == 0)
        // {


        if (!Files.exists(Paths.get(MSExchangeEmailService.MAIL_FOLDER))) {
            try {
                Files.createDirectory(Paths.get(MSExchangeEmailService.MAIL_FOLDER));

            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        else
        {
            for(File iDir : new File(MSExchangeEmailService.MAIL_FOLDER).listFiles())
            {
                try {
                    if (iDir.getName().contains(".pdf")) {
                        Files.delete(iDir.toPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        if (!Files.exists(Paths.get(IMAGES_FOLDER))) {
            try {
                Files.createDirectory(Paths.get(IMAGES_FOLDER));

            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        for (File imageFile : Arrays.asList(new File(IMAGES_FOLDER).listFiles())) {

            try {
                Files.deleteIfExists(imageFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!Files.exists(Paths.get(DICTIONARY_FOLDER))) {
            try {
                Files.createDirectory(Paths.get(DICTIONARY_FOLDER));

            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        if (!Files.exists(Paths.get(UPLOAD_PATH))) {
            try {
                Files.createDirectory(Paths.get(UPLOAD_PATH));

            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        if (!Files.exists(Paths.get(ERROR_SCAN_FOLDER))) {
            try {
                Files.createDirectory(Paths.get(ERROR_SCAN_FOLDER));

            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        MSExchangeEmailService service = new MSExchangeEmailService();
        service.readEmails();

        ExecutorService executorService = Executors.newCachedThreadPool();
        ExecutorService fixExecutorService = Executors.newFixedThreadPool(10);
        allCount = new File(MSExchangeEmailService.MAIL_FOLDER).listFiles().length;
        for (File f : new File(MSExchangeEmailService.MAIL_FOLDER).listFiles()) {

            if (f.getName().contains(".pdf")) {
                String paperCatNum = f.getName().substring(0, f.getName().indexOf("_"));
                fixExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (view.zlCheckBox.isSelected())
                        {
                            addZLToArchive(f);
                        }
                        else
                            {
                            addToArchive(f, paperCatNum);
                        }
                    }
                });
            }

/*
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    addToArchive(f);
                }
            });
*/
        }

       // executorService.shutdown();
            fixExecutorService.shutdown();
        try {
            int minutes = 0;
            fixExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            for (File f : new File(MSExchangeEmailService.MAIL_FOLDER).listFiles())
            {
                Files.deleteIfExists(f.toPath());
            }

            //executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            try {
                long milliseconds = new Date().getTime() - startDate.getTime();
                minutes = (int) (milliseconds / (60 * 1000));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            view.compileButton.setEnabled(true);
            JOptionPane.showMessageDialog(null, "Всего : " + Main.allCount + "\nДобавлено заявлений : " + Main.successCount + "\nОшибок добавления : " + Main.errorCount + "\nЗатрачено времени : " + minutes +
                     " минут", "Выполнено", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        } catch (InterruptedException e) {
         e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void addZLToArchive(File f) {
        ITesseract instance = new Tesseract();  // JNA Interface Mapping


        instance.setDatapath(DICTIONARY_FOLDER); // path to tessdata directory

        instance.setTessVariable("user_defined_dpi", dpi);
        instance.setLanguage("rus");


        File[] files;
        try {


            files = PdfUtilities.convertPdf2Png(f);

            //for (File file : files) {
            //BufferedImage bf = ImageIO.read(file);
            for(File imageFile : files) {

                    BufferedImage bf = ImageIO.read(imageFile);
                    bf = ImageHelper.convertImageToGrayscale(bf);
                    File outputfile = new File(IMAGES_FOLDER + f.getName().replaceAll(".pdf", "") + imageFile.getName() + "_image.jpg");
                    ImageIO.write(bf, "jpg", outputfile);
                    //BufferedImage bimg = ImageIO.read(outputfile);
                    //int imgWidth = bimg.getWidth();
                    //System.out.println(imgWidth);
                    bf = ImageHelper.getSubImage(bf, 0, 247, 2200, 410);
                    bf = ImageHelper.getSubImage(bf, 0, 247, 2200, 410);

                    String result = instance.doOCR(bf);
                    String[] resArr = result.split(",");

                    Map<String, String> zlMap = new HashMap<>();

                for(String zlSrting : Arrays.asList(resArr))
                {
                    if (zlSrting.contains("О=") && !zlMap.containsKey("org"))
                    {
                        String orgZlString = zlSrting.substring(zlSrting.indexOf("О=") + 2, zlSrting.length());
                        if (orgZlString.contains(".")) {
                            orgZlString = orgZlString.substring(0, orgZlString.indexOf("."));
                        }
                        zlMap.put("org", orgZlString.replaceAll("О=", ""));
                    }
                    if ((zlSrting.contains("$№") || zlSrting.contains("5№") || zlSrting.contains("5№=")) && !zlMap.containsKey("firstName"))
                    {
                        if (zlSrting.contains("."))
                        {
                            zlSrting = zlSrting.trim().replaceAll("\n", ".").replaceAll("\\.", "|");
                            String [] fioArray = zlSrting.split("\\|");
                            for (String fioString : fioArray)
                            {
                                if ((fioString.contains("$№") || fioString.contains("5№") || fioString.contains("5№=")) && !zlMap.containsKey("firstName"))
                                {
                                    zlMap.put("firstName", fioString.replaceAll("\\$", "").replaceAll("№", "").replaceAll("=", "").replaceAll("[^А-Яа-я]", ""));
                                }
                                if (fioString.contains("С=") && !zlMap.containsKey("lastName"))
                                {
                                    fioString = fioString.substring(fioString.indexOf("С=") + 2, fioString.length());
                                    String[] nameArray = fioString.replaceAll("С=", "").split(" ");
                                    List<String> nameList = new ArrayList<>();

                                    for (String s : nameArray)
                                    {
                                        if (!s.replaceAll(" ", "").equals(""))
                                        {
                                            nameList.add(s);
                                        }
                                    }

                                    if (nameList.size() > 1) {
                                        try {

                                            if (Character.isUpperCase(nameList.get(0).toCharArray()[0]) && Character.isLowerCase(nameList.get(0).toCharArray()[1]) && Character.isUpperCase(nameList.get(1).toCharArray()[0]) && Character.isLowerCase(nameList.get(1).toCharArray()[1]))
                                            {
                                                zlMap.put("lastName", fioString.replaceAll("С=","").replaceAll("[^А-Яа-я ]", ""));
                                            }
                                        }catch (ArrayIndexOutOfBoundsException e)
                                        {
                                            continue;
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            zlMap.put("firstName", zlSrting.replaceAll("\\$", "").replaceAll("№", "").replaceAll("=", "").replaceAll("[^А-Яа-я]", ""));
                        }
                    }
                    if (zlSrting.contains("С=") && !zlMap.containsKey("lastName"))
                    {
                        zlSrting = zlSrting.substring(zlSrting.indexOf("С=") + 2, zlSrting.length());
                        String[] nameArray = zlSrting.replaceAll("С=", "").split(" ");
                        List<String> nameList = new ArrayList<>();

                        for (String s : nameArray)
                        {
                            if (!s.replaceAll(" ", "").equals(""))
                            {
                                nameList.add(s);
                            }
                        }

                        if (nameList.size() > 1) {
                            try {

                                if (Character.isUpperCase(nameList.get(0).toCharArray()[0]) && Character.isLowerCase(nameList.get(0).toCharArray()[1]) && Character.isUpperCase(nameList.get(1).toCharArray()[0]) && Character.isLowerCase(nameList.get(1).toCharArray()[1]))
                                {
                                    zlMap.put("lastName", zlSrting.replaceAll("С=","").replaceAll("[^А-Яа-я ]", ""));
                                }
                            }catch (ArrayIndexOutOfBoundsException e)
                            {
                                continue;
                            }
                        }
                    }
                    if (zlMap.size() == 3)
                    {
                        break;
                    }
                }
                if (zlMap.size() < 3) {
                    logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                    convertImageToPdf(imageFile.getName().replaceAll(".png", "") + "_" + catnum +  ".pdf", ERROR_SCAN_FOLDER, f, imageFile);
                    errorCount.incrementAndGet();
                }

                for (Map.Entry<String, String>  m : zlMap.entrySet())
                {
                    System.out.println(m.getKey() + " " + m.getValue());
                }

                String fio;
                String organization;

                if (zlMap.size() == 3) {
                    fio = zlMap.get("firstName").trim() + " " + zlMap.get("lastName").trim();
                    String directoryName = UPLOAD_PATH + "\\" + fio;
                    organization = zlMap.get("org").trim();
                    catnum = catnum.trim();

                    if (!Files.exists(Paths.get(directoryName))) {
                        try {
                            Files.createDirectory(Paths.get(directoryName));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    File uploadDir = new File(UPLOAD_PATH);

                    if (!uploadDir.exists()) {
                        uploadDir.mkdir();
                    }

                    File fioDir = new File(UPLOAD_PATH + "/" + fio.trim());
                    if (!fioDir.exists()) {
                        fioDir.mkdir();
                        //logging("Каталог \"" + fio.trim() + "\" создан");
                    }

                    //String uuidFile = UUID.randomUUID().toString();
                    String dateForFileName = new SimpleDateFormat("dd.MM.yyyy_HH_mm_ss").format(new Date());
                    String resultFileName = dateForFileName + "_" + fio.split(" ")[0] + "_" + organization.trim().replaceAll("\"", "").replaceAll("\\\\\\\\", "-").replaceAll("/", "-").replaceAll(":", "-") + " ЗЛ" +  "_" + catnum + f.getName().substring(f.getName().lastIndexOf("."), f.getName().length());

                    File destFile = new File(fioDir + "/" + resultFileName.replaceAll("[^a-zA-Zа-яА-Я0-9.. _]", ""));
                    try {
                        convertImageToPdf(resultFileName.replaceAll("[^a-zA-Zа-яА-Я0-9.. _]", ""), UPLOAD_PATH + "/" + fio.trim(), f, imageFile);
                        //FileUtils.copyFile(f, destFile);


                        logging("Файл \"" + resultFileName + "\" добавлен в архив автоматически");
                        successCount.incrementAndGet();
                        //Files.deleteIfExists(f.toPath());
                        //model.put("error", "Документы добавлены");
                    } catch (IOException e) {
                        logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                        convertImageToPdf(imageFile.getName().replaceAll(".png", "") + "_" + catnum + ".pdf", ERROR_SCAN_FOLDER, f, imageFile);
                        //FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + f.getName()));
                        errorCount.incrementAndGet();
                        e.printStackTrace();

                    }
                }
                else
                {
                    logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                    convertImageToPdf(imageFile.getName().replaceAll(".png", "") + "_" + catnum + ".pdf", ERROR_SCAN_FOLDER, f, imageFile);

                    //FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + f.getName()));
                    errorCount.incrementAndGet();
                }

                zlMap.clear();



            }
            /*

*/

            System.out.println("It's All!");

        } catch (IOException e) {
            e.printStackTrace();

            logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
            try {
                FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" + f.getName()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            errorCount.incrementAndGet();
        } catch (TesseractException e) {
            e.printStackTrace();
            logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
            try {
                FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" + f.getName()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            errorCount.incrementAndGet();
        }
    }

    private static void convertImageToPdf(String destFileName, String destFolder, File f, File imageFile) throws IOException {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(new File(new File(destFolder), destFileName)));
            document.open();
            document.newPage();
            Image image = Image.getInstance(IMAGES_FOLDER + f.getName().replaceAll(".pdf", "") + imageFile.getName() + "_image.jpg");
            image.setAbsolutePosition(0, 0);
            image.setBorderWidth(0);
            image.scaleAbsoluteHeight(PageSize.A4.getHeight());
            image.scaleAbsoluteWidth(PageSize.A4.getWidth());
            document.add(image);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    // private static void addToArchive(ITesseract instance, File f) {
    private static void addToArchive(File f, String paperCatNum) {
        ITesseract instance = new Tesseract();  // JNA Interface Mapping


        instance.setDatapath(DICTIONARY_FOLDER); // path to tessdata directory

        instance.setTessVariable("user_defined_dpi", dpi);
        instance.setLanguage("rus");




        File[] files;
        try {
/*
            if (!Files.exists(Paths.get(IMAGES_FOLDER + f.getName().replaceAll(".pdf", "") ))) {
                try {
                    Files.createDirectory(Paths.get(IMAGES_FOLDER + f.getName().replaceAll(".pdf", "")));

                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
            */
            files = PdfUtilities.convertPdf2Png(f);
            //for (File file : files) {
            //BufferedImage bf = ImageIO.read(file);

            BufferedImage bf = ImageIO.read(files[0]);
            bf = ImageHelper.convertImageToGrayscale(bf);
            File outputfile = new File(IMAGES_FOLDER + f.getName().replaceAll(".pdf","") + files[0].getName() + "_image.jpg");
            ImageIO.write(bf, "jpg", outputfile);
            bf = ImageHelper.getSubImage(bf, 555, 770, 1200, 1200);

            String result = instance.doOCR(bf);
            String[] resArr = result.split("\n");


            String[] orgFioStringArray = new String[0];
            String[] resultOrg = new String[0];
            String[] resultFio = new String[0];

            List<String> finalOrgList = new ArrayList<>();
            List<String> finalFioList = new ArrayList<>();
            String fio = "";

            try {

                orgFioStringArray = result.trim().split("Значение");


            try {
                resultOrg = orgFioStringArray[1].split("\n");
                resultFio = orgFioStringArray[2].split("\n");



                for (String sOrg : resultOrg) {
                    if (sOrg.length() > 0) {
                        finalOrgList.add(sOrg);
                    }

                }

                for (String sFio : resultFio) {
                    if (sFio.length() > 0) {
                        finalFioList.add(sFio);
                    }

                }

                String lName = null;
                String fName = null;

                fName = finalFioList.get(0).trim().replaceAll("[^А-Яа-яё]", "").replaceAll("›", "");

                if (fName.contains("Филиал") || fName.contains("Значение") || fName.contains("Значегие") || fName.contains("Дирекция"))
                {
                    throw new ArrayIndexOutOfBoundsException();
                }


                for (int i = 1; i < finalFioList.size(); i++)
                {
                    if (fName.length() < 2 && i == 1 && finalFioList.get(i).trim().split(" ").length < 2)
                    {
                        if (!cityList.contains(finalFioList.get(i).trim())) {
                            fName = finalFioList.get(i).trim();
                        }
                        continue;
                    }
                    String l;
                    l = finalFioList.get(i).trim().replaceAll("[^А-Яа-я ]", "").replaceAll("›", "");

                    if (l.split(" ").length >= 2)
                    {
                        String[] lMas = l.split(" ");
                        String firstLName = "";
                        String secondLName = "";
                        for (int j = 0; j < lMas.length; j++)
                        {
                            if (lMas[j].length() > 1 && lMas[j + 1].length() > 1 && j + 1 <= lMas.length - 1
                                    && Character.isUpperCase(lMas[j].toCharArray()[0])
                                    && Character.isUpperCase(lMas[j + 1].toCharArray()[0])
                                    && Character.isLowerCase(lMas[j].toCharArray()[1])
                                    && Character.isLowerCase(lMas[j + 1].toCharArray()[1])
                                    && (!lMas[j].equals("Акционерное") || !lMas[j + 1].equals("Общество"))
                                    )
                            {
                                firstLName = lMas[j];
                                secondLName = lMas[j + 1];
                                break;
                            }
                            else
                            {
                                throw new ArrayIndexOutOfBoundsException();
                            }
                        }
                        lName = firstLName.trim() + " " + secondLName.trim();
                        break;
                    }
                }
                fio = fName + " " + lName;
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                try {
                    String fNameEr = "";
                    String lNameEr = "";
                    for (String s : resArr) {
                        if (s.contains("ФГУП") || s.contains("Значение") || s.toUpperCase().contains("Значение".toUpperCase()) || s.contains("ул.") || s.contains("улица") || s.contains(",") || s.contains(".") || s.contains("Промзона") || s.contains("[0-9]"))
                        {
                            continue;
                        }
                        s = s.replaceAll("Дирекция", "").replaceAll("Значние", "").replaceAll("Значение", "").replaceAll("Значевие", "").replaceAll("значение", "").replaceAll("Длина", "").replaceAll("Эначение", "").replaceAll("Зачение", "");
                        s = s.trim().replaceAll("[^А-Яа-я ]", "").replaceAll("›", "");
                        if (!s.equals("") && !s.equals(" ") && s.split(" ").length == 1) {
                            s = s.trim().replaceAll("[^А-Яа-я]", "").replaceAll("›", "");

                            if (s.length() >= 2 && s.length() <= 15 && Character.isUpperCase(s.toCharArray()[0]) && Character.isLowerCase(s.toCharArray()[1]) && fNameEr.equals("") && !cityList.contains(s)) {
                                fNameEr = s.trim();
                                continue;
                            }
                        } else if (!s.equals("") && !s.equals(" ") && s.split(" ").length >= 2) {
                            s = s.trim().replaceAll("[^А-Яа-я ]", "").replaceAll("›", "");
                            if (s.split(" ").length > 1 && s.split(" ")[0].length() >= 2 && s.split(" ")[0].length() >= 4) {
                                if (lNameEr.equals("") && Character.isUpperCase(s.trim().split(" ")[0].toCharArray()[0]) && Character.isUpperCase(s.trim().split(" ")[1].toCharArray()[0]) && !cityList.contains(s)) {
                                    lNameEr = s.trim().split(" ")[0] + " " + s.trim().split(" ")[1];
                                    continue;
                                }
                            }
                        }
                    }
                    fNameEr = fNameEr == null ? "Имя" : fNameEr;
                    lNameEr = lNameEr == null || lNameEr.equals("") ? "ИМЯ ОТЧЕТВО" : lNameEr;
                    fio = fNameEr + " " + lNameEr;
                }catch (Exception exp)
                {
                    logging("Ошибка распознавания ФИО, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                    FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" + f.getName()));
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                }
            }

        if (resultOrg.length > 0) {
            for (String sOrg : resArr) {
             if (sOrg.length() > 0) {
            finalOrgList.add(sOrg);
            }

            }
        }

                String organization = "НЕ_ОПРЕДЕЛЕНА";
            try {
                organization = finalOrgList.get(0).replaceAll("Акционерное общество", "АО").replaceAll("начение", "").replaceAll("значение", "").replaceAll("Значение", "").replaceAll("Эначение", "").replaceAll("Зачение", "").trim().replaceAll("[^А-Яа-я ]", "").replaceAll("\"", "").replaceAll("_", "").replaceAll("/", "").replaceAll(":", "").replaceAll("›", "");
                System.out.println(organization.length());
                if (organization.length() < 3)
                {
                    try {
                        String[] orgSplit = organization.split(" ");
                        if (orgSplit.length < 2)
                        {
                            throw new Exception();
                        }
                    }
                    catch (Exception e) {
                        throw new Exception();
                    }
                }
            }catch (Exception e) {
                try{
                e.printStackTrace();
                if (finalOrgList.get(0).contains("Акционерное общество")) {
                    organization = finalOrgList.get(0).trim().replaceAll("Акционерное общество", "АО").replaceAll("[^А-Яа-я ]", "").replaceAll("\"", "").replaceAll("_", "").replaceAll("/", "").replaceAll(":", "");
                }
                if (organization.equals("НЕ_ОПРЕДЕЛЕНА") || organization.length() < 3) {
                    for (String orgName : resArr) {
                        if (orgName.contains("Значение") || orgName.contains("ачение") || orgName.contains("Эначение") || orgName.contains("Зачение") || orgName.length() < 3) {
                            try{
                                String[] orgNameSplit = orgName.split(" ");
                                if (orgNameSplit.length < 2)
                                {
                                    continue;
                                }
                            }catch (Exception exc)
                            {
                                continue;
                            }
                            continue;
                        }
                        if (orgName.contains("АО") ||
                                orgName.toUpperCase().contains("Общество".toUpperCase()) ||
                                orgName.toUpperCase().contains("Акционерное общество".toUpperCase()) ||
                                orgName.toUpperCase().contains("Публичное акционерное общество".toUpperCase()) ||
                                orgName.toUpperCase().contains("Общество с ограниченной отвественностью".toUpperCase()) ||
                                orgName.toUpperCase().contains("Филиал".toUpperCase()) ||
                                orgName.toUpperCase().contains("Предприятие".toUpperCase()) ||
                                orgName.toUpperCase().contains("частное учреждение".toUpperCase()) ||
                                orgName.toUpperCase().contains("Концерн".toUpperCase()) ||
                                orgName.toUpperCase().contains("Государственная корпорация".toUpperCase()) ||
                                orgName.contains("ФГУП") ||
                                orgName.contains("фгуп") ||
                                orgName.contains("ГНЦ") ||
                                orgName.contains("ООО") ||
                                orgName.contains("НПО") ||
                                orgName.contains("ЧУ") ||
                                orgName.contains("АО") ||
                                orgName.contains("АНО") ||
                                orgName.contains("ГУП") ||
                                orgName.contains("ФКП") ||
                                orgName.contains("МУУП") ||
                                orgName.contains("ДУП") ||
                                orgName.contains("ДП") ||
                                orgName.contains("ПК") ||
                                orgName.contains("ООО") ||
                                orgName.contains("ОДО") ||
                                orgName.contains("ОАО") ||
                                orgName.contains("ЗАО") ||
                                orgName.contains("ФЛ") ||
                                orgName.contains("НП") ||
                                orgName.contains("ГУЧ") ||
                                orgName.contains("МУЧ") ||
                                orgName.contains("АО") ||
                                orgName.contains("АОЗТ") ||
                                orgName.contains("АООТ") ||
                                orgName.contains("ТОО") ||
                                orgName.contains("ГП") ||
                                orgName.contains("МУП") ||
                                orgName.contains("ПОО") ||
                                orgName.contains("ППКООП") ||
                                orgName.contains("УОО") ||
                                orgName.contains("УЧПТК") ||
                                orgName.contains("НПО") ||
                                orgName.contains("ПО") ||
                                orgName.contains("СКБ") ||
                                orgName.contains("КБ") ||
                                orgName.contains("УПТК") ||
                                orgName.contains("СМУ") ||
                                orgName.contains("ХОЗУ") ||
                                orgName.contains("НТЦ") ||
                                orgName.contains("ФИК") ||
                                orgName.contains("НПП") ||
                                orgName.contains("ЧИФ") ||
                                orgName.contains("ЧОП") ||
                                orgName.contains("РЭУ") ||
                                orgName.contains("ОП") ||
                                orgName.contains("НПФ") ||
                                orgName.contains("ПКФ") ||
                                orgName.contains("ПКП") ||
                                orgName.contains("ПКК") ||
                                orgName.contains("КФ") ||
                                orgName.contains("ТФ") ||
                                orgName.contains("ТД") ||
                                orgName.contains("ТФПГ") ||
                                orgName.contains("НИФХИ") ||
                                orgName.contains("МФПГ")

                                ) {
                            orgName = orgName.replaceAll("Акционерное общество", "АО");
                            orgName = orgName.replaceAll("Общество с ограниченной отвественностью", "ООО");
                            orgName = orgName.replaceAll("Частное учреждение", "ЧУ");
                            organization = orgName.replaceAll("[^А-Яа-я ]", "");
                            System.out.println(organization);
                            break;
                        }
                    }
                }
            }catch(Exception ex){


                    logging("Ошибка распознавания наименования организации, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                    FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER +  catnum + "_" + f.getName()));
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                }
            }




                System.out.println(fio);
                System.out.println(organization);



                if (fio.split(" ").length == 3) {
                    fio = fio.trim();
                    String directoryName = UPLOAD_PATH + "\\" + fio;
                    organization = organization.trim();
                    catnum = catnum.trim();

                    if (!Files.exists(Paths.get(directoryName))) {
                        try {
                            Files.createDirectory(Paths.get(directoryName));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    File uploadDir = new File(UPLOAD_PATH);

                    if (!uploadDir.exists()) {
                        uploadDir.mkdir();
                    }

                    File fioDir = new File(UPLOAD_PATH + "/" + fio.trim());
                    if (!fioDir.exists()) {
                        fioDir.mkdir();
                        //logging("Каталог \"" + fio.trim() + "\" создан");
                    }

                    //String uuidFile = UUID.randomUUID().toString();
                    String dateForFileName = new SimpleDateFormat("dd.MM.yyyy_HH_mm_ss").format(new Date());
                    String resultFileName = dateForFileName + "_" + fio.split(" ")[0] + "_" + organization.trim().replaceAll("\"", "").replaceAll("\\\\\\\\", "-").replaceAll("/", "-").replaceAll(":", "-") + "_" + paperCatNum + f.getName().substring(f.getName().lastIndexOf("."), f.getName().length());

                    File destFile = new File(fioDir + "/" + resultFileName.replaceAll("[^a-zA-Zа-яА-Я0-9.. _ ]", ""));
                    try {
                        FileUtils.copyFile(f, destFile);
                        //f.transferTo(destFile);

                        logging("Файл \"" + resultFileName + "\" добавлен в архив автоматически");
                        successCount.incrementAndGet();
                        Files.deleteIfExists(f.toPath());
                        //model.put("error", "Документы добавлены");
                    } catch (IOException e) {
                        logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                        FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" + f.getName()));
                        errorCount.incrementAndGet();
                        e.printStackTrace();

                    }
                }
                else
                {
                    logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                    FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" +f.getName()));
                    errorCount.incrementAndGet();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
                FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" +f.getName()));
                errorCount.incrementAndGet();
            }


            //System.out.println(result);
            // }
        } catch (IOException e) {
            e.printStackTrace();

            logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
            try {
                FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" +f.getName()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            errorCount.incrementAndGet();
        } catch (TesseractException e) {
            e.printStackTrace();
            logging("Ошибка распознавания, файл скана отправлен в папку ошибок " + ERROR_SCAN_FOLDER);
            try {
                FileUtils.copyFile(f, new File(ERROR_SCAN_FOLDER + catnum + "_" + f.getName()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            errorCount.incrementAndGet();
        }
    }


    public static synchronized void logging(String text)
    {

        //try(PrintWriter output = new PrintWriter(new FileWriter(logPath,true)))
        try(Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logPath, true), "UTF-8")))
        //try(PrintWriter output = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logPath), StandardCharsets.UTF_8), true))
        {
            writer.append(dateFormat.format(new Date()) + text + "\r\n");
        }
        catch (Exception e) {

            e.printStackTrace();
        }
    }
    private static List<String> cityRead()
    {
        List<String> cityList = new ArrayList<>();
        String[] cityes = new String[0];



        try(FileReader reader = new FileReader(CITY_FILE_PATH))
        {
            char[] buf = new char[256];
            int c;
            while((c = reader.read(buf))>0){

                if(c < 256){
                    buf = Arrays.copyOf(buf, c);
                }
                cityes = String.valueOf(buf).trim().replaceAll(" ", "").split("\n");
                for (String city : cityes) {
                    cityList.add(city.replaceAll("\r", ""));
                }
            }

        }
        catch(IOException ex){

            System.out.println(ex.getMessage());
        }


        return cityList;
    }

}
