package vn.vnpay.config.bankcode;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
@Slf4j
public class XmlBankValidator {
    private static final String currentDirectory = System.getProperty("user.dir");
    private static final String PATH_BANK_CODE = currentDirectory + "/src/main/resources/bankcode.xml";
    private final List<Bank> bankList;
    // Constructor để tải danh sách ngân hàng khi ứng dụng khởi động
    public XmlBankValidator() {
        bankList = loadBankList(); // Tải danh sách ngân hàng khi khởi động
    }

    // Phương thức để tải danh sách ngân hàng từ file XML
    private List<Bank> loadBankList() {
        log.info("Starting to load bank list from XML file.");
        List<Bank> bankList = new ArrayList<>();
        try {
            File file = new File(PATH_BANK_CODE);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("bank");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String bankCode = element.getElementsByTagName("bankCode").item(0).getTextContent();
                    String privateKey = element.getElementsByTagName("privateKey").item(0).getTextContent();
                    bankList.add(new Bank(bankCode, privateKey));
                }
            }
            log.info("Successfully loaded {} banks.", bankList.size());
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Error loading bank list from XML file: {}", e.getMessage());
        }
        return bankList;
    }

    // Phương thức để kiểm tra tính hợp lệ của bankCode và privateKey
    public boolean isValidBank(String bankCode, String privateKey) {
        for (Bank bank : bankList) {
            if (bank.getBankCode().equals(bankCode) && bank.getPrivateKey().equals(privateKey)) {
                return true;
            }
        }
        return false;
    }
}
