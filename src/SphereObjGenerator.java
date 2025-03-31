import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SphereObjGenerator {

    public static void main(String[] args) {
        /*

         Decrease the stacks & slices if rendering is taking too long
         
         ****I suggest 16 for both****

         */
        int stacks = 32; 
        int slices = 32; 
        float radius = 2.0f;

        StringBuilder obj = new StringBuilder();

        
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                double theta = 2 * Math.PI * j / slices;

                float x = (float)(radius * Math.sin(phi) * Math.cos(theta));
                float y = (float)(radius * Math.cos(phi));
                float z = (float)(radius * Math.sin(phi) * Math.sin(theta));

                obj.append(String.format("v %.6f %.6f %.6f\n", x, y, z));
            }
        }


        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int first = (i * (slices + 1)) + j + 1;
                int second = first + slices + 1;

                obj.append(String.format("f %d %d %d\n", first, second, first + 1));
                obj.append(String.format("f %d %d %d\n", second, second + 1, first + 1));
            }
        }

        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Textures\\sphere.obj"))) {
            writer.write(obj.toString());
            System.out.println("sphere.obj generated successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
