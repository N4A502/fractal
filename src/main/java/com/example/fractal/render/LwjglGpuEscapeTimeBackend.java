package com.example.fractal.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;

public class LwjglGpuEscapeTimeBackend implements EscapeTimeBackend {

    private static final String VERTEX_SHADER =
            "#version 330 core\n" +
            "layout (location = 0) in vec2 aPos;\n" +
            "out vec2 vUv;\n" +
            "void main() {\n" +
            "  vUv = aPos * 0.5 + 0.5;\n" +
            "  gl_Position = vec4(aPos, 0.0, 1.0);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 330 core\n" +
            "in vec2 vUv;\n" +
            "out vec4 fragColor;\n" +
            "uniform int uWidth;\n" +
            "uniform int uHeight;\n" +
            "uniform int uMaxIterations;\n" +
            "uniform int uProfile;\n" +
            "uniform float uZoom;\n" +
            "uniform float uOffsetX;\n" +
            "uniform float uOffsetY;\n" +
            "uniform float uBaseScale;\n" +
            "uniform float uCenterX;\n" +
            "uniform float uCenterY;\n" +
            "uniform float uJuliaCx;\n" +
            "uniform float uJuliaCy;\n" +
            "uniform vec3 uInsideColor;\n" +
            "uniform float uHueStart;\n" +
            "uniform float uHueRange;\n" +
            "uniform float uSaturation;\n" +
            "uniform float uBrightnessFloor;\n" +
            "uniform float uBrightnessRange;\n" +
            "vec3 hsv2rgb(vec3 c) {\n" +
            "  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
            "  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
            "  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
            "}\n" +
            "vec3 computeColor(int iterations, int maxIterations) {\n" +
            "  if (iterations >= maxIterations) {\n" +
            "    return uInsideColor;\n" +
            "  }\n" +
            "  float progress = float(iterations) / float(maxIterations);\n" +
            "  float hue = fract((uHueStart - progress * uHueRange) / 360.0);\n" +
            "  float brightness = clamp(uBrightnessFloor + uBrightnessRange * progress, 0.0, 1.0);\n" +
            "  return hsv2rgb(vec3(hue, uSaturation, brightness));\n" +
            "}\n" +
            "void main() {\n" +
            "  float px = vUv.x * float(uWidth);\n" +
            "  float py = (1.0 - vUv.y) * float(uHeight);\n" +
            "  float scale = uBaseScale / uZoom;\n" +
            "  float x0 = (px - float(uWidth) * 0.5 - uOffsetX) * scale / float(uWidth) + uCenterX;\n" +
            "  float y0 = (py - float(uHeight) * 0.5 - uOffsetY) * scale / float(uWidth) + uCenterY;\n" +
            "  float x = 0.0;\n" +
            "  float y = 0.0;\n" +
            "  float cx = x0;\n" +
            "  float cy = y0;\n" +
            "  if (uProfile == 1) { x = x0; y = y0; cx = uJuliaCx; cy = uJuliaCy; }\n" +
            "  int iteration = 0;\n" +
            "  for (int i = 0; i < uMaxIterations; i++) {\n" +
            "    if (x * x + y * y > 4.0) { break; }\n" +
            "    float nextX;\n" +
            "    if (uProfile == 2) {\n" +
            "      float absX = abs(x);\n" +
            "      float absY = abs(y);\n" +
            "      nextX = absX * absX - absY * absY + x0;\n" +
            "      y = 2.0 * absX * absY + y0;\n" +
            "    } else {\n" +
            "      nextX = x * x - y * y + cx;\n" +
            "      y = 2.0 * x * y + cy;\n" +
            "    }\n" +
            "    x = nextX;\n" +
            "    iteration++;\n" +
            "  }\n" +
            "  fragColor = vec4(computeColor(iteration, uMaxIterations), 1.0);\n" +
            "}\n";

    private boolean initialized;
    private long window;
    private int programId;
    private int vaoId;
    private int vboId;
    private int framebufferId;
    private int textureId;
    private int framebufferWidth;
    private int framebufferHeight;
    private String rendererName;

    public synchronized String initializeAndDescribe() {
        ensureInitialized();
        return rendererName;
    }

