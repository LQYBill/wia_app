package org.jeecg.modules.business.domain.shippingInvoice;

import lombok.Data;

@Data
public class Country {
    private final String code;

    private final String zh_name;

    private final String en_name;


    public static final int ATTRIBUTE_CODE = 0;
    public static final int ATTRIBUTE_ZH_NAME = 1;
    public static final int ATTRIBUTE_EN_NAME = 2;


    public Country(String code, String zh_name, String en_name) {
        this.code = code;
        this.zh_name = zh_name;
        this.en_name = en_name;
    }

    /**
     * Construct a country object by one of his attribute.
     * In case that other attribute can not be found, it will be set to null.
     *
     * @param attribute the attribute to search
     * @param type      the type of attribute used to search
     *                  0 for code,
     *                  1 for zh name,
     *                  2 for en name.
     */
    public static Country makeCountry(String attribute, int type) {
        String code, en_name, zh_name;
        switch (type) {
            case ATTRIBUTE_CODE:
                code = attribute;
                en_name = CodeEN.getName(code);
                zh_name = CodeZH.getName(code);
                break;
            case ATTRIBUTE_ZH_NAME:
                zh_name = attribute;
                code = CodeZH.findByName(zh_name);
                if (code == null){
                    en_name = null;
                }else {
                    en_name = CodeEN.getName(code);
                }
                break;
            case ATTRIBUTE_EN_NAME:
                en_name = attribute;
                code = CodeEN.findByName(attribute);
                if (code == null){
                    zh_name = null;
                }else {
                    zh_name = CodeZH.getName(code);
                }
                break;
            default:
                return null;
        }
        return new Country(code, zh_name, en_name);
    }

    @Override
    public String toString() {
        return "Country{" +
                "code='" + code + '\'' +
                ", zh_name='" + zh_name + '\'' +
                ", en_name='" + en_name + '\'' +
                '}';
    }
}
