package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.mapper.sys.SysNotificationRecipientMapper;
import com.neuedu.ems.vo.sys.NotificationCategoryCount;
import com.neuedu.ems.vo.sys.NotificationInboxItem;
import com.neuedu.ems.vo.sys.NotificationUnreadSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysNotificationServiceImplTest {

    private final SysNotificationRecipientMapper mapper = mock(SysNotificationRecipientMapper.class);
    private final SysNotificationServiceImpl service = new SysNotificationServiceImpl(mapper);

    @Test
    void unreadSummaryOnlyUsesCurrentUserAndGroupsCategories() {
        NotificationCategoryCount mfg = count("MFG", 3L);
        NotificationCategoryCount qms = count("QMS", 2L);
        when(mapper.countUnreadByCategory(7L)).thenReturn(List.of(mfg, qms));

        NotificationUnreadSummary summary = service.unreadSummary(7L);

        assertEquals(5L, summary.total());
        assertEquals(3L, summary.categories().get("MFG"));
        verify(mapper).countUnreadByCategory(7L);
    }

    @Test
    void inboxNormalizesFiltersAndPinsCurrentUser() {
        Page<NotificationInboxItem> result = new Page<>(1, 20);
        when(mapper.selectInboxPage(any(), eq(7L), eq("MFG"), eq("HIGH"), eq("UNREAD")))
                .thenReturn(result);

        assertEquals(result, service.pageInbox(7L, 1, 20, "mfg", "high", "unread"));
        verify(mapper).selectInboxPage(any(), eq(7L), eq("MFG"), eq("HIGH"), eq("UNREAD"));
    }

    @Test
    void detailCannotFallBackToAnotherUsersRecipient() {
        when(mapper.selectInboxItem(7L, 99L)).thenReturn(null);

        assertNull(service.getInboxItem(7L, 99L));
    }

    @Test
    void markReadAlwaysCarriesCurrentUser() {
        when(mapper.markRead(7L, 9L)).thenReturn(1);
        assertTrue(service.markRead(7L, 9L));
        verify(mapper).markRead(7L, 9L);
    }

    private NotificationCategoryCount count(String category, Long total) {
        NotificationCategoryCount count = new NotificationCategoryCount();
        count.setCategory(category);
        count.setTotal(total);
        return count;
    }
}
