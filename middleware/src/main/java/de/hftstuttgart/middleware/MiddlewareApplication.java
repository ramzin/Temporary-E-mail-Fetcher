package de.hftstuttgart.middleware;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

@SpringBootApplication
@RestController
public class MiddlewareApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiddlewareApplication.class, args);
	}

	@GetMapping("/test")
	public String hello(@RequestParam(value = "name", defaultValue = "HFT") String name) {
		
		String protocol = "imap";
        String host = "imap.gmail.com";
        String port = "993";
 
 
        String userName = "softtechproj1@gmail.com";
        String password = "AaBb123321.";
        
        downloadEmails(protocol, host, port, userName, password);
		
		return String.format("Hello %s!", name);
	}
	
	@GetMapping("/test2")
	public String hello2() {
		return String.format("Test2!");
	}
	
	@GetMapping("/test3/{parameter}")
	public String hello3(@PathVariable String parameter) {
		return String.format("Hello %s!", parameter);
	}
	
	 /**
     * Returns a Properties object which is configured for a POP3/IMAP server
     *
     * @param protocol either "imap" or "pop3"
     * @param host
     * @param port
     * @return a Properties object
     */
    private Properties getServerProperties(String protocol, String host,
            String port) {
        Properties properties = new Properties();
 
        // server setting
        properties.put(String.format("mail.%s.host", protocol), host);
        properties.put(String.format("mail.%s.port", protocol), port);
 
        // SSL setting
        properties.setProperty(
                String.format("mail.%s.socketFactory.class", protocol),
                "javax.net.ssl.SSLSocketFactory");
        properties.setProperty(
                String.format("mail.%s.socketFactory.fallback", protocol),
                "false");
        properties.setProperty(
                String.format("mail.%s.socketFactory.port", protocol),
                String.valueOf(port));
 
        return properties;
    }
 
    /**
     * Downloads new messages and fetches details for each message.
     * @param protocol
     * @param host
     * @param port
     * @param userName
     * @param password
     */
    public void downloadEmails(String protocol, String host, String port,
            String userName, String password) {
        Properties properties = getServerProperties(protocol, host, port);
        Session session = Session.getDefaultInstance(properties);
 
        try {
            // connects to the message store
            Store store = session.getStore(protocol);
            store.connect(userName, password);
            
            // opens the inbox folder
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);
 
            // fetches new messages from server
            Message[] messages = folderInbox.getMessages();
 
            for (int i = 0; i < messages.length; i++) {
                Message msg = messages[i];
                Address[] fromAddress = msg.getFrom();
                String from = fromAddress[0].toString();
                String subject = msg.getSubject();
                String toList = parseAddresses(msg
                        .getRecipients(RecipientType.TO));
                String ccList = parseAddresses(msg
                        .getRecipients(RecipientType.CC));
                String sentDate = msg.getSentDate().toString();
 
                String contentType = msg.getContentType();
                String messageContent = "";
                
                if (contentType.contains("text/plain")
                        || contentType.contains("text/html")) {
                    try {
                        Object content = msg.getContent();
                        if (content != null) {
                            messageContent = content.toString();
                        }
                    } catch (Exception ex) {
                        messageContent = "[Error downloading content]";
                        ex.printStackTrace();
                    }
                }else {
                	  try {
						Multipart multipart = (Multipart) messages[i].getContent();

						  for (int j = 0; j < multipart.getCount(); j++) {

						      BodyPart bodyPart = multipart.getBodyPart(j);

						      String disposition = bodyPart.getDisposition();

						        if (disposition != null && (disposition.equalsIgnoreCase("ATTACHMENT"))) {
						            System.out.println("Mail have some attachment");
						            
						            DataHandler handler = bodyPart.getDataHandler();
						            System.out.println("file name : " + handler.getName());
						          }
						        else { 
						            System.out.println("Body: "+bodyPart.getContent());
						            messageContent= bodyPart.getContent().toString();
						          }
						  }
					} catch (IOException e) {
						e.printStackTrace();
					}
                }
                
                // print out details of each message
                System.out.println("Message #" + (i + 1) + ":");
                System.out.println("\t From: " + from);
                System.out.println("\t To: " + toList);
                System.out.println("\t CC: " + ccList);
                System.out.println("\t Subject: " + subject);
                System.out.println("\t Sent Date: " + sentDate);
                System.out.println("\t Message: " + messageContent);
            }
 
            // disconnect
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider for protocol: " + protocol);
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("Could not connect to the message store");
            ex.printStackTrace();
        }
    }
    
    private String parseAddresses(Address[] address) {
        String listAddress = "";
 
        if (address != null) {
            for (int i = 0; i < address.length; i++) {
                listAddress += address[i].toString() + ", ";
            }
        }
        if (listAddress.length() > 1) {
            listAddress = listAddress.substring(0, listAddress.length() - 2);
        }
 
        return listAddress;
    }
    
}
