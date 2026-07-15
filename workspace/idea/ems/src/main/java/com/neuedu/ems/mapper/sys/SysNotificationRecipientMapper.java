package com.neuedu.ems.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.entity.sys.SysNotificationRecipient;
import com.neuedu.ems.vo.sys.NotificationCategoryCount;
import com.neuedu.ems.vo.sys.NotificationInboxItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysNotificationRecipientMapper extends BaseMapper<SysNotificationRecipient> {

    String ITEM_COLUMNS = """
            r.id AS recipient_id, r.notification_id, r.recipient_user_id, r.recipient_employee_id,
            n.event_type, n.category, n.severity, n.title, n.summary, n.biz_type, n.biz_id, n.biz_no,
            n.route_path, n.occurred_time, n.expire_time, n.status AS notification_status,
            r.recipient_reason, r.read_status, r.read_time, r.ack_status, r.ack_time, r.delivery_status
            """;

    @Insert("""
            INSERT IGNORE INTO sys_notification_recipient
              (notification_id, recipient_user_id, recipient_employee_id, recipient_reason,
               read_status, read_time, ack_status, ack_time, delivery_status, delivered_time,
               remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
            VALUES
              (#{notificationId}, #{recipientUserId}, #{recipientEmployeeId}, #{recipientReason},
               #{readStatus}, #{readTime}, #{ackStatus}, #{ackTime}, #{deliveryStatus}, #{deliveredTime},
               #{remark}, #{version}, #{createdBy}, #{createdTime}, #{updatedBy}, #{updatedTime}, #{deletedFlag})
            """)
    int insertIgnore(SysNotificationRecipient recipient);

    @Select("""
            <script>
            SELECT
            """ + ITEM_COLUMNS + """
            FROM sys_notification_recipient r
            JOIN sys_notification n ON n.notification_id = r.notification_id AND n.deleted_flag = 0
            WHERE r.recipient_user_id = #{userId} AND r.deleted_flag = 0
              AND n.status = 'ACTIVE' AND (n.expire_time IS NULL OR n.expire_time &gt; NOW())
            <if test="category != null and category != ''">AND n.category = #{category}</if>
            <if test="severity != null and severity != ''">AND n.severity = #{severity}</if>
            <if test="readStatus != null and readStatus != ''">AND r.read_status = #{readStatus}</if>
            ORDER BY n.occurred_time DESC, r.id DESC
            </script>
            """)
    Page<NotificationInboxItem> selectInboxPage(Page<NotificationInboxItem> page,
                                                 @Param("userId") Long userId,
                                                 @Param("category") String category,
                                                 @Param("severity") String severity,
                                                 @Param("readStatus") String readStatus);

    @Select("""
            SELECT
            """ + ITEM_COLUMNS + """
            FROM sys_notification_recipient r
            JOIN sys_notification n ON n.notification_id = r.notification_id AND n.deleted_flag = 0
            WHERE r.id = #{recipientId} AND r.recipient_user_id = #{userId}
              AND r.deleted_flag = 0
            LIMIT 1
            """)
    NotificationInboxItem selectInboxItem(@Param("userId") Long userId,
                                           @Param("recipientId") Long recipientId);

    @Select("""
            SELECT n.category, COUNT(*) AS total
            FROM sys_notification_recipient r
            JOIN sys_notification n ON n.notification_id = r.notification_id AND n.deleted_flag = 0
            WHERE r.recipient_user_id = #{userId} AND r.deleted_flag = 0
              AND r.read_status = 'UNREAD' AND n.status = 'ACTIVE'
              AND (n.expire_time IS NULL OR n.expire_time > NOW())
            GROUP BY n.category
            ORDER BY n.category
            """)
    List<NotificationCategoryCount> countUnreadByCategory(@Param("userId") Long userId);

    @Update("""
            UPDATE sys_notification_recipient
            SET read_status = 'READ', read_time = COALESCE(read_time, NOW()),
                updated_by = #{userId}, updated_time = NOW(), version = version + 1
            WHERE id = #{recipientId} AND recipient_user_id = #{userId} AND deleted_flag = 0
            """)
    int markRead(@Param("userId") Long userId, @Param("recipientId") Long recipientId);

    @Update("""
            <script>
            UPDATE sys_notification_recipient r
            JOIN sys_notification n ON n.notification_id = r.notification_id AND n.deleted_flag = 0
            SET r.read_status = 'READ', r.read_time = COALESCE(r.read_time, NOW()),
                r.updated_by = #{userId}, r.updated_time = NOW(), r.version = r.version + 1
            WHERE r.recipient_user_id = #{userId} AND r.deleted_flag = 0 AND r.read_status = 'UNREAD'
            <if test="category != null and category != ''">AND n.category = #{category}</if>
            </script>
            """)
    int markAllRead(@Param("userId") Long userId, @Param("category") String category);

    @Update("""
            UPDATE sys_notification_recipient
            SET ack_status = 'ACKNOWLEDGED', ack_time = COALESCE(ack_time, NOW()),
                read_status = 'READ', read_time = COALESCE(read_time, NOW()),
                updated_by = #{userId}, updated_time = NOW(), version = version + 1
            WHERE id = #{recipientId} AND recipient_user_id = #{userId}
              AND deleted_flag = 0 AND ack_status IN ('PENDING', 'ACKNOWLEDGED')
            """)
    int acknowledge(@Param("userId") Long userId, @Param("recipientId") Long recipientId);

    @Select("""
            <script>
            SELECT
            """ + ITEM_COLUMNS + """
            FROM sys_notification_recipient r
            JOIN sys_notification n ON n.notification_id = r.notification_id AND n.deleted_flag = 0
            WHERE r.deleted_flag = 0
            <if test="recipientUserId != null">AND r.recipient_user_id = #{recipientUserId}</if>
            <if test="category != null and category != ''">AND n.category = #{category}</if>
            <if test="deliveryStatus != null and deliveryStatus != ''">AND r.delivery_status = #{deliveryStatus}</if>
            ORDER BY n.occurred_time DESC, r.id DESC
            </script>
            """)
    Page<NotificationInboxItem> selectAuditPage(Page<NotificationInboxItem> page,
                                                 @Param("recipientUserId") Long recipientUserId,
                                                 @Param("category") String category,
                                                 @Param("deliveryStatus") String deliveryStatus);
}