    @Override
    public synchronized int[] renderPixels(AbstractEscapeTimeRenderer renderer, EscapeTimeRenderContext context) {
        if (RenderCancellation.isCancelled(context.requestSequence())) {
            throw new IllegalStateException("Render cancelled");
        }
        ensureInitialized();
        glfwMakeContextCurrent(window);
        GL.setCapabilities(GL.createCapabilities());
        ensureFramebuffer(context.width(), context.height());

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
        GL11.glViewport(0, 0, context.width(), context.height());
        GL11.glClearColor(0f, 0f, 0f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programId);
        setUniforms(renderer, context);
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        GL30.glBindVertexArray(0);
        if (RenderCancellation.isCancelled(context.requestSequence())) {
            throw new IllegalStateException("Render cancelled");
        }

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(context.width() * context.height() * 4);
        GL11.glReadPixels(0, 0, context.width(), context.height(), GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
        GL20.glUseProgram(0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        return convertPixels(byteBuffer, context.width(), context.height());
    }

    public synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        glfwMakeContextCurrent(window);
        GL.setCapabilities(GL.createCapabilities());
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
        }
        if (framebufferId != 0) {
            GL30.glDeleteFramebuffers(framebufferId);
            framebufferId = 0;
        }
        if (vboId != 0) {
            GL15.glDeleteBuffers(vboId);
            vboId = 0;
        }
        if (vaoId != 0) {
            GL30.glDeleteVertexArrays(vaoId);
            vaoId = 0;
        }
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
            programId = 0;
        }
        glfwDestroyWindow(window);
        glfwTerminate();
        initialized = false;
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        GLFWErrorCallback.createThrow().set();
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW initialization failed");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(1, 1, "fractal-gpu", 0L, 0L);
        if (window == 0L) {
            glfwTerminate();
            throw new IllegalStateException("OpenGL context creation failed");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        rendererName = GL11.glGetString(GL11.GL_RENDERER) + " / " + GL11.glGetString(GL11.GL_VERSION);
        programId = createProgram();
        createFullscreenTriangle();
        initialized = true;
    }

    private void createFullscreenTriangle() {
        float[] vertices = new float[]{
                -1.0f, -1.0f,
                3.0f, -1.0f,
                -1.0f, 3.0f
        };

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0L);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
    }

    private void ensureFramebuffer(int width, int height) {
        if (framebufferId != 0 && framebufferWidth == width && framebufferHeight == height) {
            return;
        }

        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
        }
        if (framebufferId == 0) {
            framebufferId = GL30.glGenFramebuffers();
        }

        framebufferWidth = width;
        framebufferHeight = height;

        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("OpenGL framebuffer is incomplete");
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private void setUniforms(AbstractEscapeTimeRenderer renderer, EscapeTimeRenderContext context) {
        EscapeTimeColorSettings settings = EscapeTimeColorManager.getSettings();
        setInt("uWidth", context.width());
        setInt("uHeight", context.height());
        setInt("uMaxIterations", context.maxIterations());
        setInt("uProfile", shaderProfileIndex(renderer.getShaderProfile()));
        setFloat("uZoom", context.zoom());
        setFloat("uOffsetX", context.offsetX());
        setFloat("uOffsetY", context.offsetY());
        setFloat("uBaseScale", renderer.getBaseScale());
        setFloat("uCenterX", renderer.getCenterX());
        setFloat("uCenterY", renderer.getCenterY());
        setFloat("uJuliaCx", renderer.getJuliaCx());
        setFloat("uJuliaCy", renderer.getJuliaCy());
        setVec3("uInsideColor", settings.insideColorRgb());
        setFloat("uHueStart", settings.hueStartDegrees());
        setFloat("uHueRange", settings.hueRangeDegrees());
        setFloat("uSaturation", settings.saturation());
        setFloat("uBrightnessFloor", settings.brightnessFloor());
        setFloat("uBrightnessRange", settings.brightnessRange());
    }

    private int shaderProfileIndex(EscapeTimeShaderProfile profile) {
        if (profile == EscapeTimeShaderProfile.JULIA) {
            return 1;
        }
        if (profile == EscapeTimeShaderProfile.BURNING_SHIP) {
            return 2;
        }
        return 0;
    }

    private void setInt(String name, int value) {
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, name), value);
    }

    private void setFloat(String name, double value) {
        GL20.glUniform1f(GL20.glGetUniformLocation(programId, name), (float) value);
    }

    private void setVec3(String name, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        GL20.glUniform3f(GL20.glGetUniformLocation(programId, name), r, g, b);
    }

    private int[] convertPixels(ByteBuffer buffer, int width, int height) {
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int sourceY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int index = (sourceY * width + x) * 4;
                int r = buffer.get(index) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                int a = buffer.get(index + 3) & 0xFF;
                pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return pixels;
    }

    private int createProgram() {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String info = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            throw new IllegalStateException("OpenGL shader link failed: " + info);
        }
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        return program;
    }

    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException("OpenGL shader compile failed: " + GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }
}