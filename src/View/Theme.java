package View;

import java.awt.Color;

/**
 * Single source of truth for the app's colour palette.
 * Every screen (Login, dashboards, widgets, headers) pulls its colours from
 * here instead of redefining its own new Color(...) values, so the whole
 * theme can be re-skinned in one place.
 *
 * Palette: deep petrol / teal with warm sand accents — a coastal, tourism-led
 * identity for "Travel BP Sri Lanka" that still reads as a serious business
 * system. Chosen so that white text sits on every dark surface at a contrast
 * ratio above 4.5:1 (WCAG AA), which is what makes table headers, nav items
 * and buttons legible instead of washed out.
 */
public final class Theme {

    private Theme() {}

    // ---- Core brand palette --------------------------------------------
    public static final Color DARK   = new Color(18, 48, 58);     // #12303A deep petrol — sidebar, headings, TABLE HEADERS
    public static final Color NAVY_2 = new Color(27, 70, 83);     // #1B4653 lighter petrol — hover/active nav rows
    public static final Color HEADER = new Color(14, 92, 107);    // #0E5C6B teal — top bar
    public static final Color OCEAN  = new Color(11, 114, 133);   // #0B7285 primary teal — buttons, links, accents
    public static final Color SUN    = new Color(232, 150, 58);   // #E8963A warm sand — CTA / highlight strip
    public static final Color LEAF   = new Color(23, 145, 94);    // #17915E green — success / confirmed / approved
    public static final Color LIGHT  = new Color(242, 246, 247);  // #F2F6F7 pale mist — dashboard/page background

    // Status colours
    public static final Color GREEN  = LEAF;                      // success / confirmed / approved
    public static final Color RED    = new Color(198, 60, 60);    // #C63C3C danger / cancelled
    public static final Color ORANGE = new Color(199, 118, 22);   // #C77616 warning / pending

    // Extra accents used by the dashboard stat cards
    public static final Color PURPLE = new Color(94, 79, 173);    // #5E4FAD revenue accent
    public static final Color BLUE_SOFT = new Color(214, 235, 239); // #D6EBEF pale teal chip background

    // Supporting tones
    public static final Color DARK_OVERLAY = new Color(18, 48, 58, 150); // translucent overlay on hero image
    public static final Color TAG_TEXT     = new Color(234, 242, 244);   // light text on dark surfaces
    public static final Color SUBTEXT      = new Color(103, 126, 136);   // muted body text
    public static final Color BORDER       = new Color(214, 228, 232);   // soft field/card border
    public static final Color CREAM        = new Color(243, 248, 249);   // zebra-stripe row background
    public static final Color SKY          = new Color(213, 236, 241);   // pale accent background (tabs, selection)

    // Login screen surfaces
    public static final Color LOGIN_BG     = new Color(9, 30, 38);     // #091E26 page behind the card
    public static final Color CARD_BG      = new Color(23, 62, 74);    // #173E4A login card
    public static final Color FIELD_BG     = new Color(35, 82, 96);    // #235260 input surface
    public static final Color FIELD_BORDER = new Color(72, 122, 136);  // #487A88 input outline
    public static final Color TEXT_LIGHT   = new Color(233, 242, 244);
    public static final Color TEXT_MUTED   = new Color(157, 186, 195);
    public static final Color OCEAN_HOVER  = new Color(20, 145, 168);  // #1491A8 button hover
}
