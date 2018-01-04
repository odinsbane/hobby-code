package org.orangepalantir;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.IntStream;

public class OilOnWater{
    int height = 512;
    int width = 512;
    double surfaceTension = 0.0;
    double diffusion = 0.0;
    double drag = 1.0;
    double viscosity = 1.0;
    double rad = 0.0;

    double dt = 0.001;

    double[] concentration;
    double[] dc;
    double[] momentum;
    double[] dp;
    BufferedImage visibleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    BufferedImage xMomentumImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    BufferedImage yMomentumImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    OilOnWater(){

    }


    /**
     * Starts simulation with the currently set parameters.
     */
    public void startSimulation(){
        concentration = new double[width*height];
        momentum = new double[2*width*height];
        dc = new double[width*height];
        dp = new double[2*width*height];


        createVerticalStripes();
        createHorizontalStripes();

        double m = 5;
        for(int i = 0; i<width; i++){
            for(int j = 0; j<height; j++){
                double px = m*Math.sin(i*Math.PI*1/width);
                double py = m*Math.sin(j*Math.PI*3/height);
                momentum[2*(i + j*width)] = py;
                momentum[2*(i + j*width) + 1] = px;
            }
        }

        /*
        for(int i = width/4;i<3*width/4; i++){

            for(int j = 0; j<10; j++){
                momentum[2*(i + (height/4+j)*width)] = 10;
                momentum[2*(i + (3*height/4+j)*width)] = 10;
            }

        }

        for(int i = 0; i<30; i++){
            for(int j = height/3; j<2*height/3; j++){
                momentum[2*(i + width/4 +(j)*width)+1] = 5;
                momentum[2*(i + 3*width/4 + (j)*width)+1] = 5;
            }
        }
        */

    }
    void createVerticalStripes(){
        for(int i = 0; i<width/20; i++){
            for(int j = 0; j<height; j++){
                for(int k = 0; k<1; k++){
                    int x = 20*i + k;
                    int y = j;
                    concentration[x + y*width] = 5;
                }
            }
        }
    }

    void createHorizontalStripes(){
        for(int i = 0; i<width; i++){
            for(int j = 0; j<height/20; j++){
                for(int k = 0; k<1; k++){
                    int x = i;
                    int y = j*20 + k;
                    concentration[x + y*width] = 5;
                }
            }
        }
    }
    double getOneDValue(int x, int y, double[] values){
        y = y%height;
        x = x%width;
        x = x<0?x+width:x;
        y = y<0?y+height:y;
        return values[x + y*width];
    }

    /**
     * Gets the value of a 2d vector field stored in a 1D array. The boundary conditions are assumed to be periodic.
     *
     * @param x
     * @param y
     * @param values 1d array storring 2, 2d values. eg {u00, v00, u10, v10, u20, v20, ... unm, vnm }
     * @param result {uxy, vxy}
     */
    void getTwoDValue(int x, int y, double[] values, double[] result){
        if(y<0) y = y+height;
        if(x<0) x = x+width;
        y = y%height;
        x = x%width;
        int dex = 2*(x + y*width);
        result[0] = values[dex];
        result[1] = values[dex+1];
    }

    /**
     * Calculates ({u, v} dot del){u,v} for finite elements.
     * @param x
     * @param y
     * @param values 2d array of vectors stored as a 1d array.
     * @param result {u du_dx + v du_dy , u dv_dx  + v dv_dy }
     */
    void convectiveTerm( int x, int y, double[] values, double[] result){
        double[] Vl = new double[2];
        double[] Vr = new double[2];
        double[] Vt = new double[2];
        double[] Vb = new double[2];
        double[] V = new double[2];

        getTwoDValue(x, y, values, V);
        getTwoDValue(x+1, y, values, Vr);
        getTwoDValue(x-1, y, values, Vl);
        getTwoDValue(x, y+1, values, Vt);
        getTwoDValue(x, y-1, values, Vb);
        double vxAve = (V[0] + Vl[0] + Vr[0])/3;
        double vyAve = (V[1] + Vl[1] + Vr[1])/3;
        //flow in from left - flow out from right
        result[0] = vxAve*(Vl[0] - Vr[0]) + vyAve*(Vb[0] - Vt[0]);

        result[1] = vxAve*(Vl[1] - Vr[1]) + vyAve*(Vb[1] - Vt[1]);

    }

