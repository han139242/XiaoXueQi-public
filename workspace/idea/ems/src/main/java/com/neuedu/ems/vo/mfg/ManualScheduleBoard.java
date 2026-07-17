package com.neuedu.ems.vo.mfg;

import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ManualScheduleBoard {
    private MfgScheduleScenario workspaceDraft;
    private List<ManualScheduleBoardItem> timelineItems = new ArrayList<>();
    private List<ManualScheduleBoardWorkingWindow> workingWindows = new ArrayList<>();
    private List<MfgProductionOrder> pendingOrders = new ArrayList<>();
    private LocalDateTime horizonStart;
    private LocalDateTime horizonEnd;
    private Integer manualItemCount;
    private Integer productionPlanCount;
    private Integer conflictCount;
    private Integer lateCount;
}
