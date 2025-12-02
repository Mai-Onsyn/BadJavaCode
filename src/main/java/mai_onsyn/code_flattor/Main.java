package mai_onsyn.code_flattor;

import java.awt.*;
import java.io.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<String> tokens = FileRecoder.tokenizeJava(new File("src/main/resources/TestJavaFile.java"));

        AXImage image = new AXImage(new File("D:/Users/Desktop/image_52.png"));
        AXImage charlize = image.charlize(0.375, 200);

        List<String> recoded = FileRecoder.makePicture(tokens, charlize, 256);
        try (FileOutputStream fos = new FileOutputStream("src/test/java/TestJavaFile.java")) {
            for (String s : recoded) {
                System.out.print(s);
                fos.write((s).getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            String videoPath = "src/main/resources/bad apple!!!.mp4";
            String tempFramePath = "src/main/resources/temp_frame.png";
            double fps = VideoUtil.getVideoFPS(videoPath);

            int frameNum = 0;
            while (true) {
                if (VideoUtil.extractFrameByNumber(videoPath, tempFramePath, frameNum++, fps)) {
                    System.out.println(frameNum);
                    AXImage frame = new AXImage(new File(tempFramePath));
                    AXImage charFrame = frame.charlize(0.375, 100);
                    List<String> charFrameMap = FileRecoder.makePicture(tokens, charFrame, 512);
                    try (FileOutputStream fos = new FileOutputStream("src/test/java/TestJavaFile.java")) {
                        for (String s : charFrameMap) {
//                        System.out.print(s);
                            fos.write((s).getBytes());
                        }
                    }
                } else break;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}