    void firstDerivative1D(int x, int y, double[] values, double[] result){
        result[0] = 0.5*(getOneDValue(x+1, y, values) - getOneDValue(x-1, y, values));
        result[1] = 0.5*(getOneDValue(x, y+1, values) - getOneDValue(x, y-1, values));
    }

    double laplace1D(int x, int y, double[] values){
        double c = getOneDValue(x, y, values);
        double dc_dxdx = (getOneDValue(x+1, y, values) + getOneDValue(x-1, y, values) - 2*c);
        double dc_dydy = (getOneDValue(x, y+1, values) + getOneDValue(x, y-1, values) - 2*c);

        return dc_dxdx + dc_dydy;
    }

    /**
     * Values contains a vector field of u,v at each point.
     * @param x
     * @param y
     * @param values
     * @param result
     */
    void laplace2D(int x, int y, double[] values, double[] result){
        double[] up2 = new double[2];
        double[] um2 = new double[2];
        double[] u = new double[2];
        getTwoDValue(x+1, y, values, up2);
        getTwoDValue(x-1, y, values, um2);
        getTwoDValue(x, y, values, u);

        double du_dxdx = (up2[0] + um2[0] - 2*u[0]);
        double dv_dxdx = (up2[1] + um2[1] - 2*u[1]);

        getTwoDValue(x, y+1, values, up2);
        getTwoDValue(x, y-1, values, um2);

        double du_dydy = (up2[0] + um2[0] - 2*u[0]);
        double dv_dydy = (up2[1] + um2[1] - 2*u[1]);

        result[0] = du_dxdx + du_dydy;
        result[1] = dv_dxdx + dv_dydy;

    }

    /**
     *
     * Derivative of a vector field.
     *
     * @param x
     * @param y
     * @param values
     * @param result { du/dx, dv/dx, du/dy, dv/dy }
     *
     */
    void firstDerivative2D(int x, int y, double[] values, double[] result){
        double[] p = new double[2];
        double[] m = new double[2];

        getTwoDValue(x+1, y, values, p);
        getTwoDValue(x-1, y, values, m);
        result[0] = 0.5*(p[0] - m[0]);
        result[1] = 0.5*(p[1] - m[1]);
        getTwoDValue(x, y+1, values, p);
        getTwoDValue(x, y-1, values, m);
        result[2] = 0.5*(p[0] - m[0]);
        result[3] = 0.5*(p[1] - m[1]);

    }




    public void step(){
        double[] deltac = new double[2];
        double[] d2p = new double[2];
        double[] pdotdelp = new double[2];
        for(int i = 0; i<concentration.length; i++){

            int x = i%width;
            int y = i/width;
            convectiveTerm(x, y, momentum, pdotdelp);
            laplace2D(x, y, momentum, d2p);
            dp[2*i] = d2p[0]*viscosity +
                      pdotdelp[0];
            dp[2*i+1] = d2p[1]*viscosity
                        + pdotdelp[1];
            firstDerivative1D(x, y, concentration, deltac);
            dc[i] = -momentum[2*i]*deltac[0] + -momentum[2*i+1]*deltac[1];
        }



        for(int i = 0; i<concentration.length; i++){
            concentration[i] += dc[i]*dt;
            concentration[i] = concentration[i]>0?concentration[i]:0;
            momentum[2*i] += dp[2*i]*dt;
            momentum[2*i+1] += dp[2*i+1]*dt;
        }
    }

    Color empty = Color.WHITE;
    Color full = Color.RED;

