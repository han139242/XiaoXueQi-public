package com.neuedu.ems.service.mfg;

import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.common.TaskReminderSummary;

public interface MfgTaskReminderService {

    TaskReminderSummary getCurrentUserSummary(AuthorizationContext context);
}
