package nomivore;

import org.powerbot.script.rt4.ClientContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class LumbyFireCookGUI extends JFrame {
    private boolean done = false;
    private JFrame frame = new JFrame("Lumby Fire & Cook");
    private JButton button1 = new JButton("Start");
    private JList<String> list1 = new JList<>();
    private JList<String> list2 = new JList<>();
    private DefaultListModel<String> listModel1 = new DefaultListModel<>();
    private DefaultListModel<String> listModel2 = new DefaultListModel<>();

    public LumbyFireCookGUI(ClientContext ctx) {
        setLayout(new BorderLayout());
        Container pane = frame.getContentPane();
        setContentPane(pane);
        button1.setMaximumSize(new Dimension(200, 50));
        button1.setPreferredSize(new Dimension(200, 50));
        pane.add(button1, BorderLayout.PAGE_END);
        list1.setPreferredSize(new Dimension(100, 250));
        list1.setMaximumSize(new Dimension(100, 250));
        pane.add(list1, BorderLayout.LINE_START);
        list2.setPreferredSize(new Dimension(100, 250));
        list1.setMaximumSize(new Dimension(100, 250));
        pane.add(list2, BorderLayout.LINE_END);
        pack();
        setVisible(true);
        setLocationRelativeTo(null);
        list1.setModel(listModel1);
        list2.setModel(listModel2);

        List<foodItem> foodList = makeList();

        for (foodItem it : foodList) {
            listModel2.addElement(it.name);
        }

        list1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 1) {
                    int index = list1.locationToIndex(e.getPoint());
                    String elm = listModel1.elementAt(index);
                    listModel1.removeElementAt(index);
                    listModel2.addElement(elm);
                }
            }
        });

        list2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 1) {
                    int index = list2.locationToIndex(e.getPoint());
                    String elm = listModel2.elementAt(index);
                    listModel1.addElement(elm);
                    listModel2.removeElementAt(index);
                }
            }
        });


        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(int i = 0; i< list1.getModel().getSize();i++){
//                    System.out.println(list1.getModel().getElementAt(i));
                    for (foodItem it : foodList) {
                        if (it.name.equals(list1.getModel().getElementAt(i))) {
                            LumbyFireCook.foodIDs.add(it.ID);
                        }
                    }
                }
                done = true;
                dispose();
            }
        });
    }

    public boolean done() {
        return done;
    }

    private class foodItem {
        String name;
        int ID;

        private foodItem(String name, int ID) {
            this.name = name;
            this.ID = ID;
        }
    }

    private List<foodItem> makeList() {
        List<foodItem> list = new ArrayList<>();
        list.add(new foodItem("Chicken", ID.RAW_CHICKEN));
        list.add(new foodItem("Beef", ID.RAW_BEEF));
        list.add(new foodItem("Shrimp", ID.RAW_SHRIMP));
        list.add(new foodItem("Anchovy", ID.RAW_ANCHOVY));
        list.add(new foodItem("Sardine", ID.RAW_SARDINE));
        list.add(new foodItem("Herring", ID.RAW_HERRING));
        list.add(new foodItem("Trout", ID.RAW_TROUT));
        list.add(new foodItem("Pike", ID.RAW_PIKE));
        list.add(new foodItem("Salmon", ID.RAW_SALMON));
        list.add(new foodItem("Tuna", ID.RAW_TUNA));
        list.add(new foodItem("Lobster", ID.RAW_LOBSTER));
        list.add(new foodItem("Swordfish", ID.RAW_SWORDFISH));
        return list;
    }
}