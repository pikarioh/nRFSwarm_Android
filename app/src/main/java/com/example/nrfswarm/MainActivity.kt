package com.example.nrfswarm

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
import com.jjoe64.graphview.series.PointsGraphSeries
import kotlinx.android.synthetic.main.activity_main.*

/*
* nRF Swarm 1.1
* Bachelor thesis's app project for MQTT messaging
* Developed by the nRF Swarm Group
* Last modification date: 27th May 2019
 */

// Main Activity

class MainActivity : AppCompatActivity(), UIUpdaterInterface {

    // Global values initiations
    var mqttManager: MQTTmanager? = null
    var graph: GraphView? = null
    var xySeries: PointsGraphSeries<DataPoint>? = null
    private var xyValueArray: ArrayList<XYValue>? = null

    // Interface methods

    override fun resetUIWithConnection(status: Boolean) {
        // This method enable/disable the views on the layout

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
        // The method updates status on the statusLabl-bar
        statusLabl.text = status
    }

    override fun update(message: String) {
        // This method updates that messageHistoryView with MQTT messages

        // Convert raw text to string
        var text = messageHistoryView.text.toString()
        var newText = """
            $text
            $message
            """
        // Refresh the text history by add new incoming messages to the old one and apply
        messageHistoryView.setText(newText)
        messageHistoryView.setSelection(messageHistoryView.text.length)

        // Find Graph view
        graph = findViewById<GraphView>(R.id.graph_view)

        // Initiate GSON (JSON Parser) and display the parsed value on the layout
        val gson = GsonBuilder().create().fromJson(message, Position::class.java)
        messageLine.text = "X: ${gson.x} // Y: ${gson.y}"

        // Draw the point from JSON data on the graph view and log the result
        xyValueArray?.add(XYValue(gson.x, gson.y))
        init()
        Log.d("POSITION ADDED", "X: ${gson.x} , Y: ${gson.y}")
    }

    // Overriding lifecycle methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable send button and message textfield only after connection
        resetUIWithConnection(false)

        graph = findViewById(R.id.graph_view)

        xyValueArray = ArrayList()

        init()
    }

    // Custom layout controller methods

    private fun init() {
        // Graph initiator

        // Declare the xySeries Object
        xySeries = PointsGraphSeries()

        // Øittle bit of exception handling for if there is no data.
        if (xyValueArray?.size !== 0) {
            createScatterPlot()
        } else { }
    }

    private fun createScatterPlot() {
        // Graph plot creator
        Log.d("GRAPH VIEW", "createScatterPlot: Creating scatter plot.")

        // Sort the array of xy values
        xyValueArray = xyValueArray?.let { sortArray(it) }

        // Add the data to the series
        for (i in 0 until xyValueArray?.size!!) {
            try {
                val x = xyValueArray?.get(i)?.x!!.toDouble()
                val y = xyValueArray?.get(i)?.y!!.toDouble()
                xySeries?.appendData(DataPoint(x, y), true, 1000)
            } catch (e: IllegalArgumentException) {
                Log.w("GRAPH VIEW", "createScatterPlot: IllegalArgumentException: " + e.message)
            }

        }

        // Set some properties
        xySeries?.setShape(PointsGraphSeries.Shape.POINT);
        //xySeries.setColor(Color.BLUE);
        xySeries?.setSize(20f);

        // Set Scrollable and Scaleable
        graph?.viewport?.isScalable = true
        graph?.viewport?.isScrollable = true
        graph?.viewport?.setScalableY(true)
        graph?.viewport?.setScrollableY(true)

        // Set manual y bounds
        graph?.viewport?.setMaxY(500.0)
        graph?.viewport?.setMinY(-500.0)
        graph?.viewport?.isYAxisBoundsManual = true

        // Set manual x bounds
        graph?.viewport?.setMaxX(500.0)
        graph?.viewport?.setMinX(-500.0)
        graph?.viewport?.isXAxisBoundsManual = true

        graph?.addSeries(xySeries)
    }

    private fun sortArray(array: ArrayList<XYValue>): ArrayList<XYValue> {
        /*
        // Sorts the xyValues in Ascending order to prepare them for the PointsGraphSeries<DataSet>
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
                // Print out the y entrys so we know what the order looks like
                // Log.d(TAG, "sortArray: Order:");
                // for(int n = 0;n < array.size();n++){
                // Log.d(TAG, "sortArray: " + array.get(n).getY());
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

    // Button methods

    fun connect(view: View){
        // This method establish the MQTT connection when pressed
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
        // Clear the graph when pressed "Clear" button
        graph?.removeAllSeries()
        xyValueArray = ArrayList()
        xySeries = null
    }

    fun sendMessage(view: View){
        //The method copies the message from the messageField to publication
        mqttManager?.publish(messageField.text.toString())
        messageField.setText("")
    }
}

// Necessary classes used in the Main Activity regarding plotting and JSON class

class Position(val x: Double, val y: Double)

class XYValue(var x: Double, var y: Double)