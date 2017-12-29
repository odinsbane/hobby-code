package org.orangepalantir;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class OilOnWater{
    int height = 32;
    int width = 32;
    double surfaceTension = -10.0;
    double diffusion = 0.0;
    double drag = 1.0;
    double viscosity = 0.0;
    double rad = 0.0;

    double dt = 0.0001;

    double[] concentration;
    double[] dc;
    double[] momentum;
    double[] dp;
    BufferedImage visibleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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

        concentration[width/2 + (height/2)*width] = 100;

        for(int i = 0;i<width; i++){

            for(int j = 0; j<5; j++){
                momentum[2*(i + (height/4+j)*width)] = 0;
                momentum[2*(i + (3*height/4+j)*width)] = 0;

            }

        }
    }
    void createVerticalStripes(){
        for(int i = 0; i<width/20; i++){
            for(int j = 0; j<height; j++){
                for(int k = 0; k<5; k++){
                    int x = 20*i + k;
                    int y = j;
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

    void getTwoDValue(int x, int y, double[] values, double[] result){
        y = y%height;
        x = x%width;
        x = x<0?x+width:x;
        y = y<0?y+height:y;
        int dex = 2*(x + y*width);
        result[0] = values[dex];
        result[1] = values[dex+1];
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
     * @param result { dux/dx, duy/dx, dux/dy, duy/dy }
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

    double curl(int x, int y, double[] values){
        double[] xp = new double[2];
        double[] xm = new double[2];
        getTwoDValue(x+1, y, values, xp);
        getTwoDValue(x-1, y, values, xm);

        double[] yp = new double[2];
        double[] ym = new double[2];
        getTwoDValue(x, y+1, values, yp);
        getTwoDValue(x, y-1, values, ym);

        return 0.5*( xp[1] - xm[1] - yp[0] + ym[0]);
    }

    void curl2(int x, int y, double[] values, double[] result){
        double[] xp = new double[2];
        double[] xm = new double[2];
        getTwoDValue(x+1, y, values, xp);
        getTwoDValue(x-1, y, values, xm);

        double[] yp = new double[2];
        double[] ym = new double[2];
        getTwoDValue(x, y+1, values, yp);
        getTwoDValue(x, y-1, values, ym);
        result[0] = 0.5*(xp[1] - xm[1]);
        result[1] = 0.5*(yp[0] - ym[0]);

    }


    void gradCurl(int x, int y, double[] values, double[] result){
        result[0] = curl(x+1, y, values) - curl(x-1, y, values);
        result[1] = curl(x, y+1, values) - curl(x, y-1, values);
    }

    public void step(){
        double[] deltac = new double[2];
        double[] d2p = new double[2];
        double[] gcp = new double[2];
        double[] delp = new double[4];;
        for(int i = 0; i<concentration.length; i++){
            int x = i%width;
            int y = i/width;
            firstDerivative1D(x, y, concentration, deltac);
            laplace2D(x, y, momentum, d2p);
            curl2(x, y, momentum, gcp);
            firstDerivative2D(x, y, momentum, delp);
            dp[2*i] = d2p[0]*viscosity +
                      delp[0]*momentum[2*i]*rad +
                      delp[2]*momentum[2*i+1]*0;
            dp[2*i+1] = d2p[1]*viscosity
                        + gcp[1]*rad
                        + delp[1]*momentum[2*i]*0
                        + delp[3]*momentum[2*i+1]*rad;

            dc[i] = diffusion*laplace1D(x, y, concentration)
                    - momentum[2*i]*deltac[0]
                    - momentum[2*i+1]*deltac[1]
                    + surfaceTension*deltac[0]
                    + surfaceTension*deltac[1];
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
        return new Color(r, g, B);
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
    public void startMainLoop(){
        Thread.currentThread().setName("main-loop");
        while(true){
            step();
            updateConcentrationImage();
        }
    }
    public void showFrame(){
        JFrame frame = new JFrame("Oil on Water");
        JPanel image = new JPanel(){
            double zoom = 20;
            @Override
            public void paintComponent(Graphics g){
                g.drawImage(
                        visibleImage,
                        0,
                        0,
                        (int)(visibleImage.getWidth(this)*zoom),
                        (int)(visibleImage.getHeight(this)*zoom),
                        this
                );
            }
            @Override
            public Dimension getPreferredSize(){
                return new Dimension(
                        (int)(visibleImage.getWidth(this)*zoom),
                        (int)(visibleImage.getHeight(this)*zoom)
                );
            }
        };

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
