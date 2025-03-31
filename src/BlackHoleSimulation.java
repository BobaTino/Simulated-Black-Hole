import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Random;
import org.lwjgl.BufferUtils;

public class BlackHoleSimulation {
    
    private long window;
    private int width = 1200;
    private int height = 900;

    private float panX = 0.0f;
    private float panY = 0.0f;
    private float rotationX = 20.0f;  // Tilt view
    private float rotationY = 0.0f;   // Around black hole
    private final float panSpeed = 0.05f;
    private final float rotationSpeed = 2.5f;

    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    private double lastMouseX, lastMouseY;


    
    // Adjusted black hole properties
    private final float blackHoleRadius = 0.4f;
    private final float eventHorizonRadius = 0.6f;
    
    // Particle system
    private static final int NUM_PARTICLES = 20000;  // More particles
    private Particle[] particles = new Particle[NUM_PARTICLES];
    
    // Camera settings
    private float cameraDistance = 2.0f;
    private float cameraAngle = 0.0f;
    private float cameraHeight = 0.4f;
    private float cameraSpeed = 0.03f;
    
    private Random random = new Random();
    private boolean paused = false;
    
    class Particle {
        float distance;
        float angle;
        float height;
        float speed;
        float size;
        float[] color = new float[3];
        float life;

        private static final int TRAIL_LENGTH = 8;
        LinkedList<float[]> trail = new LinkedList<>();

        public Particle() {
            reset();
            life = 1.0f;
        }

        public void reset() {
            distance = eventHorizonRadius + random.nextFloat() * 1.5f;
            angle = random.nextFloat() * (float)Math.PI * 2;
            height = (random.nextFloat() - 0.5f) * 0.1f;
            speed = (0.3f + random.nextFloat() * 0.7f) / (distance * distance);
            size = 0.015f + random.nextFloat() * 0.03f;

            float tempFactor = 1.0f - (distance - eventHorizonRadius) / 1.5f;
            color[0] = 0.9f + tempFactor * 0.1f;
            color[1] = 0.3f + tempFactor * 0.5f;
            color[2] = 0.1f * tempFactor;

            trail.clear();
            addCurrentPositionToTrail();
            life = 1.0f;
        }

        public void update(float deltaTime) {
            if (paused) return;

            angle += speed * deltaTime;
            distance -= 0.00005f * deltaTime;
            height *= 0.998f;
            life -= 0.0001f * deltaTime;

            addCurrentPositionToTrail();

            if (distance < eventHorizonRadius || life <= 0) {
                reset();
            }
        }

        private void addCurrentPositionToTrail() {
            float x = (float) (distance * Math.cos(angle));
            float z = (float) (distance * Math.sin(angle));
            float r = (float) Math.sqrt(x * x + z * z);
            float angleFromView = (float) Math.atan2(z, x);
            float warpBend = (float) Math.sin(angleFromView);
            float maxWarp = 0.45f;
            float verticalWarp = warpBend * maxWarp * (float) Math.exp(-Math.pow((r - blackHoleRadius) * 1.6f, 2));
            float y = verticalWarp;

            trail.addFirst(new float[]{x, y, z});
            if (trail.size() > TRAIL_LENGTH) {
                trail.removeLast();
            }
        }
    }
    
    public void run() {
        init();
        loop();
        GLFW.glfwTerminate();
    }
    
