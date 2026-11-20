package org.jeecg.modules.business.mapper;

import org.apache.ibatis.annotations.*;
import org.jeecg.modules.business.entity.AutoPurchaseRun;
import org.jeecg.modules.business.entity.Sku;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AutoPurchaseMapper {
    @Select("SELECT id FROM client WHERE id = #{id} AND active = 1 FOR UPDATE")
    String lockClient(String id);

    // A SKU shared by clients has shared stock and cannot safely be bought per client.
    @Select("SELECT s.* FROM sku s JOIN client_sku client_sku_link ON client_sku_link.sku_id=s.id " +
            "WHERE client_sku_link.client_id=#{clientId} AND s.status=3 AND s.auto_purchase_enabled=1 " +
            "AND s.is_gift = 2 " +
            "AND (SELECT COUNT(DISTINCT x.client_id) FROM client_sku x WHERE x.sku_id=s.id)=1 " +
            "GROUP BY s.id ORDER BY s.id")
    List<Sku> eligibleSkus(String clientId);

    @Select("SELECT * FROM auto_purchase_run WHERE client_id=#{clientId} " +
            "AND (plan_state != 'DONE' OR email_sent=0 OR run_date=#{day}) ORDER BY create_time LIMIT 1")
    AutoPurchaseRun findBlockingRun(@Param("clientId") String clientId, @Param("day") String day);

    @Insert("INSERT INTO auto_purchase_run(id,client_id,run_date,purchase_id,recipient,mabang_username,auto_mark_paid) " +
            "VALUES(#{run.id},#{run.clientId},#{day},#{run.purchaseId},#{run.recipient},#{run.mabangUsername},#{run.autoMarkPaid})")
    int insert(@Param("run") AutoPurchaseRun run, @Param("day") String day);

    @Update("UPDATE auto_purchase_run SET email_sent=1 WHERE id=#{id}")
    int emailSent(String id);

    @Update("UPDATE auto_purchase_run SET plan_state='SENDING' WHERE id=#{id} AND plan_state='READY'")
    int claimPlan(String id);

    @Update("UPDATE auto_purchase_run SET plan_state=#{state},plan_result=#{result} WHERE id=#{id}")
    int finishPlan(@Param("id") String id, @Param("state") String state, @Param("result") String result);
}
