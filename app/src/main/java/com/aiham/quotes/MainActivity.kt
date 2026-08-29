package com.aiham.quotes

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var quotesList: ArrayList<String>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quotesList = arrayListOf(
            "لا تحزن إن الله معنا.",
            "النجاح يبدأ بخطوة وإصرار لا ينقطع.",
            "كن أنت التغيير الذي تريد أن تراه في العالم.",
            "الصبر مفتاح الفرج وتاج المؤمن."
        )

        val listView = findViewById<ListView>(R.id.listViewQuotes)
        val btnAddQuote = findViewById<Button>(R.id.btnAddQuote)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, quotesList)
        listView.adapter = adapter

        btnAddQuote.setOnClickListener {
            showAddQuoteDialog()
        }
    }

    private fun showAddQuoteDialog() {
        val input = EditText(this)
        input.hint = "أضف اقتباسك هنا..."

        AlertDialog.Builder(this)
            .setTitle("إضافة اقتباس جديد")
            .setView(input)
            .setPositiveButton("إضافة") { _, _ ->
                val text = input.text.toString().trim()
                
                // 🔐 المفتاح السري لفتح المساحة الخاصة
                if (text == "aiham77rk") {
                    val intent = Intent(this, PrivateSpaceActivity::class.java)
                    startActivity(intent)
                } else if (text.isNotEmpty()) {
                    quotesList.add(0, text)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "تمت إضافة الاقتباس بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
