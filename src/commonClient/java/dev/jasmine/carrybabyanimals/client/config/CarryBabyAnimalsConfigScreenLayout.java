package dev.jasmine.carrybabyanimals.client.config;

record CarryBabyAnimalsConfigScreenLayout(
        int left,
        int titleY,
        int titleWidth,
        int statusX,
        int statusY,
        int statusWidth,
        int reactionsY,
        int tuckedPoseY,
        int firstPersonModeY,
        int sleepyVisualsY,
        int intensityLabelY,
        int intensityInputY,
        int disabledAnimalsLabelY,
        int disabledAnimalsInputY,
        int buttonY,
        int contentWidth,
        int buttonWidth,
        int rowHeight
) {
    static final String TUCKED_POSE_LABEL = "Third-person large baby side tuck";
    static final String FIRST_PERSON_MODE_LABEL = "First-person large baby placement";

    private static final int CONTENT_WIDTH = 320;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 150;
    private static final int STATUS_SIZE = 8;
    private static final int MIN_MARGIN = 12;
    private static final int FOOTER_MARGIN = 8;
    private static final int TITLE_STATUS_GAP = 8;

    static CarryBabyAnimalsConfigScreenLayout create(int screenWidth, int screenHeight) {
        int contentWidth = Math.min(CONTENT_WIDTH, Math.max(220, screenWidth - MIN_MARGIN * 2));
        int left = Math.max(MIN_MARGIN, (screenWidth - contentWidth) / 2);
        int buttonY = Math.max(screenHeight - ROW_HEIGHT - FOOTER_MARGIN, 0);
        int titleY = screenHeight < 340 ? 10 : 20;
        int y = titleY + (screenHeight < 340 ? 23 : 28);
        int rowSpacing = screenHeight < 340 ? 23 : 27;
        int labelInputGap = screenHeight < 340 ? 0 : 2;
        int statusWidth = STATUS_SIZE;
        int statusX = left + contentWidth - statusWidth;
        int titleWidth = Math.max(80, statusX - left - TITLE_STATUS_GAP);

        int statusY = titleY + (ROW_HEIGHT - statusWidth) / 2;
        int reactionsY = y;
        y += rowSpacing;
        int tuckedPoseY = y;
        y += rowSpacing;
        int firstPersonModeY = y;
        y += rowSpacing;
        int sleepyVisualsY = y;
        y += rowSpacing;
        int intensityLabelY = y;
        int intensityInputY = intensityLabelY + ROW_HEIGHT + labelInputGap;
        int disabledAnimalsLabelY = intensityInputY + ROW_HEIGHT + labelInputGap;
        int disabledAnimalsInputY = Math.min(disabledAnimalsLabelY + ROW_HEIGHT + labelInputGap, buttonY - ROW_HEIGHT);
        disabledAnimalsLabelY = disabledAnimalsInputY - ROW_HEIGHT - labelInputGap;

        return new CarryBabyAnimalsConfigScreenLayout(
                left,
                titleY,
                titleWidth,
                statusX,
                statusY,
                statusWidth,
                reactionsY,
                tuckedPoseY,
                firstPersonModeY,
                sleepyVisualsY,
                intensityLabelY,
                intensityInputY,
                disabledAnimalsLabelY,
                disabledAnimalsInputY,
                buttonY,
                contentWidth,
                Math.min(BUTTON_WIDTH, (contentWidth - 8) / 2),
                ROW_HEIGHT
        );
    }

    int cancelButtonX() {
        return left + contentWidth - buttonWidth;
    }

    boolean contentFitsAboveButtons() {
        return disabledAnimalsInputY + rowHeight <= buttonY;
    }
}
