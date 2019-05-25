package com.example.nrfswarm

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.nrfswarm.manager.MQTTConnectionParams
import com.example.nrfswarm.manager.MQTTmanager
import com.example.nrfswarm.protocols.UIUpdaterInterface
import com.google.gson.GsonBuilder
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.DataPointInterface
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.series.PointsGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import androidx.core.view.ViewCompat.setX
import androidx.core.view.ViewCompat.setY





class MainActivity : AppCompatActivity(), UIUpdaterInterface {

    var mqttManager: MQTTmanager? = null

    var graph: GraphView? = null
    var xySeries: PointsGraphSeries<DataPoint>? = null
    private var xyValueArray: ArrayList<XYValue>? = null

    // Interface methods
    override fun resetUIWithConnection(status: Boolean) {

        ipAddressField.isEnabled  = !status
        topicField.isEnabled      = !status
        messageField.isEnabled    = status
        connectBtn.isEnabled      = !status
        sendBtn.isEnabled         = status
        clearBtn.isEnabled        = status

        // Update the status label.
        if (status){
            updateStatusViewWith("Connected")
        }else{
            updateStatusViewWith("Disconnected")
        }
    }

    override fun updateStatusViewWith(status: String) {
        statusLabl.text = status
    }

    override fun update(message: String) {

        var text = messageHistoryView.text.toString()
        var newText = """
            $text
            $message
            """
        //var newText = text.toString() + "\n" + message +  "\n"
        messageHistoryView.setText(newText)
        messageHistoryView.setSelection(messageHistoryView.text.length)

        graph = findViewById<GraphView>(R.id.graph_view)
        val gson = GsonBuilder().create().fromJson(message, Position::class.java)

        messageLine.text = "X: ${gson.x} // Y: ${gson.y}"

        xyValueArray?.add(XYValue(gson.x, gson.y))
        init()
        Log.d("POSITION ADDED", "X: ${gson.x} , Y: ${gson.y}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable send button and message textfield only after connection
        resetUIWithConnection(false)

        graph = findViewById(R.id.graph_view)

        xyValueArray = ArrayList()

        init()
    }

    private fun init() {
        //declare the xySeries Object
        xySeries = PointsGraphSeries()

        //little bit of exception handling for if there is no data.
        if (xyValueArray?.size !== 0) {
            createScatterPlot()
        } else { }
    }

    private fun createScatterPlot() {
        Log.d("GRAPH VIEW", "createScatterPlot: Creating scatter plot.")

        //sort the array of xy values
        xyValueArray = xyValueArray?.let { sortArray(it) }

        //add the data to the series
        for (i in 0 until xyValueArray?.size!!) {
            try {
                val x = xyValueArray?.get(i)?.x!!.toDouble()
                val y = xyValueArray?.get(i)?.y!!.toDouble()
                xySeries?.appendData(DataPoint(x, y), true, 1000)
            } catch (e: IllegalArgumentException) {
                Log.w("GRAPH VIEW", "createScatterPlot: IllegalArgumentException: " + e.message)
            }

        }

        //set some properties
        xySeries?.setShape(PointsGraphSeries.Shape.POINT);
        //xySeries.setColor(Color.BLUE);
        xySeries?.setSize(20f);

        //set Scrollable and Scaleable
        graph?.viewport?.isScalable = true
        graph?.viewport?.isScrollable = true
        graph?.viewport?.setScalableY(true)
        graph?.viewport?.setScrollableY(true)

        //set manual y bounds
        graph?.viewport?.setMaxY(500.0)
        graph?.viewport?.setMinY(-500.0)
        graph?.viewport?.isYAxisBoundsManual = true

        //set manual x bounds
        graph?.viewport?.setMaxX(500.0)
        graph?.viewport?.setMinX(-500.0)
        graph?.viewport?.isXAxisBoundsManual = true

        graph?.addSeries(xySeries)
    }

    private fun sortArray(array: ArrayList<XYValue>): ArrayList<XYValue> {
        /*
        //Sorts the xyValues in Ascending order to prepare them for the PointsGraphSeries<DataSet>
         */
        val factor = Integer.parseInt(Math.round(Math.pow(array.size.toDouble(), 2.0)).toString())
        var m = array.size - 1
        var count = 0
        Log.d("GRAPH VIEW", "sortArray: Sorting the XYArray.")


        while (true) {
            m--
            if (m <= 0) {
                m = array.size - 1
            }
            Log.d("GRAPH VIEW", "sortArray: m = $m")
            try {
                //print out the y entrys so we know what the order looks like
                //Log.d(TAG, "sortArray: Order:");
                //for(int n = 0;n < array.size();n++){
                //Log.d(TAG, "sortArray: " + array.get(n).getY());
                //}
                val tempY = array[m - 1].y
                val tempX = array[m - 1].x
                if (tempX > array[m].x) {
                    array[m - 1].y = array[m].y
                    array[m].y = tempY
                    array[m - 1].x = array[m].x
                    array[m].x = tempX
                } else if (tempX == array[m].x) {
                    count++
                    Log.d("GRAPH VIEW", "sortArray: count = $count")
                } else if (array[m].x > array[m - 1].x) {
                    count++
                    Log.d("GRAPH VIEW", "sortArray: count = $count")
                }
                //break when factorial is done
                if (count == factor) {
                    break
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(
                    "GRAPH VIEW",
                    "sortArray: ArrayIndexOutOfBoundsException. Need more than 1 data point to create Plot." + e.message
                )
                break
            }

        }
        return array
    }


    fun connect(view: View){

        if (!(ipAddressField.text.isNullOrEmpty() && topicField.text.isNullOrEmpty())) {
            var host = "tcp://" + ipAddressField.text.toString() + ":1883"
            var topic = topicField.text.toString()
            var connectionParams = MQTTConnectionParams("MQTTSample",host,topic,"","")
            mqttManager = MQTTmanager(connectionParams,applicationContext,this)
            mqttManager?.connect()
        }else{
            updateStatusViewWith("Please enter all valid fields")
        }

    }

    fun clear(view: View){
        graph?.removeAllSeries()
        xyValueArray = ArrayList()
        xySeries = null
    }

    fun sendMessage(view: View){

        mqttManager?.publish(messageField.text.toString())

        messageField.setText("")
    }
}

class Position(val x: Double, val y: Double)

class XYValue(var x: Double, var y: Double)