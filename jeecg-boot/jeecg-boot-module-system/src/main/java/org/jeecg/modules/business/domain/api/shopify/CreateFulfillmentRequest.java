package org.jeecg.modules.business.domain.api.shopify;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Slf4j
public class CreateFulfillmentRequest extends ShopifyRequest {

    private final static String AFTERSHIP = "https://www.aftership.com/en/track/%s";
    private final static String OTHER = "Other";

    private enum TransportCompany {
        LA_POSTE("https://www.laposte.fr/outils/suivre-vos-envois?code=%s", "La Poste", "6[A-Z]{1}[0-9]{11}"),
        SWISS_POST("https://service.post.ch/ekp-web/ui/entry/search/%s", "Swiss Post", "[0-9]{18}"),
        DHL_PACKET("https://www.dhl.de/en/privatkunden/pakete-empfangen/verfolgen.html?piececode=%s", "DHL Packet", "[0-9]{20}"),
        GLS_NL("https://www.gls-info.nl/tracking", "GLS", "[0-9]{20}"),
        COLIS_PRIVE_BE_LU("https://colisprive.com/moncolis/pages/detailColis.aspx?numColis=%s", "Colis Privé", "Q[0-9]{11}[BL][0-9]{4}"),
        COLIS_PRIVE_FR("https://colisprive.com/moncolis/pages/detailColis.aspx?numColis=%s", "Colis Privé", "Q[0-9]{16}"),
        POSTI("https://www.posti.fi/fi/seuranta#/lahetys/%s", "Posti", "SP[0-9]{9}FI"),
        USPS("https://tools.usps.com/go/TrackConfirmAction?qtc_tLabels1=%s", "USPS", "[0-9]{22}"),
        EARLY_BIRD("https://earlybird.se/", "Early Bird", "[0-9]{19}"),
        ;

        private final String trackingUrl;
        private final String name;
        private final String regex;

        TransportCompany(String trackingUrl, String name, String regex) {
            this.trackingUrl = trackingUrl;
            this.name = name;
            this.regex = regex;
        }
    }

    public CreateFulfillmentRequest(ShopifyRequestBody body) {
        super(HttpMethod.POST, body);
    }

    @Override
    protected JSONObject generateJson(ShopifyRequestBody body) {
        JSONObject json = new JSONObject();
        JSONObject fulfillment = new JSONObject();
        fulfillment.put("notify_customer", true);

        JSONObject trackingInfo = new JSONObject();
        String trackingNumber = ((CreateFulfillmentRequestBody) body).getTrackingNumber();
        trackingInfo.put("number", trackingNumber);
        TransportCompany transportCompany = resolveTransportCompany(trackingNumber);
        String trackingUrl = transportCompany == null ? AFTERSHIP : transportCompany.trackingUrl;
        trackingInfo.put("url", String.format(trackingUrl, trackingNumber));
        String trackingCompanyName = transportCompany == null ? OTHER : transportCompany.name;
        trackingInfo.put("company", String.format(trackingCompanyName, trackingNumber));
        fulfillment.put("tracking_info", trackingInfo);

        JSONArray lineItems = new JSONArray();
        JSONObject fulfillmentOrderId = new JSONObject();
        fulfillmentOrderId.put("fulfillment_order_id", ((CreateFulfillmentRequestBody) body).getFulfillmentId());
        lineItems.add(fulfillmentOrderId);

        fulfillment.put("line_items_by_fulfillment_order", lineItems);
        json.put("fulfillment", fulfillment);
        return json;
    }

    private TransportCompany resolveTransportCompany(String trackingNumber) {
        for (TransportCompany transportCompany : TransportCompany.values()) {
            if (trackingNumber.matches(transportCompany.regex)) {
                log.info("{} matches {}'s regex", trackingNumber, transportCompany.name);
                return transportCompany;
            }
        }
        log.info("{} couldn't be matched to any transporter, generic Aftership URL will be given.", trackingNumber);
        return null;
    }

    protected HttpHeaders buildHeaders() {
        HttpHeaders headers = super.buildHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        return headers;
    }
}
