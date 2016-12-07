package li.tengfei.apng.opt.builder;

import li.tengfei.apng.base.ApngFCTLChunk;
import li.tengfei.apng.base.ApngIHDRChunk;
import li.tengfei.apng.base.FormatNotSupportException;
import li.tengfei.apng.ext.ByteArrayPngChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

import static li.tengfei.apng.base.ApngConst.CODE_IHDR;
import static li.tengfei.apng.base.ApngConst.CODE_fcTL;

/**
 * Apng Rebuilder
 *
 * @author ltf
 * @since 16/12/6, 下午4:33
 */
public class ApngRebuilder {

    private static final Logger log = LoggerFactory.getLogger(ApngRebuilder.class);

    private PngData mApngData;

    private ArrayList<PngData> mFrameDatas;

    public PngData getApngData() {
        return mApngData;
    }

    public ArrayList<PngData> getFrameData() {
        return mFrameDatas;
    }

    public boolean rebuild(String srcApngFile, String shrinkedImgsDir, String outFile) {
        try {
            mApngData = new PngReader(srcApngFile).getPngData();
            mFrameDatas = new PngsCollector().getPngs(shrinkedImgsDir);

            // prepare inputs
            if (!prepare()) return
                    false;

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatNotSupportException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean prepare() {
        // collect fctls
        ArrayList<ApngFCTLChunk> fctlChunks = new ArrayList<>();
        ArrayList<Integer> repeats = new ArrayList<>();
        int stepDelay = Integer.MAX_VALUE;
        int delayDen = -1;
        for (PngChunkData chunk : mApngData.chunks) {
            if (chunk.typeCode == CODE_fcTL) {
                ApngFCTLChunk fctlChunk = new ApngFCTLChunk();
                fctlChunk.parse(new ByteArrayPngChunk(chunk.data));
                fctlChunks.add(fctlChunk);
                if (delayDen < 0) {
                    delayDen = fctlChunk.getDelayDen();
                } else if (delayDen != fctlChunk.getDelayDen()) {
                    log.error("different delayDen in frames");
                    return false;
                }
                repeats.add(fctlChunk.getDelayNum());
                stepDelay = stepDelay <= fctlChunk.getDelayNum() ? stepDelay : fctlChunk.getDelayNum();
            }
        }

        // update frame repeats
        ArrayList<Boolean> isFrameRepeat = new ArrayList<>();
        for (int i = 0; i < repeats.size(); i++) {
            int stepLen = repeats.get(i) / stepDelay;
            isFrameRepeat.add(false);
            for (int j = 1; j < stepLen; j++) {
                isFrameRepeat.add(true);
            }
            repeats.set(i, repeats.get(i) / stepDelay);
        }

        // check all frames count and pngs count
        if (isFrameRepeat.size() != mFrameDatas.size()) {
            log.error("apng frames count not equals to png pictures count");
            return false;
        }

        // remove repeated frames' png
        for (int i = mFrameDatas.size() - 1; i >= 0; i--) {
            if (isFrameRepeat.get(i)) {
                mFrameDatas.remove(i);
            }
        }

        // collect ihdrs
        ArrayList<ApngIHDRChunk> ihdrChunks = new ArrayList<>();
        for (PngData frame : mFrameDatas) {
            for (PngChunkData chunk : frame.chunks) {
                if (chunk.typeCode == CODE_IHDR) {
                    ApngIHDRChunk ihdrChunk = new ApngIHDRChunk();
                    ihdrChunk.parse(new ByteArrayPngChunk(chunk.data));
                    ihdrChunks.add(ihdrChunk);
                    break;
                }
            }
        }

        // check fctls count and ihdrs count
        if (fctlChunks.size() != ihdrChunks.size()) {
            log.error("frames' fctl count not equals to pictures' ihdr count");
            return false;
        }

        // check each frame size and png size
        for (int i = 0; i < fctlChunks.size(); i++) {
            if (fctlChunks.get(i).getWidth() != ihdrChunks.get(i).getWidth() ||
                    fctlChunks.get(i).getHeight() != ihdrChunks.get(i).getHeight()) {

                log.error(String.format("frame and png have different size\n index: %d, fw: %d, fh: %d, pw: %d, ph: %d",
                        i,
                        fctlChunks.get(i).getWidth(),
                        fctlChunks.get(i).getHeight(),
                        ihdrChunks.get(i).getWidth(),
                        ihdrChunks.get(i).getHeight()
                ));
                return false;
            }
        }
        return true;
    }
}
