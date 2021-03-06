package li.tengfei.apng.opt.builder;

import li.tengfei.apng.base.ApngACTLChunk;
import li.tengfei.apng.base.ApngFrame;
import li.tengfei.apng.base.ApngReader;
import li.tengfei.apng.base.FormatNotSupportException;
import li.tengfei.apng.opt.shrinker.TinypngShrinker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static li.tengfei.apng.opt.utils.FileUtils.extractFileDir;
import static li.tengfei.apng.opt.utils.FileUtils.extractFilenameWithoutExt;

/**
 * Extract frame pngs from apng file
 *
 * @author ltf
 * @since 16/12/7, 下午5:03
 */
public class ApngExtractor {
    private static final Logger log = LoggerFactory.getLogger(ApngExtractor.class);

    public static String apngFramesDir(String srcApngFile) {
        return srcApngFile + ".frames";
    }

    public boolean extract(String srcApngFile, String tinyKey) {
        try {
            srcApngFile = (new File(srcApngFile)).getAbsolutePath();
            ApngReader apngReader = new ApngReader(srcApngFile);
            String frameFnPrefix = extractFilenameWithoutExt(srcApngFile);
            File outDir = new File(apngFramesDir(srcApngFile));
            if (outDir.exists()) outDir.delete();
            outDir.mkdirs();

            ApngACTLChunk actl = apngReader.getACTL();
            for (int i = 0; i < actl.getNumFrames(); i++) {
                ApngFrame frame = apngReader.nextFrame();
                File extractFile = new File(outDir,
                        String.format("%s_%04d.png",
                                frameFnPrefix,
                                i));
                saveToFile(frame.getImageStream(), extractFile);

                if (tinyKey != null && tinyKey.trim().length() > 0) {
                    TinypngShrinker.shrink(extractFile.getAbsolutePath(),
                            extractFile.getAbsolutePath(),
                            tinyKey);
                    log.info(String.format("frame[%d] compressed", i));
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatNotSupportException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void saveToFile(InputStream is, File f) {
        try {
            FileOutputStream fos = new FileOutputStream(f);
            byte[] buf = new byte[1024 * 200];
            int n = 0;
            while ((n = is.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
