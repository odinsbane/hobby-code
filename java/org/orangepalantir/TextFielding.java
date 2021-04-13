package org.orangepalantir;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class TextFielding{

    static class EndFilter extends DocumentFilter{
      
        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException{
            replace(fb, offset, 0, string, attr);
        }
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException{
            //System.out.println(fb + ", " + offset + ", " + length );
            //super.remove(fb, offset, length);
            
            if(length > 1) return;
            
            Document d = fb.getDocument();
            int docLength = d.getLength();
            
            String left = docLength == 4 ? "0" : d.getText(0, docLength - 4);
            String shiftl = d.getText(docLength -4, 1);
            String shiftr = d.getText(docLength - 2, 1);
            String result = left + "." + shiftl + shiftr;
            super.replace(fb, 0, docLength, result, null);
            
        }
        
        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException{
            if(length > 0 || text.length() != 1 || !Character.isDigit( text.charAt(0) ) ){
                return;
            }
            
            Document d = fb.getDocument();
            int docLength = d.getLength();
            
            //build the string.
            String left = d.getText( 0, docLength - 3);
            if( left.equals("0") ) left = "";
            String crossing = d.getText( docLength - 2, 1);
            String shift = d.getText( docLength -1, 1);
            String result = left + crossing + "." + shift + text;
            super.replace(fb, 0, docLength, result, attrs);
        }
    
    }

    public static void main(String[] args){
        JFrame frame = new JFrame("text field");
        JTextField field = new JTextField(10);
        
        field.setHorizontalAlignment( JTextField.RIGHT );
        field.setText("0.00");
        ((AbstractDocument)field.getDocument()).setDocumentFilter(new EndFilter());
        
        frame.add(field);
        
        
        
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
