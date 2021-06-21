package org.jeecg.modules.business.domain.shippingInvoice;

import com.neovisionaries.i18n.CountryCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
class CodeEN {

    private enum ExtraCode {
        KR("South Korea"),
        RE("Réunion");

        private final String countryName;

        ExtraCode(String countryName) {
            this.countryName = countryName;
        }

        private String getCountryName() {
            return countryName;
        }

        /**
         * Find code by name.
         *
         * @param country the country name to search
         * @return the country code, return null if no found.
         */
        public static String findByName(String country) {
            for (ExtraCode c : values()) {
                if (c.getCountryName().equals(country)) {
                    return c.name();
                }
            }
            return null;
        }
    }

    /**
     * Find code by country name.
     *
     * @param country the country name to find.
     * @return the country's code, or null if not found
     */
    static String findByName(String country) {
        List<CountryCode> code = CountryCode.findByName(country);
        if (code.size() > 0) {
            return code.get(0).name();
        } else {
            return ExtraCode.findByName(country);
        }
    }

    /**
     * Get country's name by code.
     *
     * @return country's name, or null if not found
     */
    static String getName(String code) {
        try {
            return CountryCode.valueOf(code).getName();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
