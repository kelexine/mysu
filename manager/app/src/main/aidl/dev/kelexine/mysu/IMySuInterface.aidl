// IMySuInterface.aidl
package dev.kelexine.mysu;

import android.content.pm.PackageInfo;
import rikka.parcelablelist.ParcelableListSlice;

interface IMySuInterface {
    ParcelableListSlice<PackageInfo> getPackages(int flags);

    int[] getUserIds();
}