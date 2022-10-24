import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class View extends JFrame implements ActionListener {

    JLabel spaceLabel = new JLabel();
    JLabel catNumLabel = new JLabel();
    JLabel dpiNumLabel = new JLabel();
    JLabel mailCountLabel = new JLabel();
    JTextField catNumTextField = new JTextField();
    JTextField dpiNumTextField = new JTextField();
    JTextField mailCountTextField = new JTextField();
    JCheckBox zlCheckBox = new JCheckBox("ЗЛ");
    JButton compileButton = new JButton();



    public View(String title) {
        this.setTitle(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //this.setLocationRelativeTo(null);
        this.setLocation(100, 120);
        this.setSize(200, 170);
        this.setPreferredSize(new Dimension(200, 170));
        this.setResizable(false);
        this.setLayout(new GridBagLayout());

        spaceLabel.setText("                                                           ");
        spaceLabel.setName("spaceLabel");

        dpiNumLabel.setText("dpi : ");
        dpiNumLabel.setName("dpiNumLabel");

        catNumLabel.setText("Номер папки : ");
        catNumLabel.setName("catNumLabel");

        mailCountLabel.setText("Количество писем : ");
        mailCountLabel.setName("mailCountLabel");

        dpiNumTextField.setText("150");
        mailCountTextField.setText("80");

        compileButton.setText("В архив");
        compileButton.setName("compileButton");
        compileButton.addActionListener(this);



        this.add(spaceLabel, new GridBagConstraints(
                0, 0, 2, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(catNumLabel, new GridBagConstraints(
                0, 1, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(catNumTextField, new GridBagConstraints(
                1, 1, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(mailCountLabel, new GridBagConstraints(
                0, 2, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(mailCountTextField, new GridBagConstraints(
                1, 2, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(dpiNumLabel, new GridBagConstraints(
                0, 3, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(dpiNumTextField, new GridBagConstraints(
                1, 3, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(zlCheckBox, new GridBagConstraints(
                0, 4, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

        this.add(compileButton, new GridBagConstraints(
                0, 5, 2, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
            if(zlCheckBox.isSelected())
            {
                Main.mailFolderName = "Plomb";
            }
            else
            {
                Main.mailFolderName = "Printer";
            }
            Main.catnum = this.catNumTextField.getText().trim();
            MSExchangeEmailService.NUMBER_EMAILS_FETCH = Integer.parseInt(this.mailCountTextField.getText().trim().replaceAll("[^0-9]]", ""));
            Main.dpi = this.dpiNumTextField.getText().trim();
            Main.starting();


            JOptionPane.showMessageDialog(null, "Всего : " + Main.allCount + "\nДобавлено заявлений : " + Main.successCount + "\nОшибок добавления : " + Main.errorCount, "Выполнено", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);

    }

}
