-- Run after deploying WSJob (credentials are fixed in WSTrackClient).
-- Default: hourly; last 30 shipping dates for all channels belonging to Weishi logistic companies.
-- Imported in paused state. Resume it in Quartz administration to register the scheduler trigger.
INSERT INTO sys_quartz_job
(id, create_by, create_time, del_flag, job_class_name, cron_expression, parameter, description, status)
SELECT 'weishi-track-sync', 'admin', NOW(), 0,
       'org.jeecg.modules.business.domain.job.WSJob', '0 0 * * * ?',
       '{"pageSize":200,"requestIntervalMs":200}',
       '威狮所有路线物流轨迹同步（中性文字）', -1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_quartz_job
    WHERE job_class_name = 'org.jeecg.modules.business.domain.job.WSJob' AND del_flag = 0
);
