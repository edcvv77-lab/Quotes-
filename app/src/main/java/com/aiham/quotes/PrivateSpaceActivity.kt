package com.aiham.quotes

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import top.niunaijun.blackbox.BlackBoxCore

class PrivateSpaceActivity : AppCompatActivity() {

    private val installedApps = ArrayList<String>()
    private val installedPkgNames = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_space)
        title = "المساحة الخاصة المعزولة 🔒"

        val listView = findViewById<ListView>(R.id.listViewPrivateApps)
        val btnCloneApp = findViewById<Button>(R.id.btnCloneApp)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, installedApps)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val pkg = installedPkgNames[position]
            Toast.makeText(this, "جاري تشغيل التطبيق في البيئة المعزولة...", Toast.LENGTH_SHORT).show()
            BlackBoxCore.get().launchApk(pkg, 0)
        }

        btnCloneApp.setOnClickListener {
            showCloneDialog()
        }

        loadPrivateApps()
    }

    private fun loadPrivateApps() {
        installedApps.clear()
        installedPkgNames.clear()
        try {
            val list = BlackBoxCore.get().getInstalledApplications(0, 0)
            for (info in list) {
                val label = packageManager.getApplicationLabel(info).toString()
                installedApps.add(label)
                installedPkgNames.add(info.packageName)
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCloneDialog() {
        val pm = packageManager
        val sysApps = pm.getInstalledApplications(0).filter {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
        val names = sysApps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("اختر تطبيقاً لعزله داخل المساحة")
            .setItems(names) { _, which ->
                val selectedPkg = sysApps[which].packageName
                Toast.makeText(this, "جاري نسخ وتثبيت التطبيق...", Toast.LENGTH_LONG).show()
                Thread {
                    BlackBoxCore.get().installPackageAsUser(selectedPkg, 0)
                    runOnUiThread {
                        loadPrivateApps()
                        Toast.makeText(this, "تم تثبيت التطبيق بنجاح داخل العزل!", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
