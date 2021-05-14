import clientConfig from "@views/business/client/purchaseOrder/clientConfig";
import account_config from "@views/business/client/purchaseOrder/account_config";
import buyer_config from "@views/business/client/purchaseOrder/buyer_config";

export default {
  caiwu: account_config.account,
  client: clientConfig.client,
  'Client (for test)': clientConfig.client,
  buyer:buyer_config.buyer
}