package com.sucho.placepicker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

const val PLACE_TITLE_KEY = "place_title_key"
const val PLACE_SUBTITLE_KEY = "place_subtitle_key"
const val PLACE_ID_KEY = "place_id_key"

class PlacePickerSearchActivity : AppCompatActivity() {

    private lateinit var placesClient: PlacesClient
    private lateinit var adapter: SearchPlacesAdapter

    private val writeDelayTime = 500
    private val handler = Handler()

    private val progressBar: ProgressBar
        get() = findViewById(R.id.place_picker_search_progress_bar)
    private val placesRecyclerView: RecyclerView
        get() = findViewById(R.id.place_picker_search_result_recycler_view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_picker_search)

        val app = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val bundle = app.metaData
        Places.initialize(applicationContext, bundle.getString("com.google.android.API_KEY") ?: "")

        placesClient = Places.createClient(this)

        findViewById<EditText>(R.id.place_picker_search_edit_text).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    runOnUiThread {
                        test(text.toString())
                    }
                }, writeDelayTime.toLong())
            }
        })

        adapter = SearchPlacesAdapter(this) {
            val data = Intent()
            data.putExtra(PLACE_TITLE_KEY, it.primaryTitle)
            data.putExtra(PLACE_SUBTITLE_KEY, it.secondaryTitle)
            data.putExtra(PLACE_ID_KEY, it.placeId)
            setResult(RESULT_OK, data)
            finish()
        }

        placesRecyclerView.adapter = adapter
        placesRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.search_place)

        progressBar.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun test(query: String) {
        progressBar.visibility = View.VISIBLE

        Log.i("PlacePickerSearchActivity", "log query =$query")

        val token = AutocompleteSessionToken.newInstance()

        val request = FindAutocompletePredictionsRequest.builder()
            .setCountry("es")
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            for (prediction in response.autocompletePredictions) {
                Log.i("PlacePickerSearchActivity", prediction.placeId)
                Log.i("PlacePickerSearchActivity", prediction.getFullText(null).toString())
            }
            adapter.places = response.autocompletePredictions
                .map {
                    SearchPlace(
                        it.getPrimaryText(null).toString(),
                        it.getSecondaryText(null).toString(),
                        it.placeId
                    )
                }
            adapter.notifyDataSetChanged()
            progressBar.visibility = View.GONE
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e("PlacePickerSearchActivity", "Place not found: " + exception.statusCode)
            }
            progressBar.visibility = View.GONE
        }
    }

    inner class SearchPlacesAdapter constructor(context: Context,
                                                var places: List<SearchPlace> = listOf(),
                                                val onPlaceClicked: (SearchPlace) -> Unit) : RecyclerView.Adapter<SearchPlacesAdapter.ViewHolder>() {

        private val mInflater: LayoutInflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = mInflater.inflate(R.layout.place_search_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val place = places[position]
            holder.title.text = place.primaryTitle
            holder.subtitle.text = place.secondaryTitle
            holder.itemView.setOnClickListener {
                onPlaceClicked(place)
            }
        }

        override fun getItemCount(): Int {
            return places.size
        }

        inner class ViewHolder internal constructor(private val view: View) :
            RecyclerView.ViewHolder(view) {
            val title: TextView
                get() = view.findViewById(R.id.title_text_view)
            val subtitle: TextView
                get() = view.findViewById(R.id.subtitle_text_view)
        }
    }
}