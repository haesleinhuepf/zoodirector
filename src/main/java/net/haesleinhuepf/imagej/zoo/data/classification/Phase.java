package net.haesleinhuepf.imagej.zoo.data.classification;

public enum Phase {
    unefined(0),
    phase_1st_wave(1),
    phase_2nd_wave(2),
    phase_3rd_wave(3),
    phase_4th_wave(4),
    phase_5th_wave(5),
    phase_6th_wave(6),
    phase_post_6th_wave(7),
    phase_7th_wave(8),
    phase_post_7th_wave(9),
    phase_8th_wave(10),
    phase_post_8th_wave(11),
    phase_9th_wave(12),
    phase_post_9th_wave(13),
    phase_10th_wave(14),
    phase_post_10th_wave(15),
    phase_11th_wave(16),
    phase_post_11th_wave(17),
    phase_12th_wave(18),
    phase_post_12th_wave(19),
    phase_blastopore(20),
    phase_13th_wave_differentiation(21),
    phase_gastrulation(22),
    phase_closing_serosa_winndow(23),
    phase_detachment(24);

    public final int MAX_PHASES = 25;

    public static Phase[] all = {
            unefined,
            phase_1st_wave,
            phase_2nd_wave,
            phase_3rd_wave,
            phase_4th_wave,
            phase_5th_wave,
            phase_6th_wave,
            phase_post_6th_wave,
            phase_7th_wave,
            phase_post_7th_wave,
            phase_8th_wave,
            phase_post_8th_wave,
            phase_9th_wave,
            phase_post_9th_wave,
            phase_10th_wave,
            phase_post_10th_wave,
            phase_11th_wave,
            phase_post_11th_wave,
            phase_12th_wave,
            phase_post_12th_wave,
            phase_blastopore,
            phase_13th_wave_differentiation,
            phase_gastrulation,
            phase_closing_serosa_winndow,
            phase_detachment
};

    public int value = 0;

    Phase(int value) {
        this.value = value;
    }
}
