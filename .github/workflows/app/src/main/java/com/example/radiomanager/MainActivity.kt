package com.example.radiomanager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class RadioDevice(
    val name: String,
    val ipAddress: String
)

class MainActivity : AppCompatActivity() {
    
    private lateinit var deviceSpinner: Spinner
    private lateinit var webView: WebView
    private lateinit var addDeviceButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private var radioDevices = mutableListOf<RadioDevice>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupSharedPreferences()
        loadDevices()
        setupSpinner()
        setupWebView()
        setupAddDeviceButton()
        
        // Jeśli nie ma urządzeń, pokaż dialog dodawania
        if (radioDevices.isEmpty()) {
            showAddDeviceDialog()
        }
    }
    
    private fun initializeViews() {
        deviceSpinner = findViewById(R.id.deviceSpinner)
        webView = findViewById(R.id.webView)
        addDeviceButton = findViewById(R.id.addDeviceButton)
    }
    
    private fun setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("RadioDevices", Context.MODE_PRIVATE)
    }
    
    private fun loadDevices() {
        val devicesJson = sharedPreferences.getString("devices", "[]")
        val type = object : TypeToken<List<RadioDevice>>() {}.type
        radioDevices = Gson().fromJson(devicesJson, type) ?: mutableListOf()
    }
    
    private fun saveDevices() {
        val devicesJson = Gson().toJson(radioDevices)
        sharedPreferences.edit().putString("devices", devicesJson).apply()
    }
    
    private fun setupSpinner() {
        val deviceNames = radioDevices.map { "${it.name} (${it.ipAddress})" }.toMutableList()
        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = deviceAdapter
        
        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (radioDevices.isNotEmpty() && position < radioDevices.size) {
                    loadRadioInterface(radioDevices[position].ipAddress)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                showErrorMessage("Błąd połączenia: $description")
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false // Pozwól WebView obsłużyć URL
            }
        }
    }
    
    private fun setupAddDeviceButton() {
        addDeviceButton.setOnClickListener {
            showAddDeviceDialog()
        }
    }
    
    private fun showAddDeviceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.deviceNameEditText)
        val ipEditText = dialogView.findViewById<EditText>(R.id.deviceIpEditText)
        
        AlertDialog.Builder(this)
            .setTitle("Dodaj nowe radio")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val ip = ipEditText.text.toString().trim()
                
                if (validateInput(name, ip)) {
                    addDevice(name, ip)
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    
    private fun validateInput(name: String, ip: String): Boolean {
        if (name.isEmpty()) {
            showErrorMessage("Wprowadź nazwę urządzenia")
            return false
        }
        
        if (ip.isEmpty()) {
            showErrorMessage("Wprowadź adres IP")
            return false
        }
        
        // Podstawowa walidacja IP
        val ipPattern = Regex("""^(\d{1,3}\.){3}\d{1,3}(:\d+)?$""")
        if (!ipPattern.matches(ip)) {
            showErrorMessage("Nieprawidłowy format adresu IP (np. 192.168.1.100:8080)")
            return false
        }
        
        return true
    }
    
    private fun addDevice(name: String, ip: String) {
        val formattedIp = if (!ip.startsWith("http://") && !ip.startsWith("https://")) {
            "http://$ip"
        } else {
            ip
        }
        
        val newDevice = RadioDevice(name, formattedIp)
        radioDevices.add(newDevice)
        saveDevices()
        updateSpinner()
        
        // Wybierz nowo dodane urządzenie
        deviceSpinner.setSelection(radioDevices.size - 1)
        
        showSuccessMessage("Urządzenie zostało dodane")
    }
    
    private fun updateSpinner() {
        val deviceNames = radioDevices.map { "${it.name} (${it.ipAddress})" }
        deviceAdapter.clear()
        deviceAdapter.addAll(deviceNames)
        deviceAdapter.notifyDataSetChanged()
    }
    
    private fun loadRadioInterface(ipAddress: String) {
        try {
            webView.loadUrl(ipAddress)
        } catch (e: Exception) {
            showErrorMessage("Nie można załadować interfejsu radia: ${e.message}")
        }
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showSuccessMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
