<#include "../components/header.ftl">
    <tr>
        <td style="padding:35px 0;">Cher(e) ${firstname} ${lastname},</td>
    </tr>
    <tr>
        <td style="padding:0 0 35px 0;">Voici un récapitulatif des opérations que vous avez effectuées sur vos commandes :</td>
    </tr>
    <#if cancelSuccessCount??>
        <tr>
            <td style="padding:0 0 35px 0;">Demandes d'annulations de commande : <b>${cancelSuccessCount}</b> réussie(s)</td>
        </tr>
        <#if cancelFailures?size gt 0 >
            <tr>
                <td style="padding:0 0 35px 0;">Demandes d'annulations de commande échouées :<br/>
                    <ul>
                        <#list cancelFailures as failure>
                            <li>${failure}</li>
                        </#list>
                    </ul>
                </td>
            </tr>
        </#if>
    </#if>
    <#if suspendSuccessCount??>
        <tr>
            <td style="padding:0 0 35px 0;">Demandes de suspension de commande : <b>${suspendSuccessCount}</b> réussie(s)</td>
        </tr>
        <#if suspendFailures?size gt 0 >
            <tr>
                <td style="padding:0 0 35px 0;">Demandes de suspension de commande échouées :<br/>
                    <ul>
                        <#list suspendFailures as failure>
                            <li>${failure}</li>
                        </#list>
                    </ul>
                </td>
            </tr>
        </#if>
    </#if>
    <tr>
        <td style="padding:0 0 35px 0;">Pour toute information complémentaire nous vous invitons à vous rapprocher de votre conseiller.</td>
    </tr>
<#include "../components/footer.ftl">