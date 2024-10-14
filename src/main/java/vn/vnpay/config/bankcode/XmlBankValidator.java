package vn.vnpay.config.bankcode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XmlBankValidator {
    // Phương thức để đọc file XML và trả về danh sách các đối tượng Bank
    public List<Bank> readBankFromXml(String filePath) {
        List<Bank> bankList = new ArrayList<>();
        try {
            File file = new File(filePath);
            // Khởi tạo DocumentBuilderFactory và DocumentBuilder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Phân tích file XML và tạo Document đối tượng
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            // Lấy danh sách các phần tử <bank>
            NodeList nodeList = document.getElementsByTagName("bank");

            // Duyệt qua từng phần tử <bank> và lấy giá trị
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String bankCode = element.getElementsByTagName("bankCode").item(0).getTextContent();
                    String privateKey = element.getElementsByTagName("privateKey").item(0).getTextContent();

                    // Tạo đối tượng Bank và thêm vào danh sách
                    bankList.add(new Bank(bankCode, privateKey));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bankList;
    }

    // Phương thức để kiểm tra tính hợp lệ của bankCode và privateKey
    public boolean isValidBank(String bankCode, String privateKey, List<Bank> bankList) {
        for (Bank bank : bankList) {
            if (bank.getBankCode().equals(bankCode) && bank.getPrivateKey().equals(privateKey)) {
                return true;
            }
        }
        return false;
    }
}
