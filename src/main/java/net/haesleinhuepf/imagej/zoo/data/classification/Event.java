package net.haesleinhuepf.imagej.zoo.data.classification;

public enum Event {
    unefined(0),
    event_early_nucl_1(1),
    event_post_nucl_1 (2),
    event_early_nucl_2(3),
    event_post_nucl_2 (4),
    event_early_nucl_3(5),
    event_post_nucl_3 (6),
    event_early_nucl_4(7),
    event_post_nucl_4_mitosis(8),
    event_steady_nucl (9),
    event_1st_wave    (10),
    event_post_1st_wave(11),
    event_2nd_wave    (12),
    event_post_2nd_wave(13),
    event_3rd_wave    (14),
    event_post_3rd_wave(15),
    event_blastop     (16),
    event_4th_wave    (17),
    event_gastrul     (18),
    event_closwndw    (19),
    event_detach      (20);

    public int value = 0;

    Event(int value) {
        this.value = value;
    }
}