    double bias = 1.0;
    Color interpolate(Color a, Color b, double f){
        int r = interpolate(a.getRed(), b.getRed(), f);
        int g = interpolate(a.getGreen(), b.getGreen(), f);
        int B = interpolate(a.getBlue(), b.getBlue(), f);
        try{
            return new Color(r, g, B);
        } catch(Exception e){
            System.out.println(f);
        }
        return Color.WHITE;
    }

    int interpolate(int a, int b, double f){
        return (int)((a-b)*f + b);
    }

    public void updateConcentrationImage(){
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        for(int i = 0; i<concentration.length; i++){
            int c;
            if(concentration[i]>bias){
                c = full.getRGB();
            } else{
                c = interpolate(full, empty, concentration[i]/bias).getRGB();
            }
            img.setRGB(i%width, i/width, c);
        }
        visibleImage = img;
    }


    public void updateMomentumImage(){

        BufferedImage xImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage yImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double min = Double.MAX_VALUE;
        double max = -min;
        double sum = 0;
        for(int i = 0; i<momentum.length; i++){
            double p = momentum[i];
            min = min<p?min:p;
            max = p>max?p:max;
            sum += p;
        }

        if(min>=0){
            min = -1;
        }
        if(max<=0){
            max = 1;
        }


        for(int i = 0; i<concentration.length; i++){

            int c;
            double p = momentum[2*i];
            if(p>0){
                c = interpolate(Color.RED, Color.BLACK, p/max).getRGB();
            } else{
                c = interpolate(Color.BLUE, Color.BLACK, p/min).getRGB();
            }
            xImage.setRGB(i%width, i/width, c);

            p = momentum[2*i + 1];
            if(p>0){
                c = interpolate(Color.RED, Color.BLACK, p/max).getRGB();
            } else{
                c = interpolate(Color.BLUE, Color.BLACK, p/min).getRGB();
            }
            yImage.setRGB(i%width, i/width, c);

        }
        xMomentumImage = xImage;
        yMomentumImage = yImage;
    }

    public void startMainLoop(){
        Thread.currentThread().setName("main-loop");
        while(true){
            step();
            updateConcentrationImage();
            updateMomentumImage();
        }
    }
    double zoom = 1;
    public void showFrame(){
        JFrame frame = new JFrame("Oil on Water");
        JPanel image = new JPanel(){

            @Override
            public void paintComponent(Graphics g){
                g.drawImage(
                        visibleImage,
                        (int)(xMomentumImage.getWidth(this)*zoom),
                        0,
                        (int)(visibleImage.getWidth(this)*zoom),
                        (int)(visibleImage.getHeight(this)*zoom),
                        this
                );

                g.drawImage(
                        xMomentumImage,
                        0,
                        0,
                        (int)(xMomentumImage.getWidth(this)*zoom),
                        (int)(xMomentumImage.getHeight(this)*zoom),
                        this
                );
                g.drawImage(
                        yMomentumImage,
                        0,
                        (int)(xMomentumImage.getHeight(this)*zoom),
                        (int)(yMomentumImage.getWidth(this)*zoom),
                        (int)(yMomentumImage.getHeight(this)*zoom),
                        this
                );
            }
            @Override
            public Dimension getPreferredSize(){
                return new Dimension(
                        (int)(2*visibleImage.getWidth(this)*zoom),
                        (int)(2*visibleImage.getHeight(this)*zoom)
                );
            }
        };

        image.addMouseWheelListener(new MouseWheelListener(){

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {

                if(e.getWheelRotation()>0){
                    zoom = zoom*0.99;
                } else{
                    zoom = zoom*1.01;
                }
                image.invalidate();
                frame.validate();
            }

        });

        Timer time = new Timer(30, evt->{ image.repaint();});
        time.start();

        frame.setContentPane(image);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args){
        OilOnWater sim = new OilOnWater();
        sim.startSimulation();
        new Thread(sim::startMainLoop).start();
        EventQueue.invokeLater(sim::showFrame);
    }

    //test everything!!!


}