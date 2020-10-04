package org.orangepalantir;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.Random;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Deque;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
public class ReactionDiffusion{
    final int l;
    public ReactionDiffusion( int l ){
        this.l = l;
        img = new BufferedImage(l, l, BufferedImage.TYPE_INT_ARGB);
        
    }
    
    double[] A;
    double[] B;
    final BufferedImage img;
    long seed = 2l;
    Random ng;
    JPanel panel;
    boolean running = true;
    Deque<Runnable> actions = new ConcurrentLinkedDeque<>();
    public void buildDisplay(){
        JFrame frame = new JFrame("reaction diffusion");
        panel = new JPanel(){
            
            @Override
            public void paintComponent(Graphics g){
                g.drawImage(img, 0, 0, this);
            }
            public Dimension getPreferredSize(){
                return new Dimension(l, l);
            }
        };
        
        panel.addMouseListener( new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent evt ){
                actions.add( () -> drop( evt.getX(), evt.getY() ) );
            }
        });
        
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        new Thread( this::simulate ).start();
        
    }
    
    public void drop( double x, double y){
        
        
        int r = 25;
        
        for(int i = 0; i<2*r; i++){
            for(int j = 0; j<2*r; j++){
                int x1 = (int)x - r/2 + i;
                int y1 = (int)y - r/2 + j;

                if(x1<0 || x1>=l || y1<0 || y1>=l){
                    continue;
                }
                double z = (-r/2 + i)*(-r/2 + i) + (-r/2 + j)*(-r/2 + j);
                A[x1 + l*y1] = (1 - Math.exp(-z/(r*r)*9) )* A[x1 + l*y1];
            }
        }
    }
    
    int substep = 10;
    public void simulate(){
        A = new double[l*l];
        B = new double[l*l];
        ng = new Random(seed);
        
        for(int i = 0; i<l*l; i++){
            A[i] = ng.nextDouble();
            B[i] = ng.nextDouble();
        }
        paint();
        double[] dA = new double[l*l];
        double[] dB = new double[l*l];
        double D_a = 1;
        double D_b = 0.5;
        double r_a =1;
        double r_b = -1;
        double r_ab = 1.0;
        double r_ba = -1;
        
        double dt = 0.01;
        int x,y;
        final int n = l*l;
        while(running){
            while(actions.size()>0){
                actions.removeFirst().run();
            }
            for(int j = 0; j<substep; j++){
                for(int i = 0; i<n; i++){
                    x = i%l;
                    y = i/l;
                
                    //here is the reaction diffusion equation.
                    dA[i] = D_a*(dx2(A, x, y) + dy2(A, x, y)) + r_a*c(A, x, y) + r_ba*c(B, x, y);
                    dB[i] = D_b*(dx2(B, x, y) + dy2(B, x, y)) + r_ab*c(A, x, y) + r_b*c(B, x, y);
                }
                
                for(int i = 0; i<n; i++){
                    A[i] += dA[i]*dt;
                    B[i] += dB[i]*dt;
                }
            }
            paint();
        }
    }
    double c(double[] arr, int x, int y){
        return arr[x + y*l];
    }

    double dx(  double[] arr, int x, int y ){
        if(x==0){
          return 0.5*( c(arr, x+1, y) - c(arr, x, y) );
        } else if(x==(l-1)){
          return 0.5*( c(arr, x, y) - c(arr, x - 1, y) );
        } else{
          return 0.5*( c(arr, x + 1, y) - c(arr, x - 1, y) );
        }
    }
    
    double dx2(  double[] arr, int x, int y ){
        if(x==0){
          return ( c(arr, x+1, y) - c(arr, x, y) );
        } else if(x==(l-1)){
          return ( - c(arr, x, y) + c(arr, x - 1, y) );
        } else{
          return ( c(arr, x + 1, y) + c(arr, x - 1, y) - 2*c(arr, x, y ) );
        }
    }
    
    double dy( double[] arr, int x, int y ){
        if(y==0){
          return 0.5*( c(arr, x, y+1) - c(arr, x, y) );
        } else if(y==(l-1)){
          return 0.5*( c(arr, x, y) - c(arr, x, y - 1) );
        } else{
          return 0.5*( c(arr, x, y + 1) - c(arr, x, y - 1) );
        }
    }
    
    double dy2( double[] arr, int x, int y){
        if(y==0){
          return ( c(arr, x, y+1) - c(arr, x, y) );
        } else if(y==(l-1)){
          return ( -c(arr, x, y) + c(arr, x, y - 1) );
        } else{
          return ( c(arr, x, y + 1) + c(arr, x, y - 1) - 2*c(arr, x, y) );
        }
    }
    
    public void paint(){
        int[] pixels = new int[l*l];
        for(int i = 0; i<l*l; i++){
            int g = (int)( 255 * A[i] );
            g = g<0?0:g;
            g = g>255?255:g;
            
            int b = (int)(255 *B[i]);
            b = b<0?0:b;
            b = b>255?255:b;
            
            int r = 0;
            if(A[i] > 1){ 
                r = (int)(255*( (A[i] - 1) / 10.0 ) );
                g = g-r;
            }
            if(B[i] > 1){ 
                r = (int) ( 255 * ( (B[i] - 1) / 10.0 ) );
                b = b-r;
            }

            
            pixels[i] = (255<<24) + ( r << 16 ) + (g << 8) + b;
            
        }
        img.setRGB(0, 0, l, l, pixels, 0, l);
        panel.repaint();
    }
    
    
    public static void main(String[] args){
        ReactionDiffusion rd = new ReactionDiffusion(1024);
        EventQueue.invokeLater( rd::buildDisplay );
    }

}
