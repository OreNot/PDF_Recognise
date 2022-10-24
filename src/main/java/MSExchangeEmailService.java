import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.service.ConflictResolutionMode;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.enumeration.service.MessageDisposition;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import microsoft.exchange.webservices.data.property.complex.FileAttachment;
import microsoft.exchange.webservices.data.property.complex.ItemId;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.FolderView;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MSExchangeEmailService {

    static String username = "aamartynyuk";  // like yourname@outlook.com
    static String password = "";   // password here
    static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    static final String MAIL_FOLDER = "D:\\mailFolder\\";


    public static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
        public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
            return redirectionUrl.toLowerCase().startsWith("https://");
        }
    }

    private static ExchangeService service;
    protected static Integer NUMBER_EMAILS_FETCH;//only latest 5 emails/appointments are fetched.
    /**
     * Firstly check, whether "https://webmail.xxxx.com/ews/Services.wsdl" and "https://webmail.xxxx.com/ews/Exchange.asmx"
     * is accessible, if yes that means the Exchange Webservice is enabled on your MS Exchange.
     */
    static{
        try{
            service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);


            service.setUrl(new URI("https://mail.rosatom.ru/ews/Services.wsdl"));
            //service.setUrl(new URI("https://mail.rosatom.local/ews/Services.wsdl"));
            //service.setUrl(new URI("https://core-s-exc01v1/ews/Services.wsdl"));



        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy");
    /**
     * Initialize the Exchange Credentials.
     * Don't forget to replace the "USRNAME","PWD","DOMAIN_NAME" variables.
     */
    public MSExchangeEmailService() {
        String[] creds;
        creds = credRead();
        username = creds[0].replaceAll("\n", "").replaceAll("\r", "");
        password = creds[1].replaceAll("\n", "").replaceAll("\r", "");


        service.setCredentials(new WebCredentials(username + "@gk.rosatom.local", password));
        try {

            service.autodiscoverUrl("aamartynyuk@greenatom.ru", new RedirectionUrlCallback());


        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(),"Error", JOptionPane.INFORMATION_MESSAGE);

            Logger.getLogger(MSExchangeEmailService.class.getName()).log(Level.SEVERE, null, ex);
        }
        service.setTraceEnabled(true);
    }
    /**
     * Reading one email at a time. Using Item ID of the email.
     * Creating a message data map as a return value.
     */

    /**
     * Number of email we want to read is defined as NUMBER_EMAILS_FETCH,
     */

    public List readEmails(){

        for (File f : new File(MAIL_FOLDER).listFiles())
        {
            try {
                Files.deleteIfExists(f.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List msgDataList = new ArrayList();
        try{

            //Folder folder = Folder.bind(service, WellKnownFolderName.Inbox);
            Folder folder;

           FolderView view = new FolderView(1);
           SearchFilter searchFilter = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, Main.mailFolderName);
            //SearchFilter searchFilter = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, "Plomb");
            FindFoldersResults lResult = service.findFolders(WellKnownFolderName.Inbox, searchFilter, view);

            if (lResult.getTotalCount() > 0)
            {
                folder = lResult.getFolders().get(0);
                System.out.println(folder.getDisplayName());

                FindItemsResults<Item> results = service.findItems(folder.getId(), new ItemView(NUMBER_EMAILS_FETCH));

            int i =0;
            for (Item item : results)
            {

                EmailMessage email = EmailMessage.bind(service, item.getId());


                try{
                if (email.getSender().getAddress().contains("hp84") && email.getSubject() != null && !email.getIsRead() && email.getHasAttachments()) {
                    try {
                        Main.catnum = email.getSubject();
                    }
                    catch (Exception e)
                    {

                    }
                    email.load();
                    for (Attachment it : email.getAttachments()) {
                        FileAttachment iAttachment = (FileAttachment) it;
                        File newFile = new File(MAIL_FOLDER + Main.catnum + "_" + sdf.format(new Date()) + iAttachment.getName());
                        Files.deleteIfExists(newFile.toPath());

                        iAttachment.load();
                        FileUtils.writeByteArrayToFile(newFile, iAttachment.getContent());
                    }
                    email.setIsRead(true);

                    //email.delete();
                    //service.updateFolder(folder);
                    email.update(ConflictResolutionMode.AutoResolve);
                    item.delete(DeleteMode.MoveToDeletedItems);
                    item.update(ConflictResolutionMode.AutoResolve);
                    //service.updateItem(item, folder.getId(), ConflictResolutionMode.AutoResolve, MessageDisposition.SaveOnly, null);
                }

                }catch (Exception e)
                {
                    email.setIsRead(false);
                    email.update(ConflictResolutionMode.AutoResolve);
                    Main.logging("Ошибка чтения письма от " + email.getSender() + " " + Main.dateFormat.format(email.getDateTimeSent()));

                }

                }

            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return msgDataList;
    }

    /**
     * Reading one appointment at a time. Using Appointment ID of the email.
     * Creating a message data map as a return value.
     */

    /**
     *Number of Appointments we want to read is defined as NUMBER_EMAILS_FETCH,
     *  Here I also considered the start data and end date which is a 30 day span.
     *  We need to set the CalendarView property depending upon the need of ours.
     */


    private static String[] credRead()
    {
        String[] creds = new String[0];



        try(FileReader reader = new FileReader("C:\\Creds\\creds.txt"))
        {
            char[] buf = new char[256];
            int c;
            while((c = reader.read(buf))>0){

                if(c < 256){
                    buf = Arrays.copyOf(buf, c);
                }
                creds = String.valueOf(buf).trim().replaceAll(" ", "").split("\n");
            }
        }
        catch(IOException ex){

            System.out.println(ex.getMessage());
        }


        return creds;
    }


}