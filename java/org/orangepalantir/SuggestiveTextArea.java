package org.orangepalantir;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 18.10.17.
 */
public class SuggestiveTextArea {
    List<String> possible = new ArrayList<>();
    int MAX_SHOWN = 10;
    Popup lastPopUp;
    JTextArea buildTextArea(){
        JTextArea area = new JTextArea(20, 20);

        InputMap im = area.getInputMap();
        KeyStroke tab = KeyStroke.getKeyStroke("TAB");

        area.getActionMap().put(im.get(tab), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSuggestions(area);
            }
        });
        area.addCaretListener(evt->{
            if(lastPopUp!=null){

                lastPopUp.hide();
                lastPopUp = null;
            }
        });
        return area;
    }

    public void showSuggestions(JTextArea input) {
        Caret caret = input.getCaret();
        Document doc = input.getDocument();
        int loc = caret.getMark();
        int len = doc.getLength();


        try {
            //int end = Utilities.getWordEnd(input, loc);
            int starting = Utilities.getWordStart(input, loc);
            int end = Utilities.getWordEnd(input, loc);
            int l = loc - starting;
            if (l == 0) {
                if (len - loc != 0) {

                    end = Utilities.getWordEnd(input, loc - 1);
                    starting = Utilities.getWordStart(input, loc - 1);
                    if (end == loc) {
                        l = loc - starting;
                    }
                } else {
                    return;
                }
            }
            String partial = input.getText(starting, l);
            List<String> suggestions = getSuggestions(partial);
            suggestions.forEach(System.out::println);
            if(suggestions.size()==MAX_SHOWN){
                suggestions.add("...");
            }
            JList<String> list = new JList<>(new ListModel<String>(){

                @Override
                public int getSize() {
                    return suggestions.size();
                }

                @Override
                public String getElementAt(int index) {
                    return suggestions.get(index);
                }

                @Override
                public void addListDataListener(ListDataListener l) {
                }

                @Override
                public void removeListDataListener(ListDataListener l) {

                }
            });

            Point pt = caret.getMagicCaretPosition();

            SwingUtilities.convertPointToScreen(pt, input);
            Popup pop = PopupFactory.getSharedInstance().getPopup(input, list, pt.x, pt.y);
            pop.show();
            final int fin = end;
            list.addListSelectionListener(evt->{
                int dex = evt.getFirstIndex();
                if(dex<MAX_SHOWN){
                    String rep = suggestions.get(dex);
                    input.replaceRange(rep, loc, fin);

                }
                pop.hide();
            });
            if(lastPopUp!=null){
                lastPopUp.hide();
            }
            lastPopUp = pop;

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public List<String> getSuggestions(String partial){
        return possible.stream().filter(
                s->s.startsWith(partial)
        ).map(s->s.substring(partial.length())).filter(s->!s.isEmpty()).limit(MAX_SHOWN).collect(Collectors.toList());
    }

    public void addPossibilities(List<String> possible){
        this.possible.addAll(possible);
    }

    public void buildGui(){

        try {
            try(BufferedReader reads = Files.newBufferedReader(Paths.get("/usr/share/dict/words"))){
                addPossibilities(reads.lines().collect(Collectors.toList()));
            }
        } catch (IOException e) {
            addPossibilities(Arrays.asList("could", "not", "load", "file"));
            e.printStackTrace();
        }

        JFrame frame = new JFrame();

        frame.add(buildTextArea());

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }
    public static void main(String[] args){

        EventQueue.invokeLater(()->{
            new SuggestiveTextArea().buildGui();
        });
    }

}
