package com.kovalsikoski.johan.marvelapi

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressDialog: AlertDialog
    private lateinit var adapter: CharacterAdapter

    private var layoutManager = GridLayoutManager(this@MainActivity, StaggeredGridLayoutManager.VERTICAL)
    private var charactersList = mutableListOf<MarvelModel.MarvelPage.Characters>()

    private var timeStamp = ""
    private var hash = ""
    private var lastVisibleItem = 0
    private var loadedCharacters = 0
    private var totalCharacters = 0
    private var offSet = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeStamp = Date().time.toString()
        hash = createHash(timeStamp)

        recyclerViewInitializer()
        buildAlertDialogForProgress()
        loadFirstCharacterPage(timeStamp, hash)

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                loadedCharacters = adapter.itemCount
                lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if(loadedCharacters <= (lastVisibleItem + 5)) {
                    loadNextCharacterPage(timeStamp, hash)
                }

            }
        })
    }

    private fun loadFirstCharacterPage(ts: String, hash: String){
        progressDialog.show()

        val call = RetrofitInitializer().marvelService().getCharactersFirstPage(ts, getString(R.string.public_key),hash)

        call.enqueue(object : Callback<MarvelModel> {
            override fun onResponse(call: Call<MarvelModel>?, response: Response<MarvelModel>?) {
                if (response?.body()?.code == 200) {

                    totalCharacters = response.body()!!.data.count
                    Log.v("eoq", "TOTAL-> $totalCharacters")

                    response.body()!!.data.results.let {
                        it.forEach { adapter.add(it) }
                        progressDialog.dismiss()
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, getString(R.string.cant_load_content), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<MarvelModel>?, t: Throwable?) {
                Log.e("error", t?.message)
                progressDialog.dismiss()
            }
        })
    }

    private fun loadNextCharacterPage(ts: String, hash: String){
        progressDialog.show()

        val call = RetrofitInitializer().marvelService().getCharactersNextPage(ts, getString(R.string.public_key),hash, offSet)

        call.enqueue(object : Callback<MarvelModel> {
            override fun onResponse(call: Call<MarvelModel>?, response: Response<MarvelModel>?) {
                if (response?.body()?.code == 200) {

                    Log.v("eoq", "NEXT-> ${response.body()!!.data.offset}")

                    response.body()!!.data.results.let {
                        it.forEach {
                            adapter.add(it)
                        }

                        progressDialog.dismiss()

                        offSet += 20
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, getString(R.string.cant_load_content), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<MarvelModel>?, t: Throwable?) {
                Log.e("error", t?.message)
                progressDialog.dismiss()
            }
        })
    }

    private fun createHash(ts: String): String {

        val hash = "$ts${getString(R.string.private_key)}${getString(R.string.public_key)}"

        val messageDigest = MessageDigest.getInstance("MD5")
        messageDigest.update(hash.toByteArray(), 0, hash.length)

        return BigInteger(1, messageDigest.digest()).toString(16)
    }

    private fun recyclerViewInitializer(){
        recyclerView = findViewById(R.id.character_recyclerview)
        adapter = CharacterAdapter(charactersList, this@MainActivity)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
    }

    private fun buildAlertDialogForProgress() {
        val dialogView = View.inflate(this, R.layout.custom_layout_progressbar, null)
        val builder = AlertDialog.Builder(this)
        val message = dialogView.findViewById<TextView>(R.id.messageDialog)

        message.text = getString(R.string.loading_progressbar)
        builder.setView(dialogView)
        builder.setCancelable(false)
        progressDialog = builder.create()
    }
}