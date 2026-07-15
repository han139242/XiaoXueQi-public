package com.neuedu.ems.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.sys.SysEventOutbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysEventOutboxMapper extends BaseMapper<SysEventOutbox> {

    @Insert("""
            INSERT IGNORE INTO sys_event_outbox
              (event_key, aggregate_type, aggregate_id, event_type, payload_json, status, retry_count,
               next_retry_time, last_error, occurred_time, processed_time, remark, version,
               created_by, created_time, updated_by, updated_time, deleted_flag)
            VALUES
              (#{eventKey}, #{aggregateType}, #{aggregateId}, #{eventType}, #{payloadJson}, #{status}, #{retryCount},
               #{nextRetryTime}, #{lastError}, #{occurredTime}, #{processedTime}, #{remark}, #{version},
               #{createdBy}, #{createdTime}, #{updatedBy}, #{updatedTime}, #{deletedFlag})
            """)
    int insertIgnore(SysEventOutbox outbox);

    @Update("""
            UPDATE sys_event_outbox
            SET status = 'PROCESSING', updated_time = #{now}, version = version + 1
            WHERE outbox_id = #{outboxId} AND deleted_flag = 0
              AND status IN ('PENDING', 'FAILED')
              AND retry_count < #{maxRetries}
              AND (next_retry_time IS NULL OR next_retry_time <= #{now})
            """)
    int claim(@Param("outboxId") Long outboxId, @Param("now") LocalDateTime now,
              @Param("maxRetries") int maxRetries);

    @Update("""
            UPDATE sys_event_outbox
            SET status = 'FAILED', next_retry_time = #{now},
                last_error = 'PROCESSING_TIMEOUT', updated_time = #{now}, version = version + 1
            WHERE deleted_flag = 0 AND status = 'PROCESSING' AND updated_time < #{staleBefore}
            """)
    int resetStale(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);
}
