package mai_onsyn.code_flattor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class VideoUtil {

    /**
     * 使用FFmpeg提取指定时间点的帧
     * @param videoPath 视频文件路径
     * @param outputPath 输出图片路径
     * @param timeInSeconds 时间点（秒）
     * @return 是否成功
     */
    public static boolean extractFrameAtTime(String videoPath, String outputPath, double timeInSeconds) {
        try {
            // 构建FFmpeg命令
            String[] cmd = {
                    "ffmpeg",
                    "-ss", String.valueOf(timeInSeconds), // 跳转到指定时间
                    "-i", videoPath,
                    "-vframes", "1", // 只提取1帧
                    "-q:v", "2", // 输出质量（2-31，2为最高质量）
                    "-y", // 覆盖输出文件
                    outputPath
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();

            return exitCode == 0 && new File(outputPath).exists();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 使用FFmpeg提取指定帧号的帧
     * @param videoPath 视频文件路径
     * @param outputPath 输出图片路径
     * @param frameNumber 帧号
     * @return 是否成功
     */
    public static boolean extractFrameByNumber(String videoPath, String outputPath, int frameNumber, double fps) {
        try {
            // 先获取视频帧率
//            double fps = getVideoFPS(videoPath);
            if (fps <= 0) return false;

            // 根据帧号计算时间
            double time = frameNumber / fps;

            return extractFrameAtTime(videoPath, outputPath, time);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取视频帧率
     */
    public static double getVideoFPS(String videoPath) {
        try {
            String[] cmd = {
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=r_frame_rate",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath
            };

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String fpsStr = reader.readLine();
            process.waitFor();

            if (fpsStr != null && fpsStr.contains("/")) {
                String[] parts = fpsStr.split("/");
                double num = Double.parseDouble(parts[0]);
                double den = Double.parseDouble(parts[1]);
                return num / den;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

}
