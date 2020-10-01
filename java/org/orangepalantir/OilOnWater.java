package org.orangepalantir;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * The premise of this simulation is to create patterns in the oil cause by the flowing of water beneath. The oil is
 * a collection of particles that move due to the flow of water underneath.
 *
 */
public class OilOnWater{
    int height = 1024;
    int width = 1024;
    double surfaceTension = 30000.0;
    double dispersion = 100.0;
    double couple = 200.0;
    double viscosity = 1.0;
    double rad = 0.0;
    double time = 0;
    double dt = 1e-3;
    int dripRadius = 25;
    Random ng = new Random();
    /* x,y coordinates of oil particles. */
    double[] oil;
    int particles = 0x8000;
    /* height x width arrays for handling field components. */
    double[] concentration;
    double[] momentum;
    double[] dp;


    double maxConcentration = 1.1;
    long realtime;
    Gui gui = new Gui();
    final static boolean NAN_CHECK = false;
    final static boolean UPDATE_MOMENTUM = false ;

    OilOnWater(){

    }


    /**
     * Starts simulation with the currently set parameters.
     */
    public void startSimulation(){
        realtime = System.currentTimeMillis();
        concentration = new double[width*height];
        oil = new double[2*particles];

        momentum = new double[2*width*height];

        dp = new double[2*width*height];


        //createVerticalStripes(0);
        //createHorizontalStripes(particles/2);
        createSpots();

        momentumStrips();
        MomentumTorus torus = new MomentumTorus(width, height);
        torus.radius = 80;
        torus.thickness = 32;
        torus.m = -0.5;
        for(int i = 0; i<2; i++){
            for(int j = 0; j<2; j++){
                torus.momentumTorus((2*i+1)*width/4.0, (2*j+1)*height/4.0, momentum);
            }
        }

        //torus.radius = 64;
        //torus.thickness = 32;
        //torus.m = 5;
        //torus.momentumTorus(width/2.0, height/2, momentum);
    }

    public void momentumStrips(){
        double m = 2;
        for(int i = 0; i<width; i++){
            for(int j = 0; j<height; j++){
                double px = m*Math.sin((i)*Math.PI*4/width);
                double py = m*Math.sin(j*Math.PI*4/height);
                momentum[2*(i + j*width)] = py;
                momentum[2*(i + j*width) + 1] = 0;
            }
        }
    }

    /**
     * create N vertical stripes with half of the particles.
     */
    void createVerticalStripes(int start){
        int N = 64;
        int count = particles/2;

        int particlesPerColumn = count/N;
        double dy = height*1.0/particlesPerColumn;
        double dx = width*1.0/N;
        int laid = 0;
        //column
        for(int i = 0; i<N; i++){
            for(int j = 0; j<particlesPerColumn; j++){
                oil[2*(start + laid)] = dx*i;
                oil[2*(start + laid) + 1] = dy*j;
                laid++;
            }
        }



    }

