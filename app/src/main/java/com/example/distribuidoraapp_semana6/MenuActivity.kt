package com.example.distribuidoraapp_semana6

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address // Para Geocoder
import android.location.Geocoder // Para Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException // Para Geocoder
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

class MenuActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var googleMap: GoogleMap? = null
    private lateinit var locationCallback: LocationCallback

    // UI Elements
    private lateinit var usuarioText: TextView
    private lateinit var costoDespachoText: TextView
    private lateinit var editTextValorCompra: EditText
    private lateinit var buttonCalcularDespacho: Button
    private lateinit var editTextDireccionDespacho: EditText // Nuevo
    private lateinit var buttonUsarDireccion: Button      // Nuevo

    // Location Data
    private var ubicacionParaCalculo: LatLng? = null // Esta será la ubicación usada (GPS o dirección)
    private var ubicacionActualGPS: LatLng? = null // Para mantener la última del GPS

    private val LOCATION_REQUEST_CODE = 1001
    private val BASE_LAT = -33.4372
    private val BASE_LNG = -70.6506
    private val BODEGA_LOCATION = LatLng(BASE_LAT, BASE_LNG)

    private val clpFormatter = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d("MenuActivity", "Mapa listo.")
        googleMap?.addMarker(MarkerOptions().position(BODEGA_LOCATION).title("Bodega Principal"))
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(BODEGA_LOCATION, 12f))
        // Si ya tenemos una ubicación (GPS o manual) al cargar el mapa, la mostramos
        ubicacionParaCalculo?.let {
            updateMapWithLocation(it, "Ubicación Seleccionada", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Inicializar UI
        usuarioText = findViewById(R.id.usuarioText)
        costoDespachoText = findViewById(R.id.costoDespachoText)
        editTextValorCompra = findViewById(R.id.editTextValorCompra)
        buttonCalcularDespacho = findViewById(R.id.buttonCalcularDespacho)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        editTextDireccionDespacho = findViewById(R.id.editTextDireccionDespacho) // Nuevo
        buttonUsarDireccion = findViewById(R.id.buttonUsarDireccion)          // Nuevo

        val user = FirebaseAuth.getInstance().currentUser
        usuarioText.text = "Usuario: ${user?.email ?: "Desconocido"}"

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()

        setupLocationCallback()
        requestLocationPermission() // Esto iniciará la obtención de ubicación GPS

        // Listener para el botón "Usar Esta Dirección"
        buttonUsarDireccion.setOnClickListener {
            val direccionStr = editTextDireccionDespacho.text.toString().trim()
            if (direccionStr.isNotEmpty()) {
                geocodeAddress(direccionStr)
            } else {
                Toast.makeText(this, "Por favor, ingrese una dirección.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonCalcularDespacho.setOnClickListener {
            ubicacionParaCalculo?.let { loc ->
                procesarUbicacionYCompra(loc)
            } ?: run {
                Toast.makeText(this, "Esperando ubicación (GPS o dirección ingresada)...", Toast.LENGTH_SHORT).show()
                costoDespachoText.text = "Costo de despacho: Ubicación no disponible"
            }
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.d("MenuActivity", "Ubicación GPS obtenida: ${location.latitude}, ${location.longitude}")
                ubicacionActualGPS = LatLng(location.latitude, location.longitude)

                // Por defecto, usar la ubicación GPS si no se ha ingresado una dirección manualmente
                if (ubicacionParaCalculo == null || ubicacionParaCalculo == ubicacionActualGPS) {
                    ubicacionParaCalculo = ubicacionActualGPS
                    updateMapWithLocation(ubicacionParaCalculo!!, "Mi Ubicación (GPS)", true)
                }


                runOnUiThread {
                    if (costoDespachoText.text.toString().contains("Ubicación no disponible") ||
                        costoDespachoText.text.toString().contains("Calculando...")) {
                        costoDespachoText.text = "Costo de despacho: Listo para calcular (Presione el botón)"
                    }
                }
            }
        }
    }

    private fun geocodeAddress(addressString: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            // El '1' significa que solo queremos el primer resultado más relevante
            val addresses: List<Address>? = geocoder.getFromLocationName(addressString, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)
                ubicacionParaCalculo = latLng // Actualizar la ubicación a usar
                Log.d("MenuActivity", "Dirección geocodificada: $addressString -> $latLng")
                Toast.makeText(this, "Dirección encontrada: ${address.getAddressLine(0)}", Toast.LENGTH_LONG).show()
                updateMapWithLocation(latLng, addressString, true)
                // Opcional: Limpiar el campo de dirección después de usarla
                // editTextDireccionDespacho.text.clear()
                // Opcional: Llamar directamente a procesarUbicacionYCompra si así se desea
                // procesarUbicacionYCompra(latLng)
                runOnUiThread {
                    costoDespachoText.text = "Costo de despacho: Dirección establecida. Presione Calcular."
                }

            } else {
                Log.w("MenuActivity", "No se encontraron resultados para la dirección: $addressString")
                Toast.makeText(this, "No se pudo encontrar la dirección. Intente ser más específico o verifique la dirección.", Toast.LENGTH_LONG).show()
                // Mantener la ubicación GPS si la dirección no se encuentra
                ubicacionParaCalculo = ubicacionActualGPS
                ubicacionActualGPS?.let { updateMapWithLocation(it, "Mi Ubicación (GPS)", true) }
            }
        } catch (e: IOException) {
            // Error de red u otro problema de I/O con el Geocoder
            Log.e("MenuActivity", "Error de Geocoding", e)
            Toast.makeText(this, "Servicio de geocodificación no disponible. Verifique su conexión o intente más tarde.", Toast.LENGTH_LONG).show()
            ubicacionParaCalculo = ubicacionActualGPS
            ubicacionActualGPS?.let { updateMapWithLocation(it, "Mi Ubicación (GPS)", true) }
        }
    }


    private fun updateMapWithLocation(location: LatLng, title: String, animate: Boolean) {
        googleMap?.clear()
        googleMap?.addMarker(MarkerOptions().position(BODEGA_LOCATION).title("Bodega Principal"))
        googleMap?.addMarker(MarkerOptions().position(location).title(title))
        if (animate) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    private fun procesarUbicacionYCompra(locationToUse: LatLng) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid
        if (uid == null) {
            Log.e("MenuActivity", "UID de usuario es nulo.")
            Toast.makeText(this, "Error de autenticación, por favor reinicie sesión.", Toast.LENGTH_SHORT).show()
            return
        }

        val valorCompraStr = editTextValorCompra.text.toString()
        val valorCompra = if (valorCompraStr.isNotEmpty()) {
            valorCompraStr.toIntOrNull() ?: -1
        } else {
            0
        }

        if (valorCompra == -1) {
            runOnUiThread {
                Toast.makeText(this@MenuActivity, "Monto de compra inválido. Ingrese solo números.", Toast.LENGTH_LONG).show()
            }
            costoDespachoText.text = "Costo de despacho: Monto inválido"
            return
        }
        if (valorCompra == 0 && valorCompraStr.isEmpty()){
            runOnUiThread {
                Toast.makeText(this@MenuActivity, "Por favor, ingrese el monto de la compra.", Toast.LENGTH_SHORT).show()
            }
            costoDespachoText.text = "Costo de despacho: Ingrese monto"
            editTextValorCompra.requestFocus()
            return
        }

        val distancia = calcularDistancia(locationToUse.latitude, locationToUse.longitude, BODEGA_LOCATION.latitude, BODEGA_LOCATION.longitude)
        val costoDespacho = calcularValorDespacho(distancia.roundToInt(), valorCompra)

        val db = FirebaseDatabase.getInstance().getReference("despachos")
        val despachoData = mapOf(
            "uid_usuario" to uid,
            "latitud_destino" to locationToUse.latitude,
            "longitud_destino" to locationToUse.longitude,
            "direccion_ingresada" to if (locationToUse != ubicacionActualGPS) editTextDireccionDespacho.text.toString() else "GPS",
            "distancia_km_a_bodega" to distancia.roundToInt(),
            "monto_compra_ingresado" to valorCompra,
            "costo_despacho_calculado" to costoDespacho,
            "timestamp" to System.currentTimeMillis()
        )

        db.child(uid).child(System.currentTimeMillis().toString()).setValue(despachoData)
            .addOnSuccessListener {
                Log.d("MenuActivity", "Datos de despacho guardados en Firebase.")
            }
            .addOnFailureListener { e ->
                Log.e("MenuActivity", "Error al guardar datos en Firebase.", e)
                Toast.makeText(this, "Error al guardar en BD: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        runOnUiThread {
            val distanciaFormateada = "%.1f".format(distancia)
            val textoCosto: String
            clpFormatter.maximumFractionDigits = 0

            if (costoDespacho == 0.0 && valorCompra > 50000 && distancia < 20) {
                textoCosto = "Despacho gratuito (compra ${clpFormatter.format(valorCompra)} y dist. ${distanciaFormateada}km)"
            } else if (costoDespacho == 0.0 && valorCompra <= 50000 && valorCompra > 0) {
                textoCosto = "Despacho: ${clpFormatter.format(costoDespacho.toInt())} (Compra ${clpFormatter.format(valorCompra)}, Dist. ${distanciaFormateada}km)\n(Gratis sobre \$50.000 y <20km)"
            } else if (valorCompra == 0 && valorCompraStr.isEmpty()){
                textoCosto = "Costo de despacho: Ingrese monto"
            }
            else {
                textoCosto = "Despacho: ${clpFormatter.format(costoDespacho.toInt())} (Compra ${clpFormatter.format(valorCompra)}, Dist. ${distanciaFormateada}km)"
            }
            costoDespachoText.text = textoCosto
        }
    }

    // --- Permisos y Lógica de Ubicación GPS (sin cambios mayores) ---
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            Log.d("MenuActivity", "Permisos de ubicación ya concedidos.")
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MenuActivity", "Permiso de ubicación concedido por el usuario.")
                startLocationUpdates()
            } else {
                Log.w("MenuActivity", "Permiso de ubicación denegado por el usuario.")
                Toast.makeText(this, "Permiso de ubicación denegado. Funcionalidad limitada.", Toast.LENGTH_LONG).show()
                costoDespachoText.text = "Costo de despacho: Se requiere permiso de ubicación."
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        Log.d("MenuActivity", "Iniciando actualizaciones de ubicación GPS.")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun calcularValorDespacho(distanciaKm: Int, valorCompra: Int): Double {
        // Reglas de negocio según el caso
        // Dentro de un radio de 20 km:
        return when {
            distanciaKm < 20 && valorCompra > 50000 -> 0.0 // Compras sobre $50.000
            distanciaKm < 20 && valorCompra in 25000..49999 -> (distanciaKm * 150.0) // Compras entre $25.000 y $49.999
            distanciaKm < 20 && valorCompra < 25000 -> (distanciaKm * 300.0) // Compras menores a $25.000

            // Reglas adicionales si la distancia es >= 20 km (estas son ejemplos, ajusta a tus reglas)
            distanciaKm >= 20 && valorCompra > 50000 -> { // Fuera del radio de 20km pero compra alta
                // Ejemplo: Despacho gratis los primeros 20km, y $150 por km adicional
                // (distanciaKm - 20) * 150.0 // O la regla que definas
                (distanciaKm - 20) * 150.0
            }
            distanciaKm >= 20 && valorCompra in 25000..49999 -> {
                distanciaKm * 200.0 // Tarifa un poco mayor
            }
            distanciaKm >= 20 && valorCompra < 25000 -> {
                distanciaKm * 350.0 // Tarifa aún mayor
            }


            else -> {
                Log.w("MenuActivity", "Caso de despacho no cubierto explícitamente: Distancia ${distanciaKm}km, Compra $valorCompra")
                distanciaKm *  200.0 // Una tarifa por defecto general
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("MenuActivity", "onStop: Deteniendo actualizaciones de ubicación.")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}