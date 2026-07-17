package com.neuedu.ems.vo.mfg;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Manual-board display window derived from the same line calendar used by scheduling capacity checks.
 */
@Data
public class ManualScheduleBoardWorkingWindow {
    private Long lineId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
