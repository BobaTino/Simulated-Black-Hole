import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

public class RealisticBlackHole {

    private int width = 800, height = 800;
    private long window;
    private int shaderProgram;
    private int vaoId;
    private int vboId;
    private int eboId;

    // Uniform locations
    private int timeUniform;
    private int resolutionUniform;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE); 

        window = glfwCreateWindow(width, height, "Realistic Black Hole", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable V-sync
        glfwShowWindow(window);

        GL.createCapabilities();

        // Build and compile shader program with our improved fragment shader.
        shaderProgram = createShaderProgram(vertexShaderSource, fragmentShaderSource);
        timeUniform = glGetUniformLocation(shaderProgram, "u_time");
        resolutionUniform = glGetUniformLocation(shaderProgram, "u_resolution");

        // Setup a full-screen quad.
        float[] vertices = {
            -1.0f,  1.0f,  // top left
            -1.0f, -1.0f,  // bottom left
             1.0f, -1.0f,  // bottom right
             1.0f,  1.0f   // top right
        };
        int[] indices = {
            0, 1, 2,
            2, 3, 0
        };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    private void loop() {
        float startTime = (float) glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            float currentTime = (float) glfwGetTime() - startTime;

            glClear(GL_COLOR_BUFFER_BIT);
            glUseProgram(shaderProgram);

            // Update uniforms
            glUniform1f(timeUniform, currentTime);
            glUniform2f(resolutionUniform, width, height);

            glBindVertexArray(vaoId);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
            glUseProgram(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
        glDeleteProgram(shaderProgram);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private int createShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Vertex shader compilation failed:\n" + glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Fragment shader compilation failed:\n" + glGetShaderInfoLog(fragmentShader));
        }

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glBindAttribLocation(program, 0, "aPos");
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Shader program linking failed:\n" + glGetProgramInfoLog(program));
        }

        // Cleanup shaders as they are now linked
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    // A simple vertex shader: passes through the vertex positions.
    private String vertexShaderSource =
        "#version 330 core\n" +
        "layout (location = 0) in vec2 aPos;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
        "}\n";

    // Improved fragment shader with more realistic effects.
    private String fragmentShaderSource =
        "#version 330 core\n" +
        "out vec4 fragColor;\n" +
        "uniform float u_time;\n" +
        "uniform vec2 u_resolution;\n" +
        "\n" +
        "// Pseudo-random hash function based on UV coordinates\n" +
        "float hash(vec2 p) {\n" +
        "    p = fract(p * vec2(123.34, 456.21));\n" +
        "    p += dot(p, p + 45.32);\n" +
        "    return fract(p.x * p.y);\n" +
        "}\n" +
        "\n" +
        "// Generate a simple star field\n" +
        "vec3 starField(vec2 uv) {\n" +
        "    float n = hash(uv);\n" +
        "    float star = smoothstep(0.98, 1.0, n);\n" +
        "    return vec3(star);\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    // Normalize coordinates: center of the screen is (0,0)\n" +
        "    vec2 uv = (gl_FragCoord.xy - 0.5 * u_resolution.xy) / u_resolution.y;\n" +
        "    float r = length(uv);\n" +
        "    float theta = atan(uv.y, uv.x);\n" +
        "\n" +
        "    // --- Gravitational lensing effect ---\n" +
        "    // A simple approximation: distort the angle based on distance\n" +
        "    float lensingStrength = 0.3;\n" +
        "    float distortedTheta = theta + lensingStrength / (r + 0.001);\n" +
        "    vec2 distortedUV = r * vec2(cos(distortedTheta), sin(distortedTheta));\n" +
        "\n" +
        "    // --- Background Star Field ---\n" +
        "    vec3 background = starField(distortedUV * 5.0);\n" +
        "\n" +
        "    // --- Accretion Disk ---\n" +
        "    // Parameters for the black hole and disk\n" +
        "    float BH_RADIUS = 0.15;\n" +
        "    float DISK_INNER = 0.16;\n" +
        "    float DISK_OUTER = 0.55;\n" +
        "\n" +
        "    vec3 disk = vec3(0.0);\n" +
        "    if (r > DISK_INNER && r < DISK_OUTER) {\n" +
        "        // Create a smooth gradient for the disk\n" +
        "        float t = smoothstep(DISK_INNER, DISK_OUTER, r);\n" +
        "        // Add a swirling effect that evolves over time\n" +
        "        float swirl = sin(8.0 * theta + u_time * 2.0 + r * 20.0);\n" +
        "        vec3 innerColor = vec3(1.0, 0.9, 0.6);\n" +
        "        vec3 outerColor = vec3(0.8, 0.3, 0.0);\n" +
        "        disk = mix(innerColor, outerColor, swirl * 0.5 + 0.5) * (1.0 - t);\n" +
        "    }\n" +
        "\n" +
        "    // --- Black Hole Event Horizon ---\n" +
        "    // Inside the event horizon, all light is swallowed\n" +
        "    if (r < BH_RADIUS) {\n" +
        "        background = vec3(0.0);\n" +
        "        disk = vec3(0.0);\n" +
        "    }\n" +
        "\n" +
        "    // Combine the background and disk effects\n" +
        "    vec3 color = background + disk;\n" +
        "    // Apply gamma correction\n" +
        "    color = pow(color, vec3(0.4545));\n" +
        "    fragColor = vec4(color, 1.0);\n" +
        "}\n";

    public static void main(String[] args) {
        new RealisticBlackHole().run();
    }
}
