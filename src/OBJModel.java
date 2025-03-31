import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class OBJModel {

    public static Scene parse(String filePath) throws IOException {
        Scene scene = new Scene();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(" ");
                switch (tokens[0]) {
                    case "v":
                        scene.vertices.add(new Vector3(
                            Double.parseDouble(tokens[1]),
                            Double.parseDouble(tokens[2]),
                            Double.parseDouble(tokens[3])
                        ));
                        break;
                    case "f":
                        int[] faceVertices = new int[tokens.length - 1];
                        for (int i = 0; i < faceVertices.length; i++) {
                            faceVertices[i] = Integer.parseInt(tokens[i + 1].split("/")[0]) - 1;
                        }
                        scene.faces.add(new Face(faceVertices));
                        break;
                }
            }
        }
        return scene;
    }
}

class Scene {
    ArrayList<Vector3> vertices = new ArrayList<>();
    ArrayList<Face> faces = new ArrayList<>();
}

class Vector3 {
    double x, y, z;

    Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    Vector3 add(Vector3 v) {
        return new Vector3(x + v.x, y + v.y, z + v.z);
    }

    Vector3 subtract(Vector3 v) {
        return new Vector3(x - v.x, y - v.y, z - v.z);
    }

    Vector3 cross(Vector3 v) {
        return new Vector3(
            y * v.z - z * v.y,
            z * v.x - x * v.z,
            x * v.y - y * v.x
        );
    }

    Vector3 multiply(double scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }

    double dot(Vector3 v) {
        return x * v.x + y * v.y + z * v.z;
    }

    double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    Vector3 normalize() {
        double length = length();
        return new Vector3(x / length, y / length, z / length);
    }
}

class Face {
    int[] vertices;

    Face(int[] vertices) {
        this.vertices = vertices;
    }
}
