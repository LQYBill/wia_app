<html>
<body><p>Évolution tarifaire à partir du ${effectiveDate}</p>
<table border="1">
    <tr>
        <th style="text-align: center;">Code SKU</th>
        <th style="text-align: center;">Nom du produit</th>
        <th style="text-align: center;">Image du produit</th>
        <th style="text-align: center;">Pays</th>
        <th style="text-align: center;">Date ancien prix</th>
        <th style="text-align: center;">Date nouveau prix</th>
        <th style="text-align: center;">Ancien prix total</th>
        <th style="text-align: center;">Nouveau prix total (différence)</th>
    </tr>
    <#list rows as row>
        <tr>
            <td style="text-align: center;">${row.skuCode!}</td>
            <td style="text-align: center;">${row.skuName!}</td>
            <td style="text-align: center;"><img src="${row.imageUrl!}" width="200" style="display:block;border:0;">
            </td>
            <td style="text-align: center;">${row.countryWithFlag!}</td>
            <td style="text-align: center;">${row.oldDate}</td>
            <td style="text-align: center;">${row.newDate}</td>
            <td style="text-align: center;">${row.oldTotal}</td>
            <td style="text-align: center;">
                ${row.newTotal}
                <span style="color: <#if row.difference gt 0>red<#elseif row.difference lt 0>green<#else>inherit</#if>;">
                    （<#if row.difference gt 0>+${row.difference?string["0.00"]}<#else>${row.difference?string["0.00"]}</#if>
                                <#if row.changePercentage??>,
                        <#if row.changePercentage gt 0>+${row.changePercentage?string["0.00"]}<#else>${row.changePercentage?string["0.00"]}</#if>%
                    </#if>）
                </span>
            </td>
        </tr>
    </#list>
</table>
</body>
</html>
