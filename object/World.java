package object;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import sampler.PureRandom;
import sampler.Sampler;
import tracer.MultipleObjects;
import tracer.ShadeRec;
import tracer.Tracer;
import util.Normal;
import util.Point2D;
import util.Point3D;
import util.RGBColor;
import util.Ray;
import util.Vector3D;

/**
 *
 * @author michael
 */
public class World {
    public static void main(String[] args) throws IOException {
        World w = new World();
        
        BufferedImage bi = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        w.renderScene(bi);
        ImageIO.write(bi, "png", new File("outfile.png"));
    }
    
    private ViewPlane vp;
    private RGBColor backgroundColor;
    private Tracer tracer;
    private ArrayList<GeometricObject> objects;

    public World() {
        objects = new ArrayList<GeometricObject>();
        build();
    }

    public RGBColor getBackgroundColor() {
        return backgroundColor;
    }

    private void build() {
        vp = new ViewPlane(1000, 1000, 0.01f, 1, new PureRandom(100, 1));
        tracer = new MultipleObjects(this);
        backgroundColor = RGBColor.BLACK;
        
        Sphere sa = new Sphere(new RGBColor(1, 0, 0), new Point3D(0, 0, 0), 3);
        objects.add(sa);
        
        Sphere sb = new Sphere(new RGBColor(0, 1, 0.5f), new Point3D(0, 2, 0.5), 2);
        objects.add(sb);
        
        Plane p = new Plane(new RGBColor(0, 0.25f, 0.25f), new Point3D(0, 0, 0), new Normal(0, 1, 1));
        objects.add(p);
    }
    
    public ShadeRec hitBareBonesObjects(Ray r) {
        ShadeRec s = new ShadeRec(this);
        
        for (GeometricObject g : objects) {
            if (g.hit(r, s)) {
                s.color = g.getColor();
                s.hitAnObject = true;
            }
        }
        
        return s;
    }
    
    public void renderScene(BufferedImage img) {
        int n = (int) Math.sqrt(vp.getNumSamples());
        WritableRaster raster = img.getRaster();
        
        //TODO: In the future, introduce some type of multithreaded handling of rendering.
        for (int r = 0; r < vp.getVres(); r++) {
            for (int c = 0; c < vp.getHres(); c++) {
                Sampler.SamplerKey sk = new Sampler.SamplerKey();
                RGBColor pixelColor = new RGBColor(0); //BLACK
                Ray ray = new Ray();
                ray.d = new Vector3D(0, 0, -1);
                
                for (int p = 0; p < n; p++) {
                    for (int q = 0; q < n; q++) {
                        Point2D pp = new Point2D(0), sp = vp.getSampler().sampleUnitSquare(sk);
                        pp.x = vp.getS() * (c - 0.5 * (vp.getHres() - 1.0) + sp.x);
                        pp.y = vp.getS() * (r - 0.5 * (vp.getVres() - 1.0) + sp.y);
                        ray.o = new Point3D(pp.x, pp.y, 100.0); //z is hard-coded in.
                        pixelColor.addTo(tracer.traceRay(ray));
                    }
                }
                
                pixelColor.scaleTo(1.0f / vp.getNumSamples());
                int[] ints = {(int) (255 * pixelColor.r),
                              (int) (255 * pixelColor.g),
                              (int) (255 * pixelColor.b)};
                raster.setPixel(c, vp.getHres() - r - 1, ints);
            }
        }
    }
}