package net.haesleinhuepf.imagej.zoo.data.classification;

public enum Phase {
    unefined(0),
    phase_early_nucl_1(1),
    phase_post_nucl_1 (2),
    phase_early_nucl_2(3),
    phase_post_nucl_2 (4),
    phase_early_nucl_3(5),
    phase_post_nucl_3 (6),
    phase_early_nucl_4(7),
    phase_post_nucl_4_mitosis(8),
    phase_steady_nucl (9),
    phase_1st_wave    (10),
    phase_post_1st_wave(11),
    phase_2nd_wave    (12),
    phase_post_2nd_wave(13),
    phase_3rd_wave    (14),
    phase_post_3rd_wave(15),
    phase_blastop     (16),
    phase_4th_wave    (17),
    phase_gastrul     (18),
    phase_closwndw    (19),
    phase_detach      (20);

    public final int MAX_PHASES = 21;
    public static Phase[] all = {
            unefined,
            phase_early_nucl_1,
            phase_post_nucl_1,
            phase_early_nucl_2,
            phase_post_nucl_2,
            phase_early_nucl_3,
            phase_post_nucl_3,
            phase_early_nucl_4,
            phase_post_nucl_4_mitosis,
            phase_steady_nucl,
            phase_1st_wave,
            phase_post_1st_wave,
            phase_2nd_wave,
            phase_post_2nd_wave,
            phase_3rd_wave,
            phase_post_3rd_wave,
            phase_blastop,
            phase_4th_wave,
            phase_gastrul,
            phase_closwndw,
            phase_detach
};

    public int value = 0;

    Phase(int value) {
        this.value = value;
    }
}