    private void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        
        window = GLFW.glfwCreateWindow(width, height, "Black Hole Simulation", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Center window
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        GLFW.glfwSetWindowPos(
            window,
            (vidmode.width() - width) / 2,
            (vidmode.height() - height) / 2
        );
        
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                switch (key) {
                    case GLFW.GLFW_KEY_ESCAPE -> GLFW.glfwSetWindowShouldClose(window, true);
                    case GLFW.GLFW_KEY_SPACE -> paused = !paused;
                    case GLFW.GLFW_KEY_W -> panY += panSpeed;
                    case GLFW.GLFW_KEY_S -> panY -= panSpeed;
                    case GLFW.GLFW_KEY_A -> panX -= panSpeed;
                    case GLFW.GLFW_KEY_D -> panX += panSpeed;
                    case GLFW.GLFW_KEY_LEFT -> rotationY -= rotationSpeed;
                    case GLFW.GLFW_KEY_RIGHT -> rotationY += rotationSpeed;
                    case GLFW.GLFW_KEY_UP -> rotationX -= rotationSpeed;
                    case GLFW.GLFW_KEY_DOWN -> rotationX += rotationSpeed;
                }
            }
        });

                // Mouse button callback
        GLFW.glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                leftMousePressed = (action == GLFW.GLFW_PRESS);
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                rightMousePressed = (action == GLFW.GLFW_PRESS);
            }
        });

                // Scroll wheel callback for zoom
        GLFW.glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            cameraDistance -= (float) yoffset * 0.1f;
            cameraDistance = Math.max(0.5f, Math.min(cameraDistance, 10.0f));  // Clamp zoom range
        });


        // Cursor position callback
        GLFW.glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            double dx = xpos - lastMouseX;
            double dy = ypos - lastMouseY;
            lastMouseX = xpos;
            lastMouseY = ypos;

            if (leftMousePressed) {
                panX += (float) dx * 0.002f;
                panY -= (float) dy * 0.002f;
            }

            if (rightMousePressed) {
                rotationY += dx * 0.5f;
                rotationX += dy * 0.5f;
            }
        });

        
        
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        
        GL.createCapabilities();
        
        glViewport(0, 0, width, height);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_POINT_SMOOTH);
        glHint(GL_POINT_SMOOTH_HINT, GL_NICEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Initialize particles
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles[i] = new Particle();
        }
    }
    
    private void loop() {
        float lastTime = (float)GLFW.glfwGetTime();
        
        while (!GLFW.glfwWindowShouldClose(window)) {
            float currentTime = (float)GLFW.glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;
            
            // if (!paused) {
            //     cameraAngle += cameraSpeed * deltaTime;
            // }
            
            for (Particle p : particles) {
                p.update(deltaTime);
            }
            
            render();
            
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }
    
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
        // Set up projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float aspect = (float) width / (float) height;
        gluPerspective(65.0f, aspect, 0.1f, 100.0f);
    
        // Set up view
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    
        // Orbiting camera around Y-axis based on rotationY
        float camX = (float) (cameraDistance * Math.sin(Math.toRadians(rotationY)));
        float camZ = (float) (cameraDistance * Math.cos(Math.toRadians(rotationY)));
        float camY = cameraHeight;
    
        gluLookAt(camX, camY, camZ,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f);
    
        // Apply panning and scene rotation
        glTranslatef(panX, panY, 0.0f);  // Pan in X/Y
        glRotatef(rotationX, 1.0f, 0.0f, 0.0f);  // Rotate scene up/down
    
        // Draw event horizon glow
        for (int i = 0; i < 3; i++) {
            float scale = 1.0f + i * 0.05f;
            float alpha = 0.2f - i * 0.05f;
            glColor4f(0.8f, 0.4f, 0.1f, alpha);
            glPushMatrix();
            glScalef(scale, scale, scale);
            glutWireSphere(eventHorizonRadius, 32, 32);
            glPopMatrix();
        }
    
        // Draw particles and their trails
        for (Particle p : particles) {
            if (p.distance < blackHoleRadius * 1.1f) continue;
    
            // Draw trail
            glBegin(GL_LINE_STRIP);
            int i = 0;
            for (float[] pos : p.trail) {
                float alpha = (1.0f - i / (float) p.trail.size()) * p.life;
                glColor4f(p.color[0], p.color[1], p.color[2], alpha);
                glVertex3f(pos[0], pos[1], pos[2]);
                i++;
            }
            glEnd();
    
            // Draw main particle
            float[] pos = p.trail.getFirst();
            float lensFactor = 1.0f + 0.7f * blackHoleRadius / p.distance;
            float distortion = 1.0f + 0.3f * (float) Math.sin(p.angle * 5);
            float pointSize = p.size * 120 * lensFactor * distortion;
    
            glColor3f(p.color[0] * p.life, p.color[1] * p.life, p.color[2] * p.life);
            glPointSize(pointSize);
            glBegin(GL_POINTS);
            glVertex3f(pos[0], pos[1], pos[2]);
            glEnd();
        }
    
        // Gravitational lensing rings
        drawGravitationalLensing();
    
        // Photon ring
        drawPhotonRing();
    
        // Black hole core
        glColor3f(0.0f, 0.0f, 0.0f);
        glPushMatrix();
        glutSolidSphere(blackHoleRadius, 64, 64);
        glPopMatrix();
    }
    
    
    private void drawPhotonRing() {
    float baseRadius = blackHoleRadius * 1.45f; // very close to horizon
    int rings = 10;                             // number of concentric rings
    int segments = 256;
    
    for (int i = 0; i < rings; i++) {
        float radius = baseRadius + i * 0.01f;
        float alpha = 0.08f - i * 0.02f; // fade with distance
        if (alpha <= 0) continue;

        glColor4f(1.0f, 0.6f, 0.2f, alpha); // warm glow
        glBegin(GL_LINE_LOOP);
        for (int j = 0; j < segments; j++) {
            float angle = (float) (2 * Math.PI * j / segments);
            float x = (float) (radius * Math.cos(angle));
            float z = (float) (radius * Math.sin(angle));
            glVertex3f(x, 0.0f, z);
        }
        glEnd();
        }
    }

    private void drawGravitationalLensing() {
        int rings = 5;
        glColor4f(0.9f, 0.6f, 0.1f, 0.1f);
        
        for (int i = 1; i <= rings; i++) {
            float radius = eventHorizonRadius * 1.5f + i * 0.3f;
            glBegin(GL_LINE_LOOP);
            for (int j = 0; j < 360; j += 10) {
                float angle = (float)Math.toRadians(j);
                float x = (float)(radius * Math.cos(angle));
                float z = (float)(radius * Math.sin(angle));
                glVertex3f(x, 0, z);
            }
            glEnd();
        }
    }
    
    // Utility methods
    private void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        float f = 1.0f / (float)Math.tan(Math.toRadians(fovy) / 2.0f);
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        matrix.put(new float[] {
            f/aspect, 0, 0, 0,
            0, f, 0, 0,
            0, 0, (zFar+zNear)/(zNear-zFar), -1,
            0, 0, (2*zFar*zNear)/(zNear-zFar), 0
        });
        matrix.flip();
        glMultMatrixf(matrix);
    }
    
    private void gluLookAt(float eyeX, float eyeY, float eyeZ,
                         float centerX, float centerY, float centerZ,
                         float upX, float upY, float upZ) {
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        
        float[] forward = {centerX - eyeX, centerY - eyeY, centerZ - eyeZ};
        float len = (float)Math.sqrt(forward[0]*forward[0] + forward[1]*forward[1] + forward[2]*forward[2]);
        forward[0] /= len; forward[1] /= len; forward[2] /= len;
        
        float[] up = {upX, upY, upZ};
        float[] side = {
            forward[1]*up[2] - forward[2]*up[1],
            forward[2]*up[0] - forward[0]*up[2],
            forward[0]*up[1] - forward[1]*up[0]
        };
        
        len = (float)Math.sqrt(side[0]*side[0] + side[1]*side[1] + side[2]*side[2]);
        side[0] /= len; side[1] /= len; side[2] /= len;
        
        up[0] = side[1]*forward[2] - side[2]*forward[1];
        up[1] = side[2]*forward[0] - side[0]*forward[2];
        up[2] = side[0]*forward[1] - side[1]*forward[0];
        
        matrix.put(new float[] {
            side[0], up[0], -forward[0], 0,
            side[1], up[1], -forward[1], 0,
            side[2], up[2], -forward[2], 0,
            0, 0, 0, 1
        });
        matrix.flip();
        glMultMatrixf(matrix);
        glTranslatef(-eyeX, -eyeY, -eyeZ);
    }
    
    private void glutSolidSphere(float radius, int slices, int stacks) {
        FloatBuffer quadric = BufferUtils.createFloatBuffer(1);
        glNewList(1, GL_COMPILE);
        
        glBegin(GL_QUAD_STRIP);
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * ((double)i / stacks - 0.5);
            for (int j = 0; j <= slices; j++) {
                double theta = 2.0 * Math.PI * (double)j / slices;
                
                float x = (float)(Math.cos(theta) * Math.cos(phi));
                float y = (float)Math.sin(phi);
                float z = (float)(Math.sin(theta) * Math.cos(phi));
                
                glNormal3f(x, y, z);
                glVertex3f(x * radius, y * radius, z * radius);
            }
        }
        glEnd();
        
        glEndList();
        glCallList(1);
    }
    
    private void glutWireSphere(float radius, int slices, int stacks) {
        // Longitude lines
        for (int j = 0; j < slices; j++) {
            glBegin(GL_LINE_LOOP);
            for (int i = 0; i <= stacks; i++) {
                double theta = Math.PI * ((double)i / stacks - 0.5);
                double phi = 2.0 * Math.PI * (double)j / slices;
                
                float x = (float)(Math.cos(phi) * Math.cos(theta));
                float y = (float)Math.sin(theta);
                float z = (float)(Math.sin(phi) * Math.cos(theta));
                
                glVertex3f(x * radius, y * radius, z * radius);
            }
            glEnd();
        }
        
        // Latitude lines
        for (int i = 0; i < stacks; i++) {
            glBegin(GL_LINE_LOOP);
            for (int j = 0; j <= slices; j++) {
                double theta = Math.PI * ((double)i / stacks - 0.5);
                double phi = 2.0 * Math.PI * (double)j / slices;
                
                float x = (float)(Math.cos(phi) * Math.cos(theta));
                float y = (float)Math.sin(theta);
                float z = (float)(Math.sin(phi) * Math.cos(theta));
                
                glVertex3f(x * radius, y * radius, z * radius);
            }
            glEnd();
        }
    }
    
    public static void main(String[] args) {
        new BlackHoleSimulation().run();
    }
}