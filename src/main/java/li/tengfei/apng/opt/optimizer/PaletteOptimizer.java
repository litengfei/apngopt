package li.tengfei.apng.opt.optimizer;

import li.tengfei.apng.opt.builder.AngChunkData;
import li.tengfei.apng.opt.builder.AngData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

import static li.tengfei.apng.base.ApngConst.*;

/**
 * Optimizer for the ANG frames palette
 *
 * @author ltf
 * @since 16/12/12, 下午2:05
 */
public class PaletteOptimizer implements AngOptimizer {
    private static final int COLOR_B = 0x1;
    private static final int COLOR_G = 0x100;
    private static final int COLOR_R = 0x10000;
    private static final Logger log = LoggerFactory.getLogger(PaletteOptimizer.class);

    @Override
    public AngData optimize(AngData ang) {
        ArrayList<Palette> palettes = genPalettes(ang);
        palettes = optmizePalette(palettes);
        return null;
    }


    /**
     * @param palettes
     * @return
     */
    private ArrayList<Palette> optmizePalette(ArrayList<Palette> palettes) {


        return null;

    }

    /**
     * generate palettes with Merged Color to the most previous one
     */
    private ArrayList<Palette> genPalettes(AngData ang) {
        Palette pre = null;
        ArrayList<Palette> palettes = new ArrayList<>();
        ArrayList<Color> newColors = new ArrayList<>();
        int chunkIndex = -1;
        int plteIndex = -1;
        int trnsIndex = -1;
        for (AngChunkData chunk : ang.getChunks()) {
            chunkIndex++;
            switch (chunk.getTypeCode()) {
                case CODE_PLTE:
                    plteIndex = chunkIndex;
                    continue;
                case CODE_tRNS:
                    trnsIndex = chunkIndex;
                    continue;
                case CODE_IDAT:
                case CODE_fdAT:
                    if (plteIndex >= 0) break;
                default:
                    continue;
            }

            if (pre == null) {
                pre = new Palette(plteIndex, trnsIndex);
            }

            // get new appeared colors in this frame
            byte[] data = ang.getChunks().get(plteIndex).getData();
            byte[] alpha = ang.getChunks().get(trnsIndex).getData();
            int count = colorsCount(data);
            newColors.clear();
            HashSet<Color> preColors = new HashSet<>();
            preColors.addAll(pre.colors);
            for (int i = 0; i < count; i++) {
                Color newColor = readColor(data, alpha, i);
                if (!preColors.contains(newColor)) {
                    newColors.add(newColor);
                }
            }

            if (pre.colors.size() + newColors.size() > 256) {
                palettes.add(pre);
                pre = new Palette(plteIndex, trnsIndex);
                pre.colors.addAll(newColors);
            } else {
                pre.colors.addAll(newColors);
            }

            plteIndex = -1;
            trnsIndex = -1;
        }
        if (pre != null) palettes.add(pre);

        for (Palette p : palettes) {
            log.debug(String.format("chunk: %d, frame: %d, colors: %d",
                    p.plteIndex,
                    ang.getChunks().get(p.plteIndex).getFrameIndex(),
                    p.colors.size()));
        }

        return palettes;
    }

    private int colorsCount(byte[] plteChunkData) {
        return (plteChunkData.length - 12) / 3;
    }

    /**
     * read color from PLTE & tRNS chunks data
     *
     * @param plteChunkData not null
     * @param trnsChunkData can be null, then alpha is 255
     * @param index         color index
     * @return color
     */
    private Color readColor(byte[] plteChunkData, byte[] trnsChunkData, int index) {
        int alpha = 255;
        if (trnsChunkData != null && index < trnsChunkData.length)
            alpha = trnsChunkData[index];
        int off = 8 + index * 3;
        return new Color(plteChunkData[off]& 0xFF,
                plteChunkData[off + 1]& 0xFF,
                plteChunkData[off + 2]& 0xFF);
    }

    /**
     * write color to PLTE & rRNS chunk data
     *
     * @param chunkData     not null
     * @param trnsChunkData can be null
     * @param index         color index
     * @param color         color
     */
    private void writeColor(byte[] chunkData, byte[] trnsChunkData, int index, Color color) {
        int off = 8 + index * 3;
        chunkData[off++] = (byte) (color.getRed() & 0xFF);
        chunkData[off++] = (byte) (color.getGreen() & 0xFF);
        chunkData[off] = (byte) (color.getBlue() & 0xFF);
        if (trnsChunkData != null && index < trnsChunkData.length) {
            trnsChunkData[index] = (byte) (color.getAlpha() & 0xFF);
        }
    }

    private static class Palette {
        ArrayList<Color> colors = new ArrayList<>();
        int plteIndex;
        int trnsIndex;

        public Palette(int plteIndex, int trnsIndex) {
            this.plteIndex = plteIndex;
            this.trnsIndex = trnsIndex;
        }
    }
}