    /**
     * Creates spots of particles.
     *
     */
    void createSpots(){
        int n = 1;
        int N = n*n;
        double radius = 128;

        int spotsPerDot = particles/N;
        double dx = width/n;
        double dy = height/n;

        int count = 0;
        for(int i = 0; i<N; i++){
            double cx = ((i%n) + 0.5)*dx;
            double cy = ((i/n) + 0.5)*dy;

            for(int j = 0; j<spotsPerDot; j++){
                double r = Math.sqrt(Math.random())*radius;
                double theta = Math.random()*Math.PI*2;
                oil[2*count] = Math.sin(theta)*r + cx;
                oil[2*count+1] = Math.cos(theta)*r + cy;
                count++;
            }

        }


    }
    /**
     * create horizontal strings with half of the particles.
     */
    void createHorizontalStripes(int start){
        int N=8;

        int count = particles/2;
        int particlesPerRow = count/N;
        double dy = height*1.0/N;
        double dx = width*1.0/particlesPerRow;

        int laid = 0;
        for(int i = 0; i<N; i++){
            for(int j = 0; j<particlesPerRow; j++){
                oil[2*(laid + start)] = dx*j;
                oil[2*(laid + start) + 1] = dy*i;
                laid++;
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

        if(NAN_CHECK){
            if(Double.isNaN(result[0]) || Double.isNaN(result[1])){
                throw new RuntimeException("Its a NaN fire! " + x + y);
            }
        }
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


        if(NAN_CHECK){
            if(Double.isNaN(result[0]) || Double.isNaN(result[1])){
                throw new RuntimeException("Its a NaN fire! " + x + y);
            }
        }

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

        if(NAN_CHECK){
            if(Double.isNaN(result[0]) || Double.isNaN(result[1])){
                throw new RuntimeException("Its a NaN fire! " + x + y);
            }
        }
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
    final static double isqt2 = 1/Math.sqrt(2);
    /**
     * Gets a force that will push particles as if by diffusion.
     * @param x
     * @param y
     * @param values
     * @param result
     */
    void getDispersion(double x, double y, double[] values, double[] result){
        double cf = 0;
        double cback = 0;
        double ct = 0;
        double cbot = 0;

        int y0 = (int)(y + 0.5);
        int x0 = (int)(x + 0.5);

        int xf = x0 + 1;
        int xb = x0 - 1;

        int yt = y0 - 1;
        int yb = y0 + 1;

        for(int i = 0; i<3; i++){
            double c = getOneDValue(xf, y0 - 1 +i, values);
            if(c > maxConcentration){
                cf += (c - maxConcentration);
            }
            c = getOneDValue(xb, y0 - 1 + i, values);
            if ( c > maxConcentration){
                cback += (c - maxConcentration);
            }

            c = getOneDValue(x0 -1 + i, yt, values);
            if ( c > maxConcentration){
                ct += (c - maxConcentration);
            }
            c = getOneDValue(x0 -1 + i, yb, values);
            if ( c > maxConcentration){
                cbot += (c - maxConcentration);
            }


        }



        result[0] = - cf + cback; //away from high concentration.
        result[1] = ct - cbot;
        if(Double.isNaN(result[0]) || Double.isNaN(result[1])){
            throw new RuntimeException("Dispersive NaN is a fire! " + x + y);
        }
    }
    public void takeSnapShot(){
        if(snapshots > 1000){
            return;
        }
        try {
            ImageIO.write(gui.visibleImage, "PNG", new File(String.format("snap-%04d.png", snapshots)));
            snapshots++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static double constrain( double v, double limit){
        if(v >= limit){
            while( v >= limit ){
                v = v - limit;
            }
        } else if( v < 0){
            while( v < 0){
                v = v + limit;
            }
        }

        return v;
    }
    public void step(){
        double[] d2p = new double[2];
        double[] pdotdelp = new double[2];

        //update momentum field.
        for(int i = 0; i<concentration.length; i++){

            if(UPDATE_MOMENTUM) {
                int x = i % width;
                int y = i / width;

                convectiveTerm(x, y, momentum, pdotdelp);
                laplace2D(x, y, momentum, d2p);
                dp[2 * i] = d2p[0] * viscosity +
                        pdotdelp[0];
                dp[2 * i + 1] = d2p[1] * viscosity
                        + pdotdelp[1];
            }
            concentration[i] = 0;
        }

        //update concentration.
        for(int i =0; i<particles; i++){
            double x = oil[2*i];
            double y = oil[2*i+1];
            drip(x, y);
            //concentration[(int)x + (int)y*width] += 1;

        }

        double[] p = new double[2];
        double[] t = new double[2];
        double[] d = new double[2];
        //update particles.
        for(int i =0; i<particles; i++){
            double x = oil[2*i];
            double y = oil[2*i+1];

            getTwoDValue((int)x, (int)y, momentum, p);
            getSurfaceTension(x, y, concentration, t);
            getDispersion(x, y, concentration, d);
            x = x + (couple*p[0] + surfaceTension*t[0] + dispersion*d[0])*dt;
            y = y + (couple*p[1] + surfaceTension*t[1] + dispersion*d[1])*dt;

            // only move to valid positions.
            x = constrain(x, width);
            y = constrain(y, height);


            oil[2*i] = x;
            oil[2*i+1] = y;

        }

        if(UPDATE_MOMENTUM) {
            for (int i = 0; i < concentration.length; i++) {
                momentum[2 * i] += dp[2 * i] * dt;
                momentum[2 * i + 1] += dp[2 * i + 1] * dt;
            }
        }
        time += dt;

    }


    void drip(double x, double y){
        int radius = dripRadius;
        int x0 = (int) (x + 0.5);
        int y0 = (int) (y + 0.5);
        for(int i = 0; i<radius*2 + 1; i++ ){
            for( int j = 0; j<radius*2 + 1; j++){
                int xi = x0 + i - radius;
                int yi = y0 + j - radius;

                if( xi >= width ){
                    while(xi >= width) {
                        xi = xi - width;
                    }
                } else if( xi < 0 ){
                    while(xi<0) {
                        xi = width + xi;
                    }
                }

                if( yi >= height ){
                    while(yi >= height) {
                        yi = yi - height;
                    }
                } else if( yi < 0 ){
                    while(yi<0) {
                        yi = height + yi;
                    }
                }

                double di = i - radius;
                di = di<0?-di:di;
                double dj = j - radius;
                dj = dj<0?-dj:dj;
                double con = concentration[xi + width*yi] + ( 1.0 / ( 1.0 + di*di + dj*dj));

                concentration[xi + width*yi] = con;
            }

        }

    }


    public void getSurfaceTension(double x, double y, double[] concentration, double[] v){
        v[0] = 0;
        v[1] = 0;

        double cb = getOneDValue((int)(x-0.5), (int)(y+0.5), concentration);
        cb = cb>maxConcentration?maxConcentration : cb;
        double cf = getOneDValue((int)(x+1.5), (int)(y+0.5), concentration);
        cf = cf>maxConcentration?maxConcentration : cf;
        double ct = getOneDValue((int)(x + 0.5), (int) (y - 0.5), concentration );
        ct = ct > maxConcentration ? maxConcentration : ct;
        double cbot = getOneDValue((int)(x + 0.5), (int)(y + 1.5), concentration);
        cbot = cbot > maxConcentration ? maxConcentration : cbot;
        v[0] = -cb + cf; //Go towards the high concentration.
        v[1] = -ct + cbot;


    }


    int stepsTaken = 0;
    int snapshots = 0;
    int period = (int)(0.01/dt);

    public void startMainLoop(){
        Thread.currentThread().setName("main-loop");
        while(true){
            step();
            gui.updateConcentrationImage();
            gui.updateMomentumImage();
            if(stepsTaken++ % period == 0){
                takeSnapShot();
            }
        }
    }
    double zoom = 1;
    void showFrame(){
        gui.showFrame();
    }

    public static void main(String[] args){
        OilOnWater sim = new OilOnWater();
        sim.startSimulation();
        new Thread(sim::startMainLoop).start();
        //EventQueue.invokeLater(sim::showFrame);
    }
    //test everything!!!

    class Gui{
        BufferedImage visibleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage xMomentumImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage yMomentumImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Color pxPlus = Color.ORANGE;
        Color pxMinus = Color.RED;
        Color pyPlus = Color.YELLOW;
        Color pyMinus = Color.BLUE;

        Color empty = Color.BLACK;
        Color full = Color.RED;

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
                    } else if(e.getWheelRotation()<0){
                        zoom = zoom*1.01;
                    }
                    image.invalidate();
                    frame.validate();
                }

            });

            Timer time = new Timer(30, evt->{ image.repaint();});
            time.start();

            frame.setContentPane(new JScrollPane(image));
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }


        int interpolate(int a, int b, double f){
            return (int)((a-b)*f + b);
        }

        Color interpolate(Color a, Color b, double f){
            f = f>1?1:f;
            f = f<0?0:f;
            int r = interpolate(a.getRed(), b.getRed(), f);
            int g = interpolate(a.getGreen(), b.getGreen(), f);
            int B = interpolate(a.getBlue(), b.getBlue(), f);
            try{
                return new Color(r, g, B);
            } catch(Exception e){
                System.out.println(f);
            }
            return empty;
        }

        public void updateConcentrationImage(){
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            for(int i = 0; i<concentration.length; i++){
                int c;
                if(concentration[i]>=maxConcentration){
                    c = full.getRGB();
                } else{
                    c = interpolate(full, empty, concentration[i]/maxConcentration).getRGB();
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
                Color cx, cy;
                if(p>0){
                    cx =  interpolate(pxPlus, empty, p/max);
                } else{
                    cx = interpolate(pxMinus, empty, p/min);
                }

                xImage.setRGB(i%width, i/width, cx.getRGB());


                p = momentum[2*i + 1];
                if(p>0){
                    cy = interpolate(pyPlus, empty, p/max);
                } else{
                    cy = interpolate(pyMinus, empty, p/min);
                }


                yImage.setRGB(i%width, i/width, addColors(cx, cy).getRGB());

            }
            xMomentumImage = xImage;
            yMomentumImage = yImage;
        }

        int comp(int a, int b, int bg){
            int i = bg - ( (bg - a) + (bg - b) );
            i = i<0 ? 0: i;
            i = i>255 ? 255 : i;
            return i;
        }
        Color addColors(Color c1, Color c2){

            int r = comp(c1.getRed(), c2.getRed(), empty.getRed());
            int g = comp(c1.getGreen(),c2.getGreen(), empty.getGreen());
            int b = comp(c1.getBlue(),c2.getBlue(), empty.getBlue());

            return new Color(r,g,b);

        }
    }




}

class MomentumTorus{
    double radius = 96;
    double thickness = 16;
    double m = 5;
    int width;
    int height;
    MomentumTorus(int width, int height){
        this.width = width;
        this.height = height;
    }
    public void momentumTorus(double cx, double cy, double[] momentum){

        for(int i = 0; i<width; i++){
            for(int j = 0; j<height; j++){
                double dx = i - cx;
                double dy = -j + cy;
                double r = Math.sqrt(dx*dx + dy*dy);
                if(r == 0) r = 1;

                double sin = dx/r;
                double cos = dy/r;

                double f = Math.exp( - Math.pow( r - radius, 2) / (2 * Math.pow(thickness, 2) )) ;

                momentum[2*(i + j*width)] += m*f*cos;
                momentum[2*(i + j*width) + 1] += m*f*sin;

            }
        }
    }

}
