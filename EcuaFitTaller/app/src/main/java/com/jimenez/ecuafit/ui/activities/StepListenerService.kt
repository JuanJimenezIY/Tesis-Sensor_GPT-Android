package com.jimenez.ecuafit.ui.utilities

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class StepListenerService : WearableListenerService(), DataClient.OnDataChangedListener {

    override fun onCreate() {
        super.onCreate()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/steps") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val steps = dataMap.getInt("steps", 0)
                val intent = Intent("com.jimenez.ecuafit.STEP_UPDATE")
                intent.putExtra("steps", steps)
                sendBroadcast(intent)
            }
        }
    }
}
