-- After deploying JebKitSplitJob, import this row and resume it in Quartz administration.
-- Only JEB orders with erp_status = 1 are eligible.
-- Every 10 minutes. To target one order, set parameter to {"platformOrderId":"13377878131036"}.
INSERT INTO sys_quartz_job
(id, create_by, create_time, del_flag, job_class_name, cron_expression, parameter, description, status)
SELECT 'jeb-kit-split', 'admin', NOW(), 0,
       'org.jeecg.modules.business.domain.job.JebKitSplitJob', '0 0/10 * * * ?',
       NULL, 'JEB Kit SKU split', -1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_quartz_job
    WHERE job_class_name = 'org.jeecg.modules.business.domain.job.JebKitSplitJob' AND del_flag = 0
);
