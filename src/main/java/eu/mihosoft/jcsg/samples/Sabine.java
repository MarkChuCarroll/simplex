/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.jcsg.samples;

import eu.mihosoft.vvecmath.Transform;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Average Chicken Egg.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Sabine {

    public CSG toCSG() {

        double w = 16;
        double h = 9;
        double offset = 1.5;

        double fractW = 2;
        double fractL = 10;
        double fractD = 0.25;

        CSG cube = new Cube(w, h, 1).toCSG();

        CSG beam = new Cylinder(1, 10, 32).toCSG();

        CSG beam1 = beam.weighted(new ZModifier()).transformed(Transform.unity().scale(0.5, 0.5, 1)).weighted(new UnityModifier()).transformed(Transform.unity().translate(w / 2.0 - offset, h / 2.0 - offset, 0));
        CSG beam2 = beam.transformed(Transform.unity().translate(-(w / 2.0 - offset), h / 2.0 - offset, 0));
        CSG beam3 = beam.transformed(Transform.unity().translate(-(w / 2.0 - offset), -(h / 2.0 - offset), 0));
        CSG beam4 = beam.transformed(Transform.unity().translate(w / 2.0 - offset, -(h / 2.0 - offset), 0));

        CSG fractures = null;

        for(int i = 0; i < 50;i++) {

            double angleX = 45 + Math.random()*90;
            double angleZ = 45 + Math.random()*90;

            CSG fracture1 = new Cube(0.1+fractW*Math.random(), fractL*Math.random(),fractD).noCenter().toCSG().transformed(Transform.unity().rotZ(-angleZ).rotX(-angleX));

            double x = -w/2.0+Math.random()*(w-2);
            double y = -h/2.0+Math.random()*(h-2);


            fracture1 = fracture1.transformed(Transform.unity().translate(x, y, 0));

            if (fractures==null) {
                fractures = fracture1;
            } else {
                fractures = fractures.union(fracture1);
            }

        }


        CSG diffCube = new Cube(w, h, 11).noCenter().toCSG().transformed(Transform.unity().translate(-w/2.0, -h/2.0, 0));

        fractures = fractures.intersect(diffCube);


        return cube.union(beam1,beam2,beam3,beam4,fractures);

    }

    public static void main(String[] args) throws IOException {
        FileUtil.write(Paths.get("sabine.stl"), new Sabine().toCSG().toStlString());
    }
}
