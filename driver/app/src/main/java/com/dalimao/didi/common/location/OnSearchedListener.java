package com.dalimao.didi.common.location;

import java.util.List;

/**
 * POI 搜索结果监听器
 * Created by liuguangli on 17/3/22.
 */
public interface OnSearchedListener {
    void onSearched(List<LocationInfo> results);

    void onError(int rCode);
}
