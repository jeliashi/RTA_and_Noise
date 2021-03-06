package com.jeliav.android.rtaandnoise.view;

/**
 the array of colors for the spectogram and how they would be shown
 */

class SpectrogramColors {
//    public static final String LOG_TAG = SpectrogramColors.class.getSimpleName();
    private int[] empty = {20,20,25};
    private int[] cold = {28,135,255};
    private int[] warm = {255, 60, 60};
    private int[] hot = {249, 255, 25};
    private int[] clip = {255, 255, 255};

    int range = 256;
    private int EMPTY_LIMIT = range / 10;
    private int COLD_LIMIT = range/5;
    private int WARM_LIMIT = EMPTY_LIMIT*3;

    int[][] color_map = new int[range][3];


    SpectrogramColors(){
        generate();
    }

    private void generate(){
        for (int i = 0; i < EMPTY_LIMIT; i++){
            float fraction = ((float) i) /((float) EMPTY_LIMIT);
            color_map[i] = color_blend(empty, cold, fraction);
        }
        for (int i = EMPTY_LIMIT;i < COLD_LIMIT; i++){
            float fraction = ((float) i) /((float) (COLD_LIMIT - EMPTY_LIMIT));
            color_map[i] = color_blend(cold, warm, fraction);

        }
        for (int i = COLD_LIMIT; i< WARM_LIMIT; i++){
            float fraction = ((float) i) /((float) (WARM_LIMIT - COLD_LIMIT));
            color_map[i] = color_blend(warm, hot, fraction);
        }
        for (int i = WARM_LIMIT; i< range; i++){
            float fraction = ((float) i) /((float) (range - WARM_LIMIT));
            color_map[i] = color_blend(hot, clip, fraction);
        }
    }

    private int[] color_blend(int[] lower, int[] upper, float fraction){
        int r = int_blend(lower[0], upper[0], fraction);
        int g = int_blend(lower[1], upper[1], fraction);
        int b = int_blend(lower[2], upper[2], fraction);
        return new int[]{r,g,b};
    }

    private int int_blend(int lower, int upper, float fraction){
        return (lower + (int) ((upper - lower)*fraction));
    }


}
