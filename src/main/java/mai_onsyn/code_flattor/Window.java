package mai_onsyn.code_flattor;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;

public class Window {

    private final Robot robot;
    private WinDef.HWND window = null;

    public Window() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public void selectWindow(String title) {
        window = User32.INSTANCE.FindWindow(null, title);

//        if (window != null) System.out.println("Found window: {}", title);
        if (window == null) throw new RuntimeException("No window found: " + title);
    }

    public AXImage captureWindow() {
        WinDef.RECT rect = getClientRect();

        double scaleFactor = getScaleFactor();
        Rectangle rbtRect = new Rectangle(
                (int) Math.round(rect.left / scaleFactor),
                (int) Math.round(rect.top / scaleFactor),
                (int) Math.round((rect.right - rect.left) / scaleFactor),
                (int) Math.round((rect.bottom - rect.top) / scaleFactor)
        );


        MultiResolutionImage mrImage = robot.createMultiResolutionScreenCapture(rbtRect);
        BufferedImage highRes = (BufferedImage) mrImage.getResolutionVariants().getLast();
        return new AXImage(highRes);
    }

    public AXImage captureWindowWithResize(int width, int height) {
        AXImage sourceImage = captureWindow();
        AXImage resizedImage = new AXImage(width, height);

        double scaleX = width / (double) sourceImage.getWidth();
        double scaleY = height / (double) sourceImage.getHeight();

        int[] sourceImageData = sourceImage.getPixels();
        int[] resizedImageData = resizedImage.getPixels();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceX = (int) (x / scaleX);
                int sourceY = (int) (y / scaleY);

                int sourceIndex = (sourceY * sourceImage.getWidth() + sourceX);
                int resizedIndex = (y * width + x);
                resizedImageData[resizedIndex] = sourceImageData[sourceIndex];
            }
        }

//        try {
//            resizedImage.save(new File("D:/Users/Desktop/temp/" + System.currentTimeMillis() + ".png"));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return resizedImage;
    }

    public static double getScaleFactor() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getDefaultTransform()
                .getScaleX();
    }

    public WinDef.RECT getClientRect() {
        WinDef.RECT clientRect = new WinDef.RECT();
        if (!User32Extension.INSTANCE.GetClientRect(window, clientRect)) {
            throw new RuntimeException("GetClientRect failed");
        }

        WinDef.POINT topLeft = new WinDef.POINT(clientRect.left, clientRect.top);
        WinDef.POINT bottomRight = new WinDef.POINT(clientRect.right, clientRect.bottom);

        User32Extension.INSTANCE.ClientToScreen(window, topLeft);
        User32Extension.INSTANCE.ClientToScreen(window, bottomRight);

        WinDef.RECT screenRect = new WinDef.RECT();
        screenRect.left   = topLeft.x;
        screenRect.top    = topLeft.y;
        screenRect.right  = bottomRight.x;
        screenRect.bottom = bottomRight.y;

        return screenRect;
    }

    private interface User32Extension extends StdCallLibrary {
        User32Extension INSTANCE = Native.load("user32", User32Extension.class, com.sun.jna.win32.W32APIOptions.DEFAULT_OPTIONS);

        boolean GetClientRect(WinDef.HWND hWnd, WinDef.RECT rect);
        boolean ClientToScreen(WinDef.HWND hWnd, WinDef.POINT point);
    }
}