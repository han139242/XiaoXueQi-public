package com.neuedu.ems.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.sys.SysNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotification> {

    @Insert("""
            INSERT IGNORE INTO sys_notification
              (event_key, event_type, category, severity, title, summary, biz_type, biz_id, biz_no,
               route_path, source_user_id, source_employee_id, occurred_time, expire_time, status,
               payload_json, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
            VALUES
              (#{eventKey}, #{eventType}, #{category}, #{severity}, #{title}, #{summary}, #{bizType}, #{bizId}, #{bizNo},
               #{routePath}, #{sourceUserId}, #{sourceEmployeeId}, #{occurredTime}, #{expireTime}, #{status},
               #{payloadJson}, #{remark}, #{version}, #{createdBy}, #{createdTime}, #{updatedBy}, #{updatedTime}, #{deletedFlag})
            """)
    int insertIgnore(SysNotification notification);
}
