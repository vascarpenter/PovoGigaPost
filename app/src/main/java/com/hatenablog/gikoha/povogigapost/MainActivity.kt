package com.hatenablog.gikoha.povogigapost

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.util.*
import java.util.concurrent.TimeUnit

data class PovoGigaPost (
    val apikey: String,
    val date: String,
    val gigaleft: String,
    val memo: String,
)

class MainActivity : AppCompatActivity()
{
    private lateinit var recyclerView: RecyclerView
    private var data: ArrayList<PovoGiga> = ArrayList()
    private var mainact = this

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager(this).getOrientation())

        recyclerView = findViewById(R.id.listView)
        recyclerView.addItemDecoration(dividerItemDecoration)
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadData()

        (findViewById<Button>(R.id.submitButton)).setOnClickListener {
            closeSoftKeyboard()
            val button = findViewById<Button>(R.id.submitButton)
            button.text = "Submitting..."
            val gigaleft = (findViewById<EditText>(R.id.gigaTextField)).text.toString()
            val memo = (findViewById<EditText>(R.id.memoTextField)).text.toString()
            val d = PovoGigaPost(BuildConfig.povoapikey, getDateString(), gigaleft, memo)
            val json = Gson().toJson(d)

            val mediaTypeJson = "application/json; charset=utf8".toMediaType()

            val request: Request = Request.Builder()
                .url(BuildConfig.povopostapiurl)
                .post(json.toRequestBody(mediaTypeJson))
                .build()

            buildOkHttp().newCall(request).enqueue(object : Callback
               {
                   override fun onResponse(call: Call, response: Response)
                   {
                       val responseBody = response.body?.string().orEmpty()
                       runOnUiThread {
                           button.text = "Submit $responseBody" // will be OK
                       }
                       loadData()  // refresh list
                   }

                   override fun onFailure(call: Call, e: IOException)
                   {
                       Log.e("Post Error", e.toString())
                   }
               })

        }
    }

    private fun closeSoftKeyboard()
    {
        // close soft keyboard
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(mainact)).windowToken, 0)
    }

    private fun getDateString() : String
    {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)+1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        return String.format("%04d%02d%02d %02d%02d",year,month,day,hour,minute)
    }
    private fun loadData()
    {
        val request = Request.Builder().url(BuildConfig.povogetapiurl).build()

        // synced net access
        buildOkHttp().newCall(request).enqueue(object : Callback
           {
               override fun onFailure(call: Call, e: IOException)
               {
                   Log.e("NET", "download error", e)
               }

               override fun onResponse(call: Call, response: Response)
               {
                   val responseText: String =
                       response.body?.string()?.trimMargin() ?: ""

                   try
                   {
                       // get array of json to List<DriveLapse>
                       val itemType = object : TypeToken<ArrayList<PovoGiga>>(){}.type
                       val element: ArrayList<PovoGiga> = Gson().fromJson(responseText, itemType)
                       data = element
                   } catch (e: JsonSyntaxException)
                   {
                       Log.e("JSON", "parse error", e)
                   }

                   runOnUiThread {
                       val adapter = RecyclerAdapter(data)
                       recyclerView.adapter = adapter
                       adapter.notifyDataSetChanged()
                   }
               }
           })
    }
    private fun buildOkHttp(): OkHttpClient
    {
        val client = OkHttpClient.Builder()
        client.connectTimeout(20, TimeUnit.SECONDS)
        client.readTimeout(15, TimeUnit.SECONDS)
        client.writeTimeout(15, TimeUnit.SECONDS)
        return client.build()
    }

}