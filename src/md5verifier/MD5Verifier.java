package md5verifier;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class MD5Verifier {

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static File selectFiles(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
//        chooser.setMultiSelectionEnabled(true);
        while (true) {
            int returnVal = chooser.showOpenDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                return chooser.getSelectedFile();
            } else {
                System.out.println("Cancel??");
                return null;
            }
        }
    }

    public static JFrame createBasicFrame() {
        JFrame frame = new JFrame("Verify MD5 Sums");
        frame.setBounds(50, 50, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    public static JEditorPane prepareOutputWindow(JFrame frame) {
        JEditorPane outPane = new JEditorPane();
        outPane.setContentType("text/html");
        outPane.setEditorKit(new HTMLEditorKit());

        outPane.setAutoscrolls(true);
        outPane.setEditable(false);
        JScrollPane jscroll = new JScrollPane(outPane);
        frame.add(jscroll, BorderLayout.CENTER);
        jscroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        jscroll.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        return outPane;
    }

    public static JTextArea preparePendingListWindow(JFrame frame) {
        JTextArea jta = new JTextArea();
        jta.setPreferredSize(new Dimension(800, 200));
        jta.setAutoscrolls(true);
        jta.setEditable(false);
        JScrollPane scroll = new JScrollPane(jta);
        scroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(scroll, BorderLayout.NORTH);
        return jta;
    }

//    private static final SimpleAttributeSet PLAIN = new SimpleAttributeSet();
//    private static final SimpleAttributeSet RED = new SimpleAttributeSet();
//    static { StyleConstants.setForeground(RED, Color.RED); }
    public static void writeToOutput(final JEditorPane outPane,
            final String text, final boolean alert) {
        final String openPara = alert ? "<p color=\"red\"><em>" : "<p>";
        final String closePara = alert ? "</em></p>" : "</p>";
//        final SimpleAttributeSet attribs = alert ? RED : PLAIN;

        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        Document d = outPane.getDocument();
                        HTMLEditorKit edKit = (HTMLEditorKit) (outPane.getEditorKit());
                        int end = d.getEndPosition().getOffset();
                        try {
                            edKit.insertHTML((HTMLDocument) d, d.getLength(),
                                    openPara + text + closePara,
                                    0, 0, null);
//                        d.insertString(end, text, attribs);
                        } catch (Exception ex) {
                            System.out.println("Ouch! " + ex);
                        }

                    }
                }
        );
    }

    public static void main(String[] args) throws Exception {
        JFrame frame = createBasicFrame();
        final JEditorPane outPane = prepareOutputWindow(frame);
        frame.setVisible(true);
        final JTextArea fileList = preparePendingListWindow(frame);
        
        List<File> sumFiles = new ArrayList<File>();
        File sumFile = null;
        while ((sumFile = selectFiles(frame)) != null) {
            System.out.println("Selected " + sumFile);
            sumFiles.add(sumFile);
            fileList.append(sumFile.toString() + "\n");
        }

        boolean failures = false;
        for (File file : sumFiles) {
            byte[] buffer = new byte[4096 * 1024];
            int count = 0;
            System.out.println("Checking " + file.getAbsoluteFile().getCanonicalPath());
            Path f = file.toPath();
            Path dir = f.getParent();
            BufferedReader in = Files.newBufferedReader(f);
            String fileLine;
            while ((fileLine = in.readLine()) != null) {
                String sum = fileLine.substring(0, 32);
                String target = fileLine.substring(34);
                Path p = dir.resolve(target);
                InputStream is = Files.newInputStream(p);
                MessageDigest digester = MessageDigest.getInstance("MD5");
                while ((count = is.read(buffer)) != -1) {
                    digester.update(buffer, 0, count);
                }
                byte[] result = digester.digest();
                String computed = bytesToHex(result);
                if (sum.equalsIgnoreCase(computed)) {
                    writeToOutput(outPane, p + " OK", false);
                } else {
                    writeToOutput(outPane, p + " FAILED "
                            + "sum is " + computed
                            + " file claims " + sum, true);
                    failures = true;
                }
            }
        }
        if (failures) {
            writeToOutput(outPane, "ERRORS DETECTED!", true);
        } else {
            writeToOutput(outPane, "All good!", false);
        }
    }
}
