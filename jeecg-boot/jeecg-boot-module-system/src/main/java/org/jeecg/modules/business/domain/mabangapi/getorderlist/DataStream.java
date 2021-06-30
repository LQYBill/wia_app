package org.jeecg.modules.business.domain.mabangapi.getorderlist;

import java.util.ArrayList;

public interface DataStream<E>  {
    boolean hasNext();

    E next();

    default ArrayList<E> all() {
        ArrayList<E> res = new ArrayList<>();
        while (hasNext()) {
            res.add(next());
        }
        return res;
    }
}